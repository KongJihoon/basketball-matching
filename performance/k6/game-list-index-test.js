import http from 'k6/http';
import { check } from 'k6';

/*
 * 경기 목록 조회 API 인덱스 성능 비교 테스트
 *
 * 대상:
 * GET /api/v1/games?page=0&size=10
 *
 * 목적:
 * - 인덱스 적용 전과 후에 동일한 RPS를 가한다.
 * - 평균, p90, p95, p99 응답시간을 비교한다.
 * - 오류율, 처리량, dropped iteration을 비교한다.
 * - Prometheus로 전송하여 Grafana에서 서버 자원을 확인한다.
 */

/* =========================================================
 * 1. 실행 환경
 * ========================================================= */

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080')
    .replace(/\/$/, '');

const TEST_MODE = __ENV.TEST_MODE || 'baseline';
const TEST_PHASE = __ENV.TEST_PHASE || 'before-index';
const TEST_RUN = __ENV.TEST_RUN || 'run-01';

/*
 * 테스트 모드별 기본값
 *
 * smoke:
 *   API 연결 및 상태 코드만 확인한다.
 *
 * warmup:
 *   JVM JIT, HikariCP, MySQL Buffer Pool을 예열한다.
 *
 * baseline:
 *   인덱스 적용 전후의 실제 비교에 사용한다.
 */
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

const selectedProfile = TEST_PROFILES[TEST_MODE];

if (!selectedProfile) {
    throw new Error(
        `지원하지 않는 TEST_MODE입니다: ${TEST_MODE}. ` +
        'smoke, warmup, baseline 중 하나를 사용하세요.',
    );
}

/*
 * 환경변수를 지정하면 프로필의 기본값을 덮어쓸 수 있다.
 *
 * 예:
 * RATE=30 DURATION=1m k6 run ...
 */
const RATE = Number(__ENV.RATE || selectedProfile.rate);
const DURATION = __ENV.DURATION || selectedProfile.duration;

const PRE_ALLOCATED_VUS = Number(
    __ENV.PRE_ALLOCATED_VUS ||
    selectedProfile.preAllocatedVUs,
);

const MAX_VUS = Number(
    __ENV.MAX_VUS ||
    selectedProfile.maxVUs,
);

const P95_LIMIT_MS = Number(__ENV.P95_LIMIT_MS || 1000);

if (
    !Number.isFinite(RATE) ||
    !Number.isFinite(PRE_ALLOCATED_VUS) ||
    !Number.isFinite(MAX_VUS) ||
    RATE <= 0 ||
    PRE_ALLOCATED_VUS <= 0 ||
    MAX_VUS < PRE_ALLOCATED_VUS
) {
    throw new Error('RATE 또는 VU 설정값을 확인하세요.');
}

/* =========================================================
 * 2. k6 테스트 설정
 * ========================================================= */

export const options = {
    scenarios: {
        game_list_api: {
            /*
             * 서버 응답속도와 상관없이 일정한 요청률을 유지한다.
             *
             * 기본값:
             * 초당 20건 × 3분 = 약 3,600건
             */
            executor: 'constant-arrival-rate',

            exec: 'getGameList',

            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,

            preAllocatedVUs: PRE_ALLOCATED_VUS,
            maxVUs: MAX_VUS,

            gracefulStop: '10s',

            tags: {
                scenario_name: 'game-list-api',
                test_mode: TEST_MODE,
                test_phase: TEST_PHASE,
                test_run: TEST_RUN,
            },
        },
    },

    /*
     * 응답 본문을 메모리에 보관하지 않는다.
     * 상태 코드만 확인하므로 본문은 필요하지 않다.
     */
    discardResponseBodies: true,

    /*
     * 터미널에 출력할 응답시간 통계
     */
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
         * 경기 목록 API의 요청 실패율이 1% 미만이어야 한다.
         */
        'http_req_failed{endpoint:game-list}': [
            'rate<0.01',
        ],

        /*
         * p95 응답시간 기본 기준은 1초다.
         *
         * 실행 시 P95_LIMIT_MS로 변경할 수 있다.
         */
        'http_req_duration{endpoint:game-list}': [
            `p(95)<${P95_LIMIT_MS}`,
        ],

        /*
         * 상태 코드 검증 성공률이 99%를 초과해야 한다.
         */
        'checks{endpoint:game-list}': [
            'rate>0.99',
        ],

        /*
         * 설정된 RPS를 발생시키지 못하고 버린 요청이 없어야 한다.
         */
        dropped_iterations: [
            'count==0',
        ],
    },

    /*
     * 모든 k6 메트릭에 공통으로 추가되는 태그
     */
    tags: {
        application: 'basketball-matching',
        test_name: 'game-list-index-comparison',
        test_mode: TEST_MODE,
        test_phase: TEST_PHASE,
        test_run: TEST_RUN,
    },
};

/* =========================================================
 * 3. 경기 목록 조회
 * ========================================================= */

export function getGameList() {
    const response = http.get(
        `${BASE_URL}/api/v1/games?page=0&size=10`,
        {
            headers: {
                Accept: 'application/json',
            },

            tags: {
                /*
                 * name 태그로 동적 URL이 하나의 요청으로 집계된다.
                 */
                name: 'GET /api/v1/games',
                endpoint: 'game-list',
            },

            timeout: '10s',
        },
    );

    check(
        response,
        {
            '경기 목록 조회 상태 코드가 200이다': (res) =>
                res.status === 200,
        },
        {
            endpoint: 'game-list',
            test_phase: TEST_PHASE,
            test_run: TEST_RUN,
        },
    );
}