import http from 'k6/http';
import { check } from 'k6';


const BASE_URL = (
    __ENV.BASE_URL || 'http://localhost:8080'
).replace(/\/$/, '');

const QUERY_CASE =
    __ENV.QUERY_CASE || 'default';

const TEST_MODE =
    __ENV.TEST_MODE || 'smoke';

const TEST_PHASE =
    __ENV.TEST_PHASE || 'before-index';

const TEST_RUN =
    __ENV.TEST_RUN || 'run-01';

const FILTER_DATE =
    __ENV.FILTER_DATE;


const TEST_PROFILES = {
    smoke: {
        rate: 1,
        duration: '10s',
        preAllocatedVUs: 1,
        maxVUs: 5,
    },

    warmup: {
        rate: 5,
        duration: '30s',
        preAllocatedVUs: 5,
        maxVUs: 20,
    },

    baseline: {
        rate: 20,
        duration: '3m',
        preAllocatedVUs: 20,
        maxVUs: 100,
    },
};


const selectedProfile =
    TEST_PROFILES[TEST_MODE];


if (!selectedProfile) {
    throw new Error(
        `지원하지 않는 TEST_MODE입니다: ${TEST_MODE}. `
        + 'smoke, warmup, baseline 중 하나를 사용하세요.',
    );
}


const RATE = Number(
    __ENV.RATE || selectedProfile.rate,
);

const DURATION =
    __ENV.DURATION || selectedProfile.duration;

const PRE_ALLOCATED_VUS = Number(
    __ENV.PRE_ALLOCATED_VUS
    || selectedProfile.preAllocatedVUs,
);

const MAX_VUS = Number(
    __ENV.MAX_VUS
    || selectedProfile.maxVUs,
);

const P95_LIMIT_MS = Number(
    __ENV.P95_LIMIT_MS || 1000,
);


if (
    !Number.isFinite(RATE)
    || !Number.isFinite(PRE_ALLOCATED_VUS)
    || !Number.isFinite(MAX_VUS)
    || RATE <= 0
    || PRE_ALLOCATED_VUS <= 0
    || MAX_VUS < PRE_ALLOCATED_VUS
) {
    throw new Error(
        'RATE 또는 VU 설정값을 확인하세요.',
    );
}


function resolveRequestPath() {
    const commonPath =
        '/api/v1/games?page=0&size=10';

    switch (QUERY_CASE) {
        /*
         * A. 기본 시작 시간순
         */
        case 'default':
            return commonPath
                + '&sortType=START_TIME_ASC';

        /*
         * B. 날짜 필터
         */
        case 'date':
            if (!FILTER_DATE) {
                throw new Error(
                    'date 시나리오는 '
                    + 'FILTER_DATE가 필요합니다.',
                );
            }

            return commonPath
                + `&date=${encodeURIComponent(FILTER_DATE)}`
                + '&sortType=START_TIME_ASC';

        /*
         * C. 서울 지역 + 시작 시간순
         */
        case 'city':
            return commonPath
                + '&cityName=SEOUL'
                + '&sortType=START_TIME_ASC';

        /*
         * D. 서울 + 모집 중 + 3대3
         */
        case 'city-status-format':
            return commonPath
                + '&cityName=SEOUL'
                + '&gameStatus=RECRUITING'
                + '&matchFormat=THREE_ON_THREE'
                + '&sortType=START_TIME_ASC';

        /*
         * E. 최신순
         */
        case 'latest':
            return commonPath
                + '&sortType=LATEST';

        default:
            throw new Error(
                `지원하지 않는 QUERY_CASE입니다: ${QUERY_CASE}`,
            );
    }
}


const REQUEST_PATH =
    resolveRequestPath();


export const options = {
    scenarios: {
        game_list_filter_api: {
            executor: 'constant-arrival-rate',

            exec: 'getGameList',

            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,

            preAllocatedVUs:
            PRE_ALLOCATED_VUS,

            maxVUs:
            MAX_VUS,

            gracefulStop: '10s',

            tags: {
                scenario_name:
                    'game-list-filter-api',

                endpoint:
                    'game-list',

                query_case:
                QUERY_CASE,

                test_mode:
                TEST_MODE,

                test_phase:
                TEST_PHASE,

                test_run:
                TEST_RUN,
            },
        },
    },

    discardResponseBodies: true,

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
        'http_req_failed{endpoint:game-list}': [
            'rate<0.01',
        ],

        'http_req_duration{endpoint:game-list}': [
            `p(95)<${P95_LIMIT_MS}`,
        ],

        'checks{endpoint:game-list}': [
            'rate>0.99',
        ],

        dropped_iterations: [
            'count==0',
        ],
    },

    tags: {
        application:
            'basketball-matching',

        test_name:
            'game-list-filter-comparison',

        query_case:
        QUERY_CASE,

        test_mode:
        TEST_MODE,

        test_phase:
        TEST_PHASE,

        test_run:
        TEST_RUN,
    },
};


export function getGameList() {
    const response = http.get(
        `${BASE_URL}${REQUEST_PATH}`,
        {
            headers: {
                Accept: 'application/json',
            },

            tags: {
                name:
                    'GET /api/v1/games',

                endpoint:
                    'game-list',

                query_case:
                QUERY_CASE,
            },

            timeout: '10s',
        },
    );


    check(
        response,
        {
            '경기 목록 조회 상태 코드가 200이다':
                (result) => result.status === 200,
        },
        {
            endpoint:
                'game-list',

            query_case:
            QUERY_CASE,

            test_phase:
            TEST_PHASE,

            test_run:
            TEST_RUN,
        },
    );
}
