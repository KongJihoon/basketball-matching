#!/usr/bin/env bash

set -euo pipefail


# ============================================================
# 1. 프로젝트 경로 계산
# ============================================================

SCRIPT_DIRECTORY="$(
    cd "$(dirname "${BASH_SOURCE[0]}")"
    pwd
)"

PROJECT_ROOT="$(
    cd "${SCRIPT_DIRECTORY}/../.."
    pwd
)"


# ============================================================
# 2. 테스트 환경변수
# ============================================================

GAME_ID="${GAME_ID:-}"

BASE_URL="${BASE_URL:-http://localhost:8080}"

TEST_PHASE="${TEST_PHASE:-before-lock}"

TEST_RUN="${TEST_RUN:-run-01}"

USER_COUNT="${USER_COUNT:-100}"

EXPECTED_SUCCESS_COUNT="${EXPECTED_SUCCESS_COUNT:-5}"

TEST_PASSWORD="${TEST_PASSWORD:-Perf@1234}"

P95_LIMIT_MS="${P95_LIMIT_MS:-5000}"

SETUP_TIMEOUT="${SETUP_TIMEOUT:-5m}"

OVERWRITE="${OVERWRITE:-false}"

ENABLE_PROMETHEUS="${ENABLE_PROMETHEUS:-true}"

PROMETHEUS_REMOTE_WRITE_URL="${PROMETHEUS_REMOTE_WRITE_URL:-http://localhost:9090/api/v1/write}"

PROMETHEUS_TREND_STATS="${PROMETHEUS_TREND_STATS:-p(90),p(95),p(99),min,max,avg}"


# ============================================================
# 3. 테스트 파일 및 결과 경로
# ============================================================

TEST_ID="${TEST_PHASE}-game-${GAME_ID:-unknown}-${TEST_RUN}"

K6_SCRIPT="${PROJECT_ROOT}/performance/k6/game-participation-concurrency-test.js"

RESULT_DIRECTORY="${PROJECT_ROOT}/performance/results/game-participation-concurrency/${TEST_PHASE}/k6"

SUMMARY_FILE="${RESULT_DIRECTORY}/game-participation-${TEST_RUN}-summary.json"


# ============================================================
# 4. 입력값 검증
# ============================================================

if [[ -z "${GAME_ID}" ]]; then
    echo "GAME_ID 환경변수가 필요합니다."
    echo
    echo "예시:"
    echo "GAME_ID=1 \\"
    echo "TEST_PHASE=before-lock \\"
    echo "TEST_RUN=run-01 \\"
    echo "./performance/scripts/run-game-participation-concurrency-test.sh"
    exit 1
fi


if ! [[ "${GAME_ID}" =~ ^[0-9]+$ ]]; then
    echo "GAME_ID는 숫자여야 합니다."
    echo "입력값: ${GAME_ID}"
    exit 1
fi


if ! [[ "${USER_COUNT}" =~ ^[0-9]+$ ]] \
    || [[ "${USER_COUNT}" -le 0 ]]; then

    echo "USER_COUNT는 1 이상의 숫자여야 합니다."
    echo "입력값: ${USER_COUNT}"
    exit 1
fi


if ! [[ "${EXPECTED_SUCCESS_COUNT}" =~ ^[0-9]+$ ]] \
    || [[ "${EXPECTED_SUCCESS_COUNT}" -gt "${USER_COUNT}" ]]; then

    echo "EXPECTED_SUCCESS_COUNT를 확인하세요."
    echo "USER_COUNT: ${USER_COUNT}"
    echo "EXPECTED_SUCCESS_COUNT: ${EXPECTED_SUCCESS_COUNT}"
    exit 1
fi


case "${TEST_PHASE}" in
    before-lock|pessimistic-lock|optimistic-lock|pessimistic-lock-load-limit)
        ;;
    *)
        echo "지원하지 않는 TEST_PHASE입니다."
        echo "입력값: ${TEST_PHASE}"
        echo
        echo "사용 가능한 값:"
        echo "- before-lock"
        echo "- pessimistic-lock"
        echo "- optimistic-lock"
        echo "- pessimistic-lock-load-limit"
        exit 1
        ;;
esac


case "${OVERWRITE}" in
    true|false)
        ;;
    *)
        echo "OVERWRITE는 true 또는 false여야 합니다."
        exit 1
        ;;
esac


case "${ENABLE_PROMETHEUS}" in
    true|false)
        ;;
    *)
        echo "ENABLE_PROMETHEUS는 true 또는 false여야 합니다."
        exit 1
        ;;
esac


# ============================================================
# 5. 실행 환경 검증
# ============================================================

if [[ ! -f "${K6_SCRIPT}" ]]; then
    echo "k6 스크립트를 찾을 수 없습니다."
    echo "${K6_SCRIPT}"
    exit 1
fi


if ! command -v k6 >/dev/null 2>&1; then
    echo "k6가 설치되어 있지 않습니다."
    exit 1
fi


if ! command -v jq >/dev/null 2>&1; then
    echo "결과 JSON에서 인증 토큰을 제거하려면 jq가 필요합니다."
    exit 1
