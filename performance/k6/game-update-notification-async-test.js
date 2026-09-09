import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

/* ============================================================
 * 1. 수신자 수별 비교 및 단계별 부하 설정
 * ============================================================ */
const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const GAME_ID = Number(__ENV.GAME_ID);
const TEST_MODE = __ENV.TEST_MODE || 'recipient-scaling';
const GAME_IDS = TEST_MODE === 'load'
    ? (__ENV.GAME_IDS || '').split(',').map(Number) : [GAME_ID];
const RECEIVER_COUNT = Number(__ENV.RECEIVER_COUNT);
const TEST_PHASE = __ENV.TEST_PHASE || 'before';
const TEST_RUN = __ENV.TEST_RUN || 'run-01';
const RUN_KEY = __ENV.RUN_KEY;
const SUMMARY_FILE = __ENV.SUMMARY_FILE;
const TITLE_PREFIX = `PERF_UPDATE_${RUN_KEY}_`;
const ENDPOINT = 'game-update-notification';

if (!/^http:\/\/(localhost|127\.0\.0\.1):\d+$/.test(BASE_URL)) {
    throw new Error('이 테스트는 로컬 HTTP 주소에서만 실행할 수 있습니다.');
}
if (!['recipient-scaling', 'load'].includes(TEST_MODE)
    || GAME_IDS.some(id => !Number.isSafeInteger(id) || id < 1)
    || (TEST_MODE === 'load' && (GAME_IDS.length !== 10
        || new Set(GAME_IDS).size !== 10 || RECEIVER_COUNT !== 99))
    || ![1, 10, 50, 99].includes(RECEIVER_COUNT)
    || !['before', 'after'].includes(TEST_PHASE)
    || !/^[a-zA-Z0-9-]{1,48}$/.test(RUN_KEY || '')
    || !SUMMARY_FILE) {
    throw new Error('GAME_ID, RECEIVER_COUNT, TEST_PHASE, RUN_KEY, SUMMARY_FILE을 확인하세요.');
}

const attempted = new Counter('notification_update_attempted');
const succeeded = new Counter('notification_update_succeeded');
const unexpected = new Counter('notification_update_unexpected');

// 각 단계는 고정 RPS. 종료 후 15초를 두어 이전 단계 HTTP 요청과 겹치지 않게 한다.
// 비동기 적용 후에는 이 간격만으로 큐 소진을 보장하지 않는다. 큐 적체를 함께 관측한다.
const PROFILES = TEST_MODE === 'load' ? [
    { name: 'warmup_5rps', rate: 5, seconds: 30, start: 0 },
    { name: 'load_10rps', rate: 10, seconds: 60, start: 45 },
    { name: 'load_20rps', rate: 20, seconds: 60, start: 120 },
    { name: 'load_30rps', rate: 30, seconds: 60, start: 195 },
] : [{ name: 'recipient_scaling', rate: 1, seconds: 30, start: 0 }];
const plannedRequests = PROFILES.reduce((sum, profile) => sum + profile.rate * profile.seconds, 0);
const scenarios = {};
const stageThresholds = {};
for (const profile of PROFILES) {
    scenarios[profile.name] = {
        executor: 'constant-arrival-rate', rate: profile.rate,
        timeUnit: '1s', duration: `${profile.seconds}s`, startTime: `${profile.start}s`,
        preAllocatedVUs: TEST_MODE === 'load' ? 50 : 2,
        maxVUs: TEST_MODE === 'load' ? 200 : 10,
        gracefulStop: '15s', exec: 'updateGame',
        tags: { load_stage: profile.name },
    };
    stageThresholds[`http_req_duration{endpoint:${ENDPOINT},scenario:${profile.name}}`] = ['p(95)>=0'];
    stageThresholds[`notification_update_succeeded{scenario:${profile.name}}`] = ['count>=0'];
    stageThresholds[`notification_update_attempted{scenario:${profile.name}}`] = ['count>=0'];
}

export const options = {
    setupTimeout: '2m',
    scenarios,
    tags: {
        application: 'basketball-matching',
        test_name: 'game-update-notification-async',
        test_mode: TEST_MODE,
        test_phase: TEST_PHASE,
        test_run: TEST_RUN,
        receiver_count: String(RECEIVER_COUNT),
        testid: RUN_KEY,
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    thresholds: {
        [`http_req_failed{endpoint:${ENDPOINT}}`]: ['rate==0'],
        [`checks{endpoint:${ENDPOINT}}`]: ['rate==1'],
        // 시간 기반 실행의 경계에서 계획보다 요청이 추가될 수 있다.
        // 성공 건수와 실제 실행 건수의 일치는 실행 셸에서도 검증한다.
        notification_update_attempted: [`count>=${plannedRequests}`],
        notification_update_succeeded: [`count>=${plannedRequests}`],
        notification_update_unexpected: ['count==0'],
        dropped_iterations: ['count==0'],
        // p95은 이 단계에서 기준선을 수집한다. 임의 목표치로 통과시키지 않는다.
        [`http_req_duration{endpoint:${ENDPOINT}}`]: ['p(95)>=0'],
        ...stageThresholds,
    },
};

function bodyOf(response) {
    try { return response.json(); } catch (_) { return null; }
}

/* ============================================================
 * 2. 로그인 및 전용 경기 확인 (측정 PATCH와 다른 endpoint 태그)
 * ============================================================ */
export function setup() {
    const login = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
        email: __ENV.TEST_EMAIL || 'perf-update-creator@example.test',
        password: __ENV.TEST_PASSWORD || 'Perf@1234',
    }), {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: 'notification-setup-login' },
        timeout: '10s',
    });
    const token = bodyOf(login)?.data?.accessToken;
    if (login.status !== 200 || !token) {
        throw new Error(`테스트 계정 로그인 실패: HTTP ${login.status}`);
    }
    for (const gameId of GAME_IDS) {
    const detail = http.get(`${BASE_URL}/api/v1/games/${gameId}`, {
        headers: { Authorization: `Bearer ${token}` },
        tags: { endpoint: 'notification-setup-validation' },
        timeout: '10s',
    });
    const game = bodyOf(detail)?.data;
    const court = game?.placeName || '';
    const expectedCourt = { 1: '00', 10: '01', 50: '02' }[RECEIVER_COUNT];
    const validCourt = expectedCourt
        ? court === `PERF_UPDATE_NOTIFICATION_COURT_${expectedCourt}`
        : /^PERF_UPDATE_NOTIFICATION_COURT_(0[3-9]|1[0-2])$/.test(court);
    // API의 LocalDateTime은 프로젝트의 Asia/Seoul 기준이다.
    const start = Date.parse(`${game?.startDateTime}+09:00`);
    if (detail.status !== 200 || game?.gameId !== gameId || !validCourt
        || game?.creatorNickname !== 'perf_update_creator'
        || game?.participantCount !== RECEIVER_COUNT + 1
        || !(start > Date.now() + 24 * 60 * 60 * 1000)) {
        throw new Error('전용 경기/수신자 수/수정 가능 일정을 확인하세요. PATCH는 실행하지 않았습니다.');
    }
    }
    console.log(`알림 검증용 title_prefix: ${TITLE_PREFIX}`);
    return { token };
}

