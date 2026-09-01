#!/usr/bin/env bash

set -euo pipefail


# ============================================================
# 1. 프로젝트 경로
# ============================================================

SCRIPT_DIRECTORY="$({
    cd "$(dirname "${BASH_SOURCE[0]}")"
    pwd
})"

PROJECT_ROOT="$({
    cd "${SCRIPT_DIRECTORY}/../.."
    pwd
})"


# ============================================================
# 2. 테스트 설정
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_MODE="${TEST_MODE:-smoke}"
TEST_PHASE="${TEST_PHASE:-before}"
TEST_RUN="${TEST_RUN:-run-01}"
TEST_EMAIL="${TEST_EMAIL:-perf-my-game-user@example.test}"
TEST_PASSWORD="${TEST_PASSWORD:-Perf@1234}"
PAGE_SIZE="${PAGE_SIZE:-100}"
P95_LIMIT_MS="${P95_LIMIT_MS:-5000}"
OVERWRITE="${OVERWRITE:-false}"
ALLOW_NON_LOCAL="${ALLOW_NON_LOCAL:-false}"
ENABLE_PROMETHEUS="${ENABLE_PROMETHEUS:-true}"
PROMETHEUS_REMOTE_WRITE_URL="${PROMETHEUS_REMOTE_WRITE_URL:-http://localhost:9090/api/v1/write}"
PROMETHEUS_TREND_STATS="${PROMETHEUS_TREND_STATS:-p(90),p(95),p(99),min,max,avg}"


# ============================================================
# 3. 파일 경로
# ============================================================

TEST_ID="my-game-list-${TEST_PHASE}-${TEST_MODE}-${TEST_RUN}"

K6_SCRIPT="${PROJECT_ROOT}/performance/k6/my-game-list-n-plus-one-test.js"

RESULT_DIRECTORY="${PROJECT_ROOT}/performance/results/my-game-list-n-plus-one/${TEST_PHASE}/k6"

case "${TEST_MODE}" in
    smoke)
        SUMMARY_FILE="${RESULT_DIRECTORY}/smoke-summary.json"
        ;;
    baseline)
        SUMMARY_FILE="${RESULT_DIRECTORY}/${TEST_RUN}-summary.json"
        ;;
    stress)
        SUMMARY_FILE="${RESULT_DIRECTORY}/stress-${TEST_RUN}-summary.json"
        ;;
esac


# ============================================================
# 4. 입력값 검증
# ============================================================

case "${TEST_MODE}" in
    smoke|baseline|stress)
        ;;
    *)
        echo "지원하지 않는 TEST_MODE입니다: ${TEST_MODE}"
        echo "사용 가능: smoke, baseline, stress"
        exit 1
        ;;
esac

case "${TEST_PHASE}" in
    before|after)
        ;;
    *)
        echo "지원하지 않는 TEST_PHASE입니다: ${TEST_PHASE}"
        echo "사용 가능: before, after"
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

case "${ALLOW_NON_LOCAL}" in
    true|false)
        ;;
    *)
        echo "ALLOW_NON_LOCAL은 true 또는 false여야 합니다."
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

if ! [[ "${PAGE_SIZE}" =~ ^[0-9]+$ ]] \
    || [[ "${PAGE_SIZE}" -lt 1 ]] \
    || [[ "${PAGE_SIZE}" -gt 100 ]]; then

    echo "PAGE_SIZE는 1~100 정수여야 합니다."
    echo "입력값: ${PAGE_SIZE}"
    exit 1
fi

if [[ "${ALLOW_NON_LOCAL}" != "true" ]]; then
    case "${BASE_URL}" in
        http://localhost:*|https://localhost:*|http://127.0.0.1:*|https://127.0.0.1:*)
            ;;
        *)
            echo "로컬 주소가 아닌 대상에는 실행할 수 없습니다."
            echo "BASE_URL: ${BASE_URL}"
            exit 1
            ;;
    esac
fi


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
    echo "결과 JSON을 정리하려면 jq가 필요합니다."
    exit 1
fi

if ! curl -fsS "${BASE_URL}/actuator/health" \
    | jq -e '.status == "UP"' >/dev/null; then

    echo "애플리케이션 상태가 UP이 아닙니다."
    exit 1
fi

mkdir -p "${RESULT_DIRECTORY}"

if [[ -f "${SUMMARY_FILE}" ]] \
    && [[ "${OVERWRITE}" != "true" ]]; then

    echo "이미 결과 파일이 존재합니다."
    echo "${SUMMARY_FILE}"
    echo "덮어쓰려면 OVERWRITE=true를 사용하세요."
    exit 1
fi


# ============================================================
# 6. 실행 정보
# ============================================================

echo "============================================================"
echo "내 경기 목록 N+1 성능 테스트"
echo "BASE_URL    : ${BASE_URL}"
echo "TEST_MODE   : ${TEST_MODE}"
echo "TEST_PHASE  : ${TEST_PHASE}"
echo "TEST_RUN    : ${TEST_RUN}"
echo "TEST_ID     : ${TEST_ID}"
echo "TEST_EMAIL  : ${TEST_EMAIL}"
echo "PAGE_SIZE   : ${PAGE_SIZE}"
echo "P95_LIMIT   : ${P95_LIMIT_MS}ms"
echo "PROMETHEUS  : ${ENABLE_PROMETHEUS}"
echo "RESULT_FILE : ${SUMMARY_FILE}"
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

set +e

BASE_URL="${BASE_URL}" \
TEST_MODE="${TEST_MODE}" \
TEST_PHASE="${TEST_PHASE}" \
TEST_RUN="${TEST_RUN}" \
TEST_EMAIL="${TEST_EMAIL}" \
TEST_PASSWORD="${TEST_PASSWORD}" \
PAGE_SIZE="${PAGE_SIZE}" \
P95_LIMIT_MS="${P95_LIMIT_MS}" \
ALLOW_NON_LOCAL="${ALLOW_NON_LOCAL}" \
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

if [[ -f "${SUMMARY_FILE}" ]]; then
    SANITIZED_SUMMARY_FILE="$({
        mktemp "${SUMMARY_FILE}.sanitized.XXXXXX"
    })"

    jq 'del(.setup_data)' \
        "${SUMMARY_FILE}" \
        > "${SANITIZED_SUMMARY_FILE}"

    mv "${SANITIZED_SUMMARY_FILE}" \
        "${SUMMARY_FILE}"
fi


# ============================================================
# 10. 결과 출력
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
    echo "개선 전 부하 테스트에서는 병목 재현 결과일 수 있습니다."
    echo
    echo "확인 항목:"
    echo "- http_req_duration p(95), p(99)"
    echo "- http_req_failed"
    echo "- dropped_iterations"
    echo "- my_game_list_expected_page_size"
    echo "- my_game_list_system_error"
fi

exit "${K6_EXIT_CODE}"
