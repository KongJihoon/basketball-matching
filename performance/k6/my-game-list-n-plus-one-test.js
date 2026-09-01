import http from 'k6/http';
import { check } from 'k6';
import {
    Counter,
    Rate,
} from 'k6/metrics';


/*
 * ============================================================
 * 기본 설정
 * ============================================================
 */

const BASE_URL = (
    __ENV.BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '');

const TEST_MODE =
    __ENV.TEST_MODE || 'smoke';

const TEST_PHASE =
    __ENV.TEST_PHASE || 'before';

const TEST_RUN =
    __ENV.TEST_RUN || 'run-01';

const TEST_EMAIL =
    __ENV.TEST_EMAIL
    || 'perf-my-game-user@example.test';

const TEST_PASSWORD =
    __ENV.TEST_PASSWORD || 'Perf@1234';

const PAGE_SIZE = Number(
    __ENV.PAGE_SIZE || 100,
);

const P95_LIMIT_MS = Number(
    __ENV.P95_LIMIT_MS || 5000,
);

const ALLOW_NON_LOCAL =
    __ENV.ALLOW_NON_LOCAL === 'true';

const REQUEST_PATH =
    '/api/v1/mypage/games/upcoming'
    + `?page=0&size=${PAGE_SIZE}`;


/*
 * ============================================================
 * 안전 검증
 * ============================================================
 */

const isLocalTarget =
    /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/
        .test(BASE_URL);

if (!isLocalTarget && !ALLOW_NON_LOCAL) {
    throw new Error(
        '로컬 주소가 아닌 대상에는 '
        + '성능 테스트를 실행할 수 없습니다. '
        + `BASE_URL=${BASE_URL}`,
    );
}

if (!Number.isInteger(PAGE_SIZE) || PAGE_SIZE < 1 || PAGE_SIZE > 100) {
    throw new Error(
        `PAGE_SIZE는 1~100 정수여야 합니다: ${PAGE_SIZE}`,
    );
}


/*
 * ============================================================
 * 부하 프로필
 * ============================================================
 */

const TEST_PROFILES = {
    smoke: {
        startRate: 1,
        preAllocatedVUs: 2,
        maxVUs: 10,
        stages: [
            {
                target: 1,
                duration: '10s',
            },
        ],
    },

    baseline: {
        startRate: 5,
        preAllocatedVUs: 100,
        maxVUs: 300,
        stages: [
            {
                target: 5,
                duration: '30s',
            },
            {
                target: 10,
                duration: '30s',
            },
            {
                target: 20,
                duration: '1m',
            },
            {
                target: 30,
                duration: '1m',
            },
            {
                target: 0,
                duration: '15s',
            },
        ],
    },

    stress: {
        startRate: 30,
        preAllocatedVUs: 100,
        maxVUs: 500,
        stages: [
            {
                target: 30,
                duration: '30s',
            },
            {
                target: 50,
                duration: '30s',
            },
            {
                target: 100,
                duration: '1m',
            },
            {
                target: 150,
                duration: '1m',
            },
            {
                target: 200,
                duration: '1m',
            },
            {
                target: 0,
                duration: '15s',
            },
        ],
    },
};

const selectedProfile =
    TEST_PROFILES[TEST_MODE];

if (!selectedProfile) {
    throw new Error(
        `지원하지 않는 TEST_MODE입니다: ${TEST_MODE}. `
        + 'smoke, baseline 또는 stress를 사용하세요.',
    );
}


/*
 * ============================================================
 * 사용자 정의 지표
 * ============================================================
 */

const expectedPageSizeRate =
    new Rate('my_game_list_expected_page_size');

const systemErrorCount =
    new Counter('my_game_list_system_error');

const unexpectedResponseCount =
    new Counter('my_game_list_unexpected_response');


/*
 * ============================================================
 * k6 실행 설정
 * ============================================================
 */

export const options = {
    setupTimeout: '30s',

    scenarios: {
        my_game_list_query: {
            executor: 'ramping-arrival-rate',
            exec: 'getUpcomingGames',

            startRate:
                selectedProfile.startRate,

            timeUnit: '1s',

            preAllocatedVUs:
                selectedProfile.preAllocatedVUs,

            maxVUs:
                selectedProfile.maxVUs,

            stages:
                selectedProfile.stages,

            gracefulStop: '15s',

            tags: {
                scenario_name:
                    'my-game-list-query',

                endpoint:
                    'my-game-list-upcoming',

                test_mode:
                    TEST_MODE,

                test_phase:
                    TEST_PHASE,

                test_run:
                    TEST_RUN,

                page_size:
                    String(PAGE_SIZE),
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
        'http_req_failed{endpoint:my-game-list-upcoming}': [
            'rate<0.01',
        ],

        'http_req_duration{endpoint:my-game-list-upcoming}': [
            `p(95)<${P95_LIMIT_MS}`,
        ],

        'checks{endpoint:my-game-list-upcoming}': [
            'rate>0.99',
        ],

        my_game_list_expected_page_size: [
            'rate>0.99',
        ],

        my_game_list_system_error: [
            'count==0',
        ],

        my_game_list_unexpected_response: [
            'count==0',
        ],

        dropped_iterations: [
            'count==0',
        ],
    },

    tags: {
        application:
            'basketball-matching',

        test_name:
            'my-game-list-n-plus-one',

        test_mode:
            TEST_MODE,

        test_phase:
            TEST_PHASE,

        test_run:
            TEST_RUN,

        page_size:
            String(PAGE_SIZE),
    },
};


/*
 * ============================================================
 * 응답 파싱
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
 * 로그인 및 데이터 사전 검증
 * ============================================================
 */

export function setup() {
    const loginResponse = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            email: TEST_EMAIL,
            password: TEST_PASSWORD,
        }),
        {
            headers: {
                'Content-Type':
                    'application/json',
            },

            tags: {
                endpoint:
                    'my-game-list-login',

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
                    && typeof accessToken === 'string'
                    && accessToken.length > 0,
        },
        {
            endpoint:
                'my-game-list-login',
        },
    );

    if (!loginSucceeded) {
        throw new Error(
            '테스트 사용자 로그인에 실패했습니다. '
            + `status=${loginResponse.status}`,
        );
    }

    const validationResponse = http.get(
        `${BASE_URL}${REQUEST_PATH}`,
        {
            headers: {
                Authorization:
                    `Bearer ${accessToken}`,

                Accept:
                    'application/json',
            },

            tags: {
                endpoint:
                    'my-game-list-setup-validation',

                test_phase:
                    TEST_PHASE,

                test_run:
                    TEST_RUN,
            },

            timeout: '10s',
        },
    );

    const validationBody =
        parseResponseBody(validationResponse);

    const returnedElements =
        validationBody?.data?.numberOfElements;

    if (
        validationResponse.status !== 200
        || returnedElements !== PAGE_SIZE
    ) {
        throw new Error(
            '재현 데이터 검증에 실패했습니다. '
            + `status=${validationResponse.status}, `
            + `expectedElements=${PAGE_SIZE}, `
            + `actualElements=${returnedElements}`,
        );
    }

    return {
        accessToken,
    };
}


/*
 * ============================================================
 * 내 예정 경기 목록 조회
 * ============================================================
 */

export function getUpcomingGames(testData) {
    const response = http.get(
        `${BASE_URL}${REQUEST_PATH}`,
        {
            headers: {
                Authorization:
                    `Bearer ${testData.accessToken}`,

                Accept:
                    'application/json',
            },

            tags: {
                name:
                    'GET /api/v1/mypage/games/upcoming',

                endpoint:
                    'my-game-list-upcoming',

                test_phase:
                    TEST_PHASE,

                test_run:
                    TEST_RUN,

                page_size:
                    String(PAGE_SIZE),
            },

            timeout: '10s',
        },
    );

    const responseBody =
        parseResponseBody(response);

    const returnedElements =
        responseBody?.data?.numberOfElements;

    const statusSucceeded =
        response.status === 200;

    const expectedSizeReturned =
        returnedElements === PAGE_SIZE;

    const isSystemError =
        response.status === 0
        || response.status >= 500;

    const isUnexpectedResponse =
        !statusSucceeded
        && !isSystemError;

    expectedPageSizeRate.add(
        expectedSizeReturned,
    );

    systemErrorCount.add(
        isSystemError ? 1 : 0,
    );

    unexpectedResponseCount.add(
        isUnexpectedResponse ? 1 : 0,
    );

    check(
        response,
        {
            '내 예정 경기 목록 응답이 200이다':
                () => statusSucceeded,

            '예상한 페이지 크기를 반환했다':
                () => expectedSizeReturned,

            '서버 오류가 발생하지 않았다':
                () => !isSystemError,
        },
        {
            endpoint:
                'my-game-list-upcoming',

            test_phase:
                TEST_PHASE,

            test_run:
                TEST_RUN,
        },
    );
}