/* ============================================================
 * 3. 매 요청 고유 제목으로 실제 변경 이벤트 발행
 * ============================================================ */
export function updateGame(data) {
    const stage = exec.scenario.name;
    const gameId = GAME_IDS[exec.scenario.iterationInTest % GAME_IDS.length];
    const title = `${TITLE_PREFIX}${stage}_${exec.scenario.iterationInTest}`;
    attempted.add(1);
    const response = http.patch(`${BASE_URL}/api/v1/games/${gameId}`,
        JSON.stringify({ title }), {
            headers: {
                Authorization: `Bearer ${data.token}`,
                'Content-Type': 'application/json',
            },
            tags: { name: 'PATCH /api/v1/games/{gameId}', endpoint: ENDPOINT },
            timeout: '10s',
        });
    const result = bodyOf(response)?.data;
    const passed = check(response, {
        '경기 수정 응답이 200이다': r => r.status === 200,
        '요청한 경기와 제목이 반환됐다': () => result?.gameId === gameId && result?.title === title,
        '참가 인원이 유지됐다': () => result?.participantCount === RECEIVER_COUNT + 1,
    }, { endpoint: ENDPOINT });
    succeeded.add(passed ? 1 : 0);
    unexpected.add(passed ? 0 : 1);
}

/* ============================================================
 * 4. 토큰/setup_data를 제외한 JSON 및 콘솔 결과
 * ============================================================ */
export function handleSummary(data) {
    const count = name => data.metrics[name]?.values?.count || 0;
    const successCount = count('notification_update_succeeded');
    const attemptedCount = count('notification_update_attempted');
    const latency = data.metrics[`http_req_duration{endpoint:${ENDPOINT}}`]?.values || {};
    const result = {
        metadata: {
            test_phase: TEST_PHASE, test_run: TEST_RUN, run_key: RUN_KEY,
            test_mode: TEST_MODE, game_ids: GAME_IDS, receiver_count: RECEIVER_COUNT,
            title_prefix: TITLE_PREFIX, profiles: PROFILES,
            planned_requests: plannedRequests, attempted_requests: attemptedCount,
            successful_requests: successCount,
            all_attempts_succeeded: attemptedCount > 0 && attemptedCount === successCount,
            expected_notifications_for_successful_requests: successCount * RECEIVER_COUNT,
            notification_delivery_verified: false,
            note: 'HTTP 결과만 기록. 실제 알림 저장 건수와 수신자 중복은 별도 SQL로 검증한다.',
        },
        stages: PROFILES.map(profile => ({
            ...profile,
            title_prefix: `${TITLE_PREFIX}${profile.name}_`,
            attempted_requests: count(`notification_update_attempted{scenario:${profile.name}}`),
            successful_requests: count(`notification_update_succeeded{scenario:${profile.name}}`),
            expected_notifications: count(`notification_update_succeeded{scenario:${profile.name}}`) * RECEIVER_COUNT,
            latency: data.metrics[`http_req_duration{endpoint:${ENDPOINT},scenario:${profile.name}}`]?.values || {},
        })),
        metrics: data.metrics,
    };
    return {
        [SUMMARY_FILE]: JSON.stringify(result, null, 2),
        stdout: `\n경기 수정 결과: ${successCount}/${attemptedCount} 성공 (계획 ${plannedRequests})\n`
            + PROFILES.map(profile => {
                const values = data.metrics[`http_req_duration{endpoint:${ENDPOINT},scenario:${profile.name}}`]?.values || {};
                return `${profile.name} p95: ${values['p(95)'] ?? '측정 없음'} ms`;
            }).join('\n') + '\n'
            + `dropped_iterations: ${count('dropped_iterations')}\n`
            + `PATCH p95: ${latency['p(95)'] ?? '측정 없음'} ms\n`
            + `예상 알림: ${successCount * RECEIVER_COUNT}건 (SQL 검증 필요)\n`
            + `title_prefix: ${TITLE_PREFIX}\nJSON: ${SUMMARY_FILE}\n`,
    };
}
