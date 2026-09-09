#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# 1. 수신자 수별 측정 설정
# ============================================================
SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIRECTORY}/../.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_MODE="${TEST_MODE:-recipient-scaling}"
GAME_IDS="${GAME_IDS:-}"
GAME_ID="${GAME_ID:-265}"
RECEIVER_COUNT="${RECEIVER_COUNT:-1}"
TEST_PHASE="${TEST_PHASE:-before}"
TEST_RUN="${TEST_RUN:-run-01}"
ENABLE_PROMETHEUS="${ENABLE_PROMETHEUS:-true}"
PROMETHEUS_REMOTE_WRITE_URL="${PROMETHEUS_REMOTE_WRITE_URL:-http://localhost:9090/api/v1/write}"

[[ "$GAME_ID" =~ ^[1-9][0-9]*$ ]] || { echo 'GAME_ID를 확인하세요.'; exit 1; }
case "$TEST_MODE" in
    recipient-scaling) ;;
    load)
        [[ "$GAME_IDS" =~ ^[1-9][0-9]*(,[1-9][0-9]*){9}$ && "$RECEIVER_COUNT" == 99 ]] \
            || { echo 'load 모드는 GAME_IDS 10개와 RECEIVER_COUNT=99가 필요합니다.'; exit 1; }
        ;;
    *) echo 'TEST_MODE는 recipient-scaling/load입니다.'; exit 1;;
esac
case "$RECEIVER_COUNT" in 1|10|50|99) ;; *) echo '수신자는 1/10/50/99명입니다.'; exit 1;; esac
case "$TEST_PHASE" in before|after) ;; *) echo 'TEST_PHASE는 before/after입니다.'; exit 1;; esac
case "$ENABLE_PROMETHEUS" in true|false) ;; *) echo 'ENABLE_PROMETHEUS는 true/false입니다.'; exit 1;; esac
[[ "$TEST_RUN" =~ ^[a-zA-Z0-9-]{1,24}$ ]] || { echo 'TEST_RUN은 영문/숫자/하이픈 1~24자입니다.'; exit 1; }
[[ "$BASE_URL" =~ ^http://(localhost|127\.0\.0\.1):[0-9]+$ ]] || { echo '로컬 HTTP 주소만 허용합니다.'; exit 1; }

for dependency in k6 curl jq; do
    command -v "$dependency" >/dev/null || { echo "$dependency 설치가 필요합니다."; exit 1; }
done
curl -fsS --max-time 5 "${BASE_URL}/actuator/health" | jq -e '.status == "UP"' >/dev/null

# 실행마다 다른 제목 접두어 사용. 결과 파일은 덮어쓰지 않는다.
RUN_KEY="$(date -u +%Y%m%dT%H%M%S)-${RECEIVER_COUNT}-$$"
RESULT_DIRECTORY="${PROJECT_ROOT}/performance/results/game-update-notification-async/${TEST_PHASE}/k6"
SUMMARY_FILE="${RESULT_DIRECTORY}/receivers-${RECEIVER_COUNT}-${TEST_RUN}-summary.json"
if [[ "$TEST_MODE" == load ]]; then
    SUMMARY_FILE="${RESULT_DIRECTORY}/load-${TEST_RUN}-summary.json"
fi
[[ ! -e "$SUMMARY_FILE" ]] || { echo "이미 결과가 있습니다. TEST_RUN을 변경하세요: $SUMMARY_FILE"; exit 1; }
mkdir -p "$RESULT_DIRECTORY"

# ============================================================
# 2. 실행 정보 및 Prometheus 옵션
# ============================================================
printf '경기 수정 알림 측정\nGAME_ID: %s\n수신자: %s\nPHASE: %s\nRUN: %s\nTITLE_PREFIX: PERF_UPDATE_%s_\nRESULT: %s\n' \
    "$GAME_ID" "$RECEIVER_COUNT" "$TEST_PHASE" "$TEST_RUN" "$RUN_KEY" "$SUMMARY_FILE"
if [[ "$TEST_MODE" == load ]]; then
    printf 'GAME_IDS: %s\n조건: 5 RPS 30초 → 10/20/30 RPS 각 60초, 단계 사이 15초\n계획: 약 3750회 수정 / 371250건 알림 (워밍업 포함)\n' "$GAME_IDS"
else
    printf '조건: 1 RPS / 30초\n'
fi
K6_OUTPUT_ARGUMENTS=()
if [[ "$ENABLE_PROMETHEUS" == true ]]; then
    K6_OUTPUT_ARGUMENTS+=(--out experimental-prometheus-rw)
fi

set +e
BASE_URL="$BASE_URL" GAME_ID="$GAME_ID" RECEIVER_COUNT="$RECEIVER_COUNT" \
TEST_MODE="$TEST_MODE" GAME_IDS="$GAME_IDS" \
TEST_PHASE="$TEST_PHASE" TEST_RUN="$TEST_RUN" RUN_KEY="$RUN_KEY" SUMMARY_FILE="$SUMMARY_FILE" \
TEST_EMAIL="${TEST_EMAIL:-perf-update-creator@example.test}" \
TEST_PASSWORD="${TEST_PASSWORD:-Perf@1234}" \
K6_PROMETHEUS_RW_SERVER_URL="$PROMETHEUS_REMOTE_WRITE_URL" \
K6_PROMETHEUS_RW_TREND_STATS='p(90),p(95),p(99),min,max,avg' \
k6 run "${K6_OUTPUT_ARGUMENTS[@]}" \
    "${PROJECT_ROOT}/performance/k6/game-update-notification-async-test.js"
K6_EXIT_CODE=$?
set -e

if [[ -f "$SUMMARY_FILE" ]]; then
    if ! jq -e '.metadata.all_attempts_succeeded == true' "$SUMMARY_FILE" >/dev/null; then
        echo '실행 요청 수와 성공 요청 수가 일치하지 않습니다. JSON과 서버 로그를 확인하세요.'
        K6_EXIT_CODE=99
    fi
fi

printf '\nk6 종료 코드: %s\n알림 검증 SQL의 @perf_update_title_prefix에 아래 값을 입력하세요:\nPERF_UPDATE_%s_\n' "$K6_EXIT_CODE" "$RUN_KEY"
exit "$K6_EXIT_CODE"
