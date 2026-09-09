/* ============================================================
 * 수신자 수별 측정 후 알림 저장 결과 검증 (읽기 전용)
 * ============================================================
 * 실행 로그/JSON의 title_prefix를 아래에 입력한다.
 * expected_requests는 JSON의 attempted_requests(이전 JSON은 successful_requests)로 설정한다.
 * HTTP 성공 수와 실제 시도 수가 일치하는지 먼저 확인한다.
 * 단계별 검증에는 JSON stages의 title_prefix와 해당 단계 요청 수를 사용한다.
 * 비동기 적용 후에는 완료될 때까지 조회하되 완료까지 걸린 시간도 기록한다.
 * HTTP 오류가 있었다면 건수를 임의로 낮춰 PASS 처리하지 말고 원인을 조사한다.
 * 타임아웃이 발생해도 서버에서 커밋됐을 수 있으므로 DB 결과와 함께 조사한다.
 */
USE basketball;
SET @perf_update_title_prefix = '실행결과의_TITLE_PREFIX로_교체';
SET @perf_update_expected_requests = 30;
SET @perf_update_expected_receivers = 1;

SELECT COUNT(*) AS actual_notifications,
       @perf_update_expected_requests * @perf_update_expected_receivers AS expected_notifications,
       COUNT(DISTINCT content) AS actual_update_events,
       COUNT(DISTINCT receiver_id) AS distinct_receivers,
       MIN(created_at) AS first_created_at,
       MAX(created_at) AS last_created_at,
       CASE WHEN COUNT(*) = @perf_update_expected_requests * @perf_update_expected_receivers
                 AND COUNT(DISTINCT content) = @perf_update_expected_requests
                 AND COUNT(DISTINCT receiver_id) = @perf_update_expected_receivers
            THEN 'PASS' ELSE 'FAIL' END AS count_validation
FROM notification_entity
WHERE notification_type = 'UPDATE_GAME'
  AND LOCATE(CONCAT(CHAR(39), @perf_update_title_prefix), content) = 1;

/* 정상 결과: 0행. 각 수정 이벤트의 누락/중복 수신자를 검사한다.
 * 이벤트 자체가 통째로 누락된 경우는 위 actual_update_events로 확인한다.
 */
SELECT content, COUNT(*) AS notification_count,
       COUNT(DISTINCT receiver_id) AS distinct_receivers
FROM notification_entity
WHERE notification_type = 'UPDATE_GAME'
  AND LOCATE(CONCAT(CHAR(39), @perf_update_title_prefix), content) = 1
GROUP BY content
HAVING COUNT(*) <> @perf_update_expected_receivers
    OR COUNT(DISTINCT receiver_id) <> @perf_update_expected_receivers;

/* 정상 결과: 0행. 생성자 또는 전용 참가자가 아닌 사용자의 알림 검사 */
SELECT notification.notification_id, users.email, notification.content
FROM notification_entity AS notification
INNER JOIN user_entity AS users ON users.user_id = notification.receiver_id
WHERE notification.notification_type = 'UPDATE_GAME'
  AND LOCATE(CONCAT(CHAR(39), @perf_update_title_prefix), notification.content) = 1
  AND users.email NOT REGEXP '^perf-update-user-[0-9]{3}@example[.]test$';
