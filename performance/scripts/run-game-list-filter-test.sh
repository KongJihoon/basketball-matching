#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIRECTORY="$(
    cd "$(dirname "${BASH_SOURCE[0]}")"
    pwd
)"

PROJECT_ROOT="$(
    cd "${SCRIPT_DIRECTORY}/../.."
    pwd
)"


QUERY_CASE="${QUERY_CASE:-default}"
TEST_MODE="${TEST_MODE:-smoke}"
TEST_PHASE="${TEST_PHASE:-before-index}"
TEST_RUN="${TEST_RUN:-run-01}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
FILTER_DATE="${FILTER_DATE:-}"
OVERWRITE="${OVERWRITE:-false}"
ENABLE_PROMETHEUS="${ENABLE_PROMETHEUS:-true}"
PROMETHEUS_REMOTE_WRITE_URL="${PROMETHEUS_REMOTE_WRITE_URL:-http://localhost:9090/api/v1/write}"
PROMETHEUS_TREND_STATS="${PROMETHEUS_TREND_STATS:-p(90),p(95),p(99),min,max,avg}"

TEST_ID="${TEST_PHASE}-${QUERY_CASE}-${TEST_MODE}-${TEST_RUN}"

K6_SCRIPT="${PROJECT_ROOT}/performance/k6/game-list-filter-test.js"

RESULT_DIRECTORY="${PROJECT_ROOT}/performance/results/game-list-filter/${TEST_PHASE}/k6/${TEST_MODE}"
SUMMARY_FILE="${RESULT_DIRECTORY}/${QUERY_CASE}-${TEST_RUN}-summary.json"


if [[ ! -f "${K6_SCRIPT}" ]]; then
    echo "k6 스크립트를 찾을 수 없습니다."
    echo "${K6_SCRIPT}"
    exit 1
fi


case "${QUERY_CASE}" in
    default|date|city|city-status-format|latest)
        ;;
    *)
        echo "지원하지 않는 QUERY_CASE입니다: ${QUERY_CASE}"
        exit 1
        ;;
esac


case "${TEST_MODE}" in
    smoke|warmup|baseline)
        ;;
    *)
        echo "지원하지 않는 TEST_MODE입니다: ${TEST_MODE}"
        exit 1
        ;;
esac


if [[ "${QUERY_CASE}" == "date" && -z "${FILTER_DATE}" ]]; then
    echo "date 시나리오는 FILTER_DATE가 필요합니다."
    echo "예: FILTER_DATE=2026-08-29"
    exit 1
fi


if ! command -v k6 >/dev/null 2>&1; then
    echo "k6가 설치되어 있지 않습니다."
    exit 1
fi


mkdir -p "${RESULT_DIRECTORY}"


if [[ -f "${SUMMARY_FILE}" && "${OVERWRITE}" != "true" ]]; then
    echo "이미 결과 파일이 존재합니다."
    echo "${SUMMARY_FILE}"
    echo "덮어쓰려면 OVERWRITE=true를 사용하세요."
    exit 1
fi


echo "============================================"
echo "경기 목록 필터 성능 테스트"
echo "QUERY_CASE  : ${QUERY_CASE}"
echo "TEST_MODE   : ${TEST_MODE}"
echo "TEST_PHASE  : ${TEST_PHASE}"
echo "TEST_RUN    : ${TEST_RUN}"
echo "TEST_ID     : ${TEST_ID}"
echo "FILTER_DATE : ${FILTER_DATE:-사용 안 함}"
echo "RESULT_FILE : ${SUMMARY_FILE}"
echo "PROMETHEUS  : ${ENABLE_PROMETHEUS}"
echo "============================================"


K6_OUTPUT_ARGUMENTS=()

if [[ "${ENABLE_PROMETHEUS}" == "true" ]]; then
    K6_OUTPUT_ARGUMENTS+=(
        --out experimental-prometheus-rw
    )
elif [[ "${ENABLE_PROMETHEUS}" != "false" ]]; then
    echo "ENABLE_PROMETHEUS는 true 또는 false여야 합니다."
    exit 1
fi


BASE_URL="${BASE_URL}" \
QUERY_CASE="${QUERY_CASE}" \
TEST_MODE="${TEST_MODE}" \
TEST_PHASE="${TEST_PHASE}" \
TEST_RUN="${TEST_RUN}" \
FILTER_DATE="${FILTER_DATE}" \
K6_PROMETHEUS_RW_SERVER_URL="${PROMETHEUS_REMOTE_WRITE_URL}" \
K6_PROMETHEUS_RW_TREND_STATS="${PROMETHEUS_TREND_STATS}" \
k6 run \
    "${K6_OUTPUT_ARGUMENTS[@]}" \
    --tag "testid=${TEST_ID}" \
    --summary-export="${SUMMARY_FILE}" \
    "${K6_SCRIPT}"


echo
echo "테스트가 완료되었습니다."
echo "JSON 결과:"
echo "${SUMMARY_FILE}"