fi


mkdir -p "${RESULT_DIRECTORY}"


if [[ -f "${SUMMARY_FILE}" ]] \
    && [[ "${OVERWRITE}" != "true" ]]; then

    echo "이미 결과 파일이 존재합니다."
    echo "${SUMMARY_FILE}"
    echo
    echo "덮어쓰려면 OVERWRITE=true를 사용하세요."
    exit 1
fi


# ============================================================
# 6. 테스트 정보 출력
# ============================================================

EXPECTED_FULL_COUNT="$((USER_COUNT - EXPECTED_SUCCESS_COUNT))"


echo "============================================================"
echo "경기 참가 동시성 성능 테스트"
echo "GAME_ID                : ${GAME_ID}"
echo "BASE_URL               : ${BASE_URL}"
echo "TEST_PHASE             : ${TEST_PHASE}"
echo "TEST_RUN               : ${TEST_RUN}"
echo "TEST_ID                : ${TEST_ID}"
echo "USER_COUNT             : ${USER_COUNT}"
echo "EXPECTED_SUCCESS_COUNT : ${EXPECTED_SUCCESS_COUNT}"
echo "EXPECTED_FULL_COUNT    : ${EXPECTED_FULL_COUNT}"
echo "P95_LIMIT_MS           : ${P95_LIMIT_MS}"
echo "SETUP_TIMEOUT          : ${SETUP_TIMEOUT}"
echo "PROMETHEUS             : ${ENABLE_PROMETHEUS}"
echo "RESULT_FILE            : ${SUMMARY_FILE}"
echo "============================================================"


# ============================================================
# 7. Prometheus 출력 옵션
# ============================================================

K6_OUTPUT_ARGUMENTS=()


if [[ "${ENABLE_PROMETHEUS}" == "true" ]]; then
    K6_OUTPUT_ARGUMENTS+=(
        --out experimental-prometheus-rw
    )
fi


# ============================================================
# 8. k6 실행
# ============================================================
#
# 개선 전에는 다음 임계값이 실패할 수 있다.
#
# - 성공 요청이 5건을 초과
# - 정상 정원 초과가 USER_COUNT - 5건보다 적음
# - 500 응답 또는 데드락 발생
#
# 따라서 set +e로 k6 종료 코드를 먼저 저장하고,
# 결과 파일 경로를 출력한 후 종료 코드를 반환한다.
# ============================================================

set +e


BASE_URL="${BASE_URL}" \
GAME_ID="${GAME_ID}" \
TEST_PHASE="${TEST_PHASE}" \
TEST_RUN="${TEST_RUN}" \
USER_COUNT="${USER_COUNT}" \
EXPECTED_SUCCESS_COUNT="${EXPECTED_SUCCESS_COUNT}" \
TEST_PASSWORD="${TEST_PASSWORD}" \
P95_LIMIT_MS="${P95_LIMIT_MS}" \
SETUP_TIMEOUT="${SETUP_TIMEOUT}" \
K6_PROMETHEUS_RW_SERVER_URL="${PROMETHEUS_REMOTE_WRITE_URL}" \
K6_PROMETHEUS_RW_TREND_STATS="${PROMETHEUS_TREND_STATS}" \
k6 run \
    "${K6_OUTPUT_ARGUMENTS[@]}" \
    --tag "testid=${TEST_ID}" \
    --summary-export="${SUMMARY_FILE}" \
    "${K6_SCRIPT}"


K6_EXIT_CODE=$?


set -e


# ============================================================
# 9. 결과 JSON 민감정보 제거
# ============================================================
#
# k6의 --summary-export는 setup() 반환값도 저장한다.
# setup_data에는 테스트 중 발급받은 JWT가 포함되므로
# 저장 직후 해당 필드를 제거한다.
# ============================================================

if [[ -f "${SUMMARY_FILE}" ]]; then
    SANITIZED_SUMMARY_FILE="$(
        mktemp "${SUMMARY_FILE}.sanitized.XXXXXX"
    )"

    jq 'del(.setup_data)' \
        "${SUMMARY_FILE}" \
        > "${SANITIZED_SUMMARY_FILE}"

    mv \
        "${SANITIZED_SUMMARY_FILE}" \
        "${SUMMARY_FILE}"
fi


# ============================================================
# 10. 실행 결과 출력
# ============================================================

echo
echo "============================================================"
echo "k6 실행 완료"
echo "종료 코드: ${K6_EXIT_CODE}"
echo "JSON 결과:"
echo "${SUMMARY_FILE}"
echo "============================================================"


if [[ "${K6_EXIT_CODE}" -ne 0 ]]; then
    echo
    echo "일부 임계값을 충족하지 못했습니다."
    echo
    echo "before-lock 단계라면 동시성 문제가"
    echo "재현되어 발생한 정상적인 결과일 수 있습니다."
    echo
    echo "다음 항목을 확인하세요."
    echo "- game_participation_success"
    echo "- game_participation_full"
    echo "- game_participation_system_error"
    echo "- game_participation_unexpected"
    echo "- http_req_duration p(95), p(99)"
fi


exit "${K6_EXIT_CODE}"
