import http from 'k6/http';
import { check } from 'k6';


// 성능시험 대상 애플리케이션 주소
const BASE_URL = "http://localhost:8080";


const TEST_PHASE = 'before-index';

const TEST_MODE = 'baseline';

const TEST_RUN = 'run-03';

const TEST_PROFILES = {

    smoke: {
        rate: 1,
        duration: '10s',
        preAllocatedVUs: 1,
        maxVUs: 5
    },

    warmup: {
        rate: 10,
        duration: '30s',
        preAllocatedVUs: 10,
        maxVUs: 30
    },

    // 초당 50건을 3분간 요청
    // 응답이 느릴 때도 50RPS를 유지할 수 있도록 가상 사용자수를 충분히 확보한다.
    // maxVUs가 너무 작으면 서버가 아니라 k6의 가상 사용자 부족 때문에 dropped_iterations가 발생
    baseline: {
        rate: 50,
        duration: '3m',
        preAllocatedVUs: 50,
        maxVUs: 200
    }

};

const selectedProfile = TEST_PROFILES[TEST_MODE];

if (!selectedProfile) {
    throw Error(`지원하지 않는 테스트 모드입니다.: ${TEST_MODE}`)
}


export const options = {

    scenarios: {
        game_list_api: {
            /*
             * 가상 사용자 수를 고정하는 대신 초당 요청 수를 고정
             * 서버가 빠름 -> 적은 VU로 목표 RPS유지
             * 서버가 느림 -> 더 많은 VU를 사용해 목표 RPS 유지
             */
            executor: 'constant-arrival-rate',

            // 선택한 시험 모드의 초당 요청 수를 사용한다.
            // baseLine에서는 '1s'마다 50건의 요청을 실행한다.
            rate: selectedProfile.rate,
            timeUnit: '1s',
            duration: selectedProfile.duration,

            preAllocatedVUs: selectedProfile.preAllocatedVUs,

            maxVUs: selectedProfile.maxVUs,

            gracefulStop: '10s',

            tags: {
                scenario_name: 'game-list-api',
                endpoint: 'game-list',
                test_phase: TEST_PHASE,
                test_mode: TEST_MODE,
                test_run: TEST_RUN,
            },
        },

    },

    // 응답 본문 폐기
    discardResponseBodies: true,

    /*
     * 출력 통계
     * 평균, 최소, 중간, 최대, p90, p95, p99
     * 전체 요청의 95%가 p95 시간 이내에 완료되고,
     * 나머지 5%는 그보다 오래 걸린다
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

        // HTTP 실패율이 1% 미만
        'http_req_failed{endpoint:game-list}': [
            'rate<0.01',
        ],

        // 전체 요청의 95%가 1초 안에 완료되는지를 확인
        'http_req_duration{endpoint:game-list}': [
            'p(95) < 1000',
        ],

        // HTTP 200 검증이 99%를 초과해야 한다.
        'checks{endpoint:game-list}': [
            'rate>0.99',
        ],

        // k6가 목표로 한 요청을 하나도 누락하지 않아야 한다는 기준
        dropped_iterations : [
            'count==0',
        ],
    },

    tags: {
        application: 'basketball-matching',
        test_name: 'game-list-latest-index-retest',
        test_phase: TEST_PHASE,
        test_mode: TEST_MODE,
        test_run: TEST_RUN,
    },

}


export default function getGameList() {
    const response = http.get(

        // 실제 호출되는 주소
        `${BASE_URL}/api/v1/games`
        + '?page=0'
        + '&size=10'
        + '&sortType=LATEST',
        {
            headers: {
                Accept: 'application/json',
            },
            tags: {
                // 요청 주소에 조회 조건이 추가되더라도 k6 결과에서는 같은 API로 묶어서 집계하기 위한 태그
                name: 'GET /api/v1/games',
                // 다른 API 요청이 추가되더라도 경기 목록 요청의 지연시간만 따로 판단이 가능하다.
                endpoint: 'game-list',
                query_case: 'latest-only'
            },

            // 하나의 요청이 10초를 넘기면 실패로 간주
            timeout: '10s',
        },
    );

    check(
        response,
        {
            '경기 목록 상태 코드가 200이다':
                (result) => result.status === 200,
        },
        {
            endpoint: 'game-list',
            query_case: 'latest-only',
            test_phase: TEST_PHASE,
            test_mode: TEST_MODE,
            test_run: TEST_RUN,
        },
    );
}