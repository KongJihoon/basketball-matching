#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_PHASE="${TEST_PHASE:-before}"
TEST_EMAIL="${TEST_EMAIL:-perf-my-game-user@example.test}"
TEST_PASSWORD="${TEST_PASSWORD:-Perf@1234}"

RESULT_DIRECTORY="${PROJECT_ROOT}/performance/results/my-game-list-n-plus-one/${TEST_PHASE}"
RESULT_FILE="${RESULT_DIRECTORY}/query-count-by-page-size.csv"

PAGE_SIZES=(1 10 20 100)
ENDPOINTS=(upcoming completed)

require_command() {
    local command_name="$1"

    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "필수 명령어를 찾을 수 없습니다: ${command_name}" >&2
        exit 1
    fi
}

read_metric_value() {
    local endpoint_path="$1"
    local metric_suffix="$2"
    local metric_name="app_query_per_request_queries_${metric_suffix}"
    local metric_value

    metric_value="$({
        curl -fsS "${BASE_URL}/actuator/prometheus" \
            | awk \
                -v metric_name="${metric_name}" \
                -v endpoint_path="${endpoint_path}" '
                    index($0, metric_name "{") == 1 \
                    && index($0, "http_method=\"GET\"") > 0 \
                    && index($0, "path=\"" endpoint_path "\"") > 0 \
                    && index($0, "query_type=\"SELECT\"") > 0 \
                    && !found {
                        value = $NF
                        found = 1
                    }
                    END {
                        if (found) {
                            print value
                        }
                    }
                '
    } || true)"

    if [[ -z "${metric_value}" ]]; then
        echo "0"
        return
    fi

    echo "${metric_value}"
}

calculate_difference() {
    local after_value="$1"
    local before_value="$2"

    awk -v after_value="${after_value}" -v before_value="${before_value}" \
        'BEGIN { printf "%.0f", after_value - before_value }'
}

require_command curl
require_command jq
require_command awk

case "${TEST_PHASE}" in
    before|after)
        ;;
    *)
        echo "지원하지 않는 TEST_PHASE입니다: ${TEST_PHASE}" >&2
        echo "사용 가능: before, after" >&2
        exit 1
        ;;
esac

echo "============================================================"
echo "내 경기 목록 N+1 ${TEST_PHASE} 쿼리 수 측정"
echo "BASE_URL    : ${BASE_URL}"
echo "TEST_PHASE  : ${TEST_PHASE}"
echo "TEST_EMAIL  : ${TEST_EMAIL}"
echo "RESULT_FILE : ${RESULT_FILE}"
echo "============================================================"

health_status="$(curl -fsS "${BASE_URL}/actuator/health" | jq -r '.status')"

if [[ "${health_status}" != "UP" ]]; then
    echo "애플리케이션 상태가 UP이 아닙니다: ${health_status}" >&2
    exit 1
fi

login_response="$(
    curl -fsS \
        -X POST "${BASE_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "$(
            jq -nc \
                --arg email "${TEST_EMAIL}" \
                --arg password "${TEST_PASSWORD}" \
                '{email: $email, password: $password}'
        )"
)"

access_token="$(echo "${login_response}" | jq -r '.data.accessToken // empty')"

if [[ -z "${access_token}" ]]; then
    echo "Access Token 발급에 실패했습니다." >&2
    exit 1
fi

mkdir -p "${RESULT_DIRECTORY}"

echo "endpoint,page_size,returned_elements,select_queries,expected_select_queries,http_status,verdict" \
    > "${RESULT_FILE}"

for endpoint in "${ENDPOINTS[@]}"; do
    endpoint_path="/api/v1/mypage/games/${endpoint}"

    for page_size in "${PAGE_SIZES[@]}"; do
        before_sum="$(read_metric_value "${endpoint_path}" "sum")"
        before_count="$(read_metric_value "${endpoint_path}" "count")"

        response_with_status="$(
            curl -sS \
                -H "Authorization: Bearer ${access_token}" \
                -w $'\n%{http_code}' \
                "${BASE_URL}${endpoint_path}?page=0&size=${page_size}"
        )"

        http_status="${response_with_status##*$'\n'}"
        response_body="${response_with_status%$'\n'*}"

        if [[ "${http_status}" != "200" ]]; then
            echo "API 호출에 실패했습니다: endpoint=${endpoint}, size=${page_size}, status=${http_status}" >&2
            echo "${response_body}" >&2
            exit 1
        fi

        returned_elements="$(echo "${response_body}" | jq -r '.data.numberOfElements')"

        after_sum="$(read_metric_value "${endpoint_path}" "sum")"
        after_count="$(read_metric_value "${endpoint_path}" "count")"

        select_queries="$(calculate_difference "${after_sum}" "${before_sum}")"
        request_metric_count="$(calculate_difference "${after_count}" "${before_count}")"

        if [[ "${TEST_PHASE}" == "before" ]]; then
            expected_select_queries="$((returned_elements + 3))"
            success_verdict="N_PLUS_ONE_MATCH"
        else
            expected_select_queries="3"
            success_verdict="N_PLUS_ONE_REMOVED"
        fi

        if [[ "${request_metric_count}" != "1" ]]; then
            verdict="METRIC_COUNT_${request_metric_count}"
        elif [[ "${select_queries}" == "${expected_select_queries}" ]]; then
            verdict="${success_verdict}"
        else
            verdict="OBSERVED_DIFFERENCE"
        fi

        echo "${endpoint},${page_size},${returned_elements},${select_queries},${expected_select_queries},${http_status},${verdict}" \
            | tee -a "${RESULT_FILE}"
    done
done

echo "============================================================"
echo "측정 완료"
echo "${RESULT_FILE}"
echo "============================================================"
