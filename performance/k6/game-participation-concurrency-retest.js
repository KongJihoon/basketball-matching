import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';


/*
 * ============================================================
 * 기본 설정
 * ============================================================
 */

const BASE_URL = 'http://localhost:8080';

const GAME_ID = Number(__ENV.GAME_ID);

// 락 제거 : before
// 락 적용 : after
const TEST_PHASE = 'after-lock';

// smoke -> warmup -> baseline
const TEST_MODE = 'baseline';

const TEST_RUN = 'baseline-1000-run-03';

const TEST_PASSWORD = 'Perf@1234';

const TEST_PROFILES = {
    smoke: {
        vus: 1,
    },

    warmup: {
        vus: 10,
    },

    baseline: {
        vus: 1000,
    },
};

const selectedProfile = TEST_PROFILES[TEST_MODE];

if (!selectedProfile) {
    throw new Error(`지원하지 않는 테스트 모드 : ${TEST_MODE}`)
}

if (!Number.isInteger(GAME_ID) || GAME_ID <= 0) {
    throw new Error('GAME_ID가 유효하지 않습니다.')
}

// 경기 정원 6명 중 생성자 한 명은 이미 ACCEPT 상태다.
const EXPECTED_SUCCESS = Math.min(selectedProfile.vus, 5);
const EXPECTED_FULL = selectedProfile.vus - EXPECTED_SUCCESS;

// 201과 409를 HTTP 차원의 예상 응답으로 취급
// 409가 진짜 정원 초과인지는 응답의 ErrorCode로 다시 검사
const expectedJoinStatus = http.expectedStatuses(201, 409);

/*
 * ============================================================
 * 사용자 정의 지표
 * ============================================================
 */

const joinRequests = new Counter('game_join_requests');
const joinSuccess = new Counter('game_join_success');
const joinFull = new Counter('game_join_full');
const joinUnexpected = new Counter('game_join_unexpected');

const joinDuration = new Trend('game_join_duration_ms');
const joinSuccessDuration = new Trend('game_join_success_duration_ms');
const joinFullDuration = new Trend('game_join_full_duration_ms');


/*
 * ============================================================
 * 실행 설정
 * ============================================================
 */


const thresholds = {
    // 락이 없는 실험에서도 요청이 누락되면 비교할 수 없다.
    game_join_requests: [`count==${selectedProfile.vus}`]
};

// 정합성 기대값은 비관적 락 적용 후에만 통과 조건으로 건다.
// 락 없는 실험은 실패 현상을 관찰하는 기준선으로 설정
if (TEST_PHASE === 'after-lock') {
    thresholds.game_join_success = [`count==${EXPECTED_SUCCESS}`,];

    thresholds.game_join_full = [`count==${EXPECTED_FULL}`,];

    thresholds.game_join_unexpected = ['count==0',];
}

export const options = {

    setupTimeout: '5m',

    scenarios: {
        game_join_api: {
            /*
             * 각 VU가 정확히 1회 참가한다.
             *
             * baseline:
             * 서로 다른 사용자 100명 * 1회 = 총 100건의 요청이 발생
             */
            executor: 'per-vu-iterations',
            exec: 'joinGames',

            vus: selectedProfile.vus,
            iterations: 1,

            maxDuration: '2m',
            gracefulStop: '10s',

            tags: {
                scenario_name: 'game-join-api',
                endpoint: 'game-join',
                test_phase: TEST_PHASE,
                test_mode: TEST_MODE,
                test_run: TEST_RUN,
            },
        },
    },

    // FULL_HEADCOUNT_GAME 확인을 위해 409응답 본문이 필요하다.
    discardResponseBodies: false,

    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'max',
        'p(90)',
        'p(95)',
        'p(99)',
    ],

    thresholds,

    tags: {
        application: 'basketball-matching',
        test_name: 'game-join-concurrency-retest',
        test_phase: TEST_PHASE,
        test_mode: TEST_MODE,
        test_run: TEST_RUN,
    },

};

/*
 * ============================================================
 * 테스트 사용자 이메일
 * ============================================================
 */

function userEmail(number) {
    return (
        `perf-concurrency-user-${String(number).padStart(3, '0')}`
        + '@example.test'
    );
}

function parseJson(response) {
    try {
        return response.json();
    } catch (_) {
        return null;
    }
}

/*
 * ============================================================
 * 사전 로그인
 * ============================================================
 *
 * 참가 측정 전에 필요한 사용자들을 순서대로 로그인한다.
 * 로그인 시간은 참가 API 지표와 endpoint 태그로 구분한다.
 */

export function setup() {
    const tokens = [];

    for (let number = 1; number <= selectedProfile.vus; number++) {

        const email = userEmail(number);

        const response = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            JSON.stringify({
                email,
                password: TEST_PASSWORD,
            }),
            {
                headers: {
                    'Content-Type' : 'application/json',
                },

                tags: {
                  name: 'POST /api/v1/auth/login',
                  endpoint: 'game-join-login',
                },

                timeout: '10s',
            },
        );

        const accessToken = parseJson(response)?.data?.accessToken;

        const loginSucceeded = check(
            response,
            {
                '테스트 사용자 로그인 성공':
                    (result) =>
                        result.status === 200
                        && typeof accessToken === 'string'
                        && accessToken.length > 0,
            },
            {
                endpoint: 'game-join-login',
            },
        );

        if (!loginSucceeded) {
            throw new Error(
                `로그인 실패: user=${number}, ` + `email=${email}, status=${response.status}`,
            );
        }

        tokens.push(accessToken);
    }

    return {tokens};
}

/*
 * ============================================================
 * 경기 참가
 * ============================================================
 */

export function joinGames(testData) {

    // VU는 1부터 시작하고 배열인덱스는 0부터 시작한다.
    const accessToken = testData.tokens[__VU - 1];

    if (!accessToken) {
        throw new Error(`VU ${__VU}에 할당된 토큰이 없습니다.`);
    }

    const response = http.post(
        `${BASE_URL}/api/v1/games/${GAME_ID}/participations`,
        null,
        {
            headers: {
                Authorization: `Bearer ${accessToken}`,
                Accept: 'application/json',
            },

            tags: {
                name: 'POST /api/v1/games/{gameId}/participations',
                endpoint: 'game-join',
            },

            responseCallback: expectedJoinStatus,
            timeout: '30s',
        },
    );

    const responseBody = parseJson(response);

    const isSuccess = response.status === 201;

    const isFull = response.status === 409 && responseBody?.errorCode === 'FULL_HEADCOUNT_GAME';

    const isUnexpected = !isSuccess && !isFull;


    joinRequests.add(1);
    joinSuccess.add(isSuccess ? 1 : 0);
    joinFull.add(isFull ? 1 : 0);
    joinUnexpected.add(isUnexpected? 1 : 0);

    joinDuration.add(response.timings.duration);

    if (isSuccess) {
        joinSuccessDuration.add(response.timings.duration);
    }

    if (isFull) {
        joinFullDuration.add(response.timings.duration);
    }

    check(
        response,
        {
            '참가 성공 또는 정상적인 정원 초과':
                () => isSuccess || isFull,
        },
        {
            endpoint: 'game-join',
            test_phase: TEST_PHASE,
            test_mode: TEST_MODE,
            test_run: TEST_RUN,
        },
    );

    if (isUnexpected) {
        console.error(
            `에상 밖 참가 응답: vu=${__VU}, `
            + `status=${response.status}, `
            + `errorCode=${responseBody?.errorCode || '없읍'}`
        )
    }

}