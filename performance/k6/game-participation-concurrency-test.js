import http from 'k6/http';
import { check } from 'k6';
import {
    Counter,
    Rate,
} from 'k6/metrics';


/*
 * ============================================================
 * 기본 환경 설정
 * ============================================================
 */

const BASE_URL = (
    __ENV.BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '');

const GAME_ID = __ENV.GAME_ID;

const TEST_PHASE =
    __ENV.TEST_PHASE || 'before-lock';

const TEST_RUN =
    __ENV.TEST_RUN || 'run-01';

const USER_COUNT = Number(
    __ENV.USER_COUNT || 100,
);

const EXPECTED_SUCCESS_COUNT = Number(
    __ENV.EXPECTED_SUCCESS_COUNT || 5,
);

const EXPECTED_FULL_COUNT =
    USER_COUNT - EXPECTED_SUCCESS_COUNT;

const TEST_PASSWORD =
    __ENV.TEST_PASSWORD || 'Perf@1234';

const P95_LIMIT_MS = Number(
    __ENV.P95_LIMIT_MS || 5000,
);

const SETUP_TIMEOUT =
    __ENV.SETUP_TIMEOUT || '5m';


/*
 * ============================================================
 * 환경변수 검증
 * ============================================================
 */

if (!GAME_ID) {
    throw new Error(
        'GAME_ID 환경변수가 필요합니다.',
    );
}

if (
    !Number.isInteger(USER_COUNT)
    || USER_COUNT <= 0
) {
    throw new Error(
        `USER_COUNT가 올바르지 않습니다: ${USER_COUNT}`,
    );
}

if (
    !Number.isInteger(EXPECTED_SUCCESS_COUNT)
    || EXPECTED_SUCCESS_COUNT < 0
    || EXPECTED_SUCCESS_COUNT > USER_COUNT
) {
    throw new Error(
        'EXPECTED_SUCCESS_COUNT가 올바르지 않습니다.',
    );
}


/*
 * ============================================================
 * 사용자 정의 성능 지표
 * ============================================================
 */

const participationSuccessCount =
    new Counter(
        'game_participation_success',
    );

const participationFullCount =
    new Counter(
        'game_participation_full',
    );

const participationSystemErrorCount =
    new Counter(
        'game_participation_system_error',
    );

const participationUnexpectedCount =
    new Counter(
        'game_participation_unexpected',
    );

const participationExpectedResponseRate =
    new Rate(
        'game_participation_expected_response',
    );


/*
 * ============================================================
 * k6 실행 설정
 * ============================================================
 *
 * USER_COUNT명의 사용자가 각각 한 번씩 참가 요청을 보낸다.
 *
 * per-vu-iterations:
 * - VU마다 정해진 횟수만큼 실행
 * - VU USER_COUNT명 × 1회 = 총 USER_COUNT건
 * ============================================================
 */

export const options = {
    setupTimeout: SETUP_TIMEOUT,

    scenarios: {
        game_participation_concurrency: {
            executor: 'per-vu-iterations',

            exec: 'joinGame',

            vus: USER_COUNT,
            iterations: 1,

            maxDuration: '30s',

            gracefulStop: '0s',

            tags: {
                scenario_name:
                    'game-participation-concurrency',

                endpoint:
                    'game-participation',

                test_phase:
                TEST_PHASE,

                test_run:
                TEST_RUN,

                load_level:
                    `users-${USER_COUNT}`,
            },
        },
    },

    summaryTrendStats: [
        'avg',
        'min',
        'med',
        'max',
        'p(90)',
        'p(95)',
        'p(99)',
    ],

    thresholds: {
        /*
         * 201과 정상적인 정원 초과 409는
         * 예상된 HTTP 응답으로 처리한다.
         *
         * 500, 인증 실패, 타임아웃 등만
         * http_req_failed로 집계한다.
         */
        'http_req_failed{endpoint:game-participation}': [
            'rate<0.01',
        ],

        'http_req_duration{endpoint:game-participation}': [
            `p(95)<${P95_LIMIT_MS}`,
        ],

        /*
         * 정합성 검증
         */
        game_participation_success: [
            `count==${EXPECTED_SUCCESS_COUNT}`,
        ],

        game_participation_full: [
            `count==${EXPECTED_FULL_COUNT}`,
        ],

        game_participation_system_error: [
            'count==0',
        ],

        game_participation_unexpected: [
            'count==0',
        ],

        game_participation_expected_response: [
            'rate==1',
        ],
    },
};


/*
 * ============================================================
 * 테스트 사용자 이메일 생성
 * ============================================================
 *
 * index=1
 * → perf-concurrency-user-001@example.test
 *
 * index=100
 * → perf-concurrency-user-100@example.test
 * ============================================================
 */

function createUserEmail(index) {
    const sequence = String(index)
        .padStart(3, '0');

    return (
        `perf-concurrency-user-${sequence}`
        + '@example.test'
    );
}


/*
 * ============================================================
 * 안전한 JSON 파싱
 * ============================================================
 */

function parseResponseBody(response) {
    try {
        return response.json();
    } catch (error) {
        return null;
    }
}


/*
 * ============================================================
 * 테스트 사전 준비
 * ============================================================
 *
 * 참가 요청 전에 USER_COUNT명의 사용자를 순서대로 로그인한다.
 *
 * 로그인 요청은 테스트 준비 과정이며,
 * 경기 참가 API 지표와는 endpoint 태그로 분리한다.
 * ============================================================
 */

export function setup() {
    const accessTokens = [];

    for (
        let userNumber = 1;
        userNumber <= USER_COUNT;
        userNumber += 1
    ) {
        const email =
            createUserEmail(userNumber);

        const loginPayload = JSON.stringify({
            email,
            password: TEST_PASSWORD,
        });

        const loginResponse = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            loginPayload,
            {
                headers: {
                    'Content-Type':
                        'application/json',
                },

                tags: {
                    endpoint:
                        'game-participation-login',

                    test_phase:
                    TEST_PHASE,

                    test_run:
                    TEST_RUN,
                },

                timeout: '10s',
            },
        );

        const loginBody =
            parseResponseBody(loginResponse);

        const accessToken =
            loginBody?.data?.accessToken;

        const loginSucceeded = check(
            loginResponse,
            {
                '테스트 사용자 로그인이 성공했다':
                    (response) =>
                        response.status === 200
                        && typeof accessToken
                        === 'string'
                        && accessToken.length > 0,
            },
            {
                endpoint:
                    'game-participation-login',
            },
        );

        if (!loginSucceeded) {
            throw new Error(
                '테스트 사용자 로그인 실패: '
                + `email=${email}, `
                + `status=${loginResponse.status}, `
                + `body=${loginResponse.body}`,
            );
        }

        accessTokens.push(accessToken);
    }

    if (accessTokens.length !== USER_COUNT) {
        throw new Error(
            '발급된 Access Token 수가 올바르지 않습니다. '
            + `expected=${USER_COUNT}, `
            + `actual=${accessTokens.length}`,
        );
    }

    return {
        accessTokens,
    };
}


/*
 * ============================================================
 * 경기 참가 동시 요청
 * ============================================================
 *
 * 각 VU는 서로 다른 JWT를 사용한다.
 *
 * __VU:
 * - 첫 번째 VU는 1
 * - 마지막 VU는 USER_COUNT
 *
 * 배열 인덱스는 0부터 시작하므로 __VU - 1을 사용한다.
 * ============================================================
 */

export function joinGame(testData) {
    const tokenIndex = __VU - 1;

    const accessToken =
        testData.accessTokens[tokenIndex];

    if (!accessToken) {
        throw new Error(
            'VU에 할당할 Access Token이 없습니다. '
            + `vu=${__VU}, `
            + `tokenIndex=${tokenIndex}`,
        );
    }

    const joinResponse = http.post(
        `${BASE_URL}/api/v1/games/${GAME_ID}`
        + '/participations',

        null,

        {
            headers: {
                Authorization:
                    `Bearer ${accessToken}`,

                Accept:
                    'application/json',
            },

            tags: {
                endpoint:
                    'game-participation',

                test_phase:
                TEST_PHASE,

                test_run:
                TEST_RUN,
            },

            /*
             * 201:
             * - 참가 성공
             *
             * 409:
             * - 정상적인 정원 초과 가능
             *
             * 409를 k6 자체 HTTP 실패에서 제외한 후,
             * 아래에서 FULL_HEADCOUNT_GAME인지 다시 검증한다.
             */
            responseCallback:
                http.expectedStatuses(
                    201,
                    409,
                ),

            timeout: '10s',
        },
    );

    const responseBody =
        parseResponseBody(joinResponse);

    const errorCode =
        responseBody?.errorCode;

    const isParticipationSuccess =
        joinResponse.status === 201;

    const isFullHeadcount =
        joinResponse.status === 409
        && errorCode === 'FULL_HEADCOUNT_GAME';

    const isSystemError =
        joinResponse.status >= 500
        || joinResponse.status === 0;

    const isExpectedResponse =
        isParticipationSuccess
        || isFullHeadcount;

    const isUnexpectedResponse =
        !isExpectedResponse
        && !isSystemError;


    /*
     * 각 Counter에 매 요청마다 값을 기록한다.
     *
     * 조건에 해당하면 1,
     * 해당하지 않으면 0을 기록한다.
     */

    participationSuccessCount.add(
        isParticipationSuccess ? 1 : 0,
    );

    participationFullCount.add(
        isFullHeadcount ? 1 : 0,
    );

    participationSystemErrorCount.add(
        isSystemError ? 1 : 0,
    );

    participationUnexpectedCount.add(
        isUnexpectedResponse ? 1 : 0,
    );

    participationExpectedResponseRate.add(
        isExpectedResponse,
    );


    check(
        joinResponse,
        {
            '참가 응답이 성공 또는 정상적인 정원 초과다':
                () => isExpectedResponse,

            '서버 오류가 발생하지 않았다':
                () => !isSystemError,

            '예상하지 못한 응답이 발생하지 않았다':
                () => !isUnexpectedResponse,
        },
        {
            endpoint:
                'game-participation',
        },
    );
}
