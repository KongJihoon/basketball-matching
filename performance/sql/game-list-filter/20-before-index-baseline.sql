/*
 * ============================================================
 * 경기 목록 필터 성능 테스트: 현재 인덱스 기준선
 * ============================================================
 *
 * 현재 인덱스:
 * - idx_game_list_active_start
 * - deleted_date_time, start_date_time
 *
 * 고정 Hibernate 바인딩:
 * - now: 2026-08-22 16:45:17.184933
 * - offset: 0
 * - limit: 10
 *
 * 주의:
 * - 인덱스를 추가하거나 제거하지 않는다.
 * - Content Query와 COUNT Query를 따로 측정한다.
 * ============================================================
 */


USE basketball;


/* ============================================================
 * 0. 측정 환경 확인
 * ============================================================
 */

SELECT
    DATABASE() AS target_database,
    VERSION() AS mysql_version,
    NOW(6) AS measured_at;


/* 측정 대상 데이터 수 */

SELECT
    COUNT(*) AS total_games,

    COALESCE(
            SUM(
                    deleted_date_time IS NULL
                        AND start_date_time >=
                            '2026-08-22 16:45:17.184933'
            ),
            0
    ) AS default_target_count

FROM game_entity;


/* ============================================================
 * 1. 옵티마이저 통계 갱신
 * ============================================================
 */

ANALYZE TABLE game_entity;


/* ============================================================
 * 2. 현재 인덱스 확인
 * ============================================================
 */

SHOW INDEX FROM game_entity;


/* ============================================================
 * 3. A 시나리오: 기본 시작 시간순
 * ============================================================
 *
 * API:
 * GET /api/v1/games
 *     ?page=0
 *     &size=10
 *     &sortType=START_TIME_ASC
 * ============================================================
 */


/* ------------------------------------------------------------
 * 3-1. Content Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 16:45:17.184933'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 3-2. COUNT Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 16:45:17.184933';


/* ------------------------------------------------------------
 * 3-3. Content Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 16:45:17.184933'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 3-4. COUNT Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 16:45:17.184933';


/* ------------------------------------------------------------
 * 3-5. Content Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * 지금은 아직 실행하지 않는다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 16:45:17.184933'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 3-6. COUNT Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * 지금은 아직 실행하지 않는다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 16:45:17.184933';


/* ============================================================
 * 4. B 시나리오: 날짜 필터
 * ============================================================
 *
 * API:
 * GET /api/v1/games
 *     ?page=0
 *     &size=10
 *     &date=2026-08-29
 *     &sortType=START_TIME_ASC
 *
 * Hibernate 바인딩:
 * - startOfDay: 2026-08-29 00:00:00
 * - nextDay:   2026-08-30 00:00:00
 * - offset:    0
 * - limit:     10
 * ============================================================
 */


/* ------------------------------------------------------------
 * 4-1. Content Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-29 00:00:00'

  AND ge1_0.start_date_time <
      '2026-08-30 00:00:00'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 4-2. COUNT Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-29 00:00:00'

  AND ge1_0.start_date_time <
      '2026-08-30 00:00:00';


/* ------------------------------------------------------------
 * 4-3. Content Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-29 00:00:00'

  AND ge1_0.start_date_time <
      '2026-08-30 00:00:00'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 4-4. COUNT Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-29 00:00:00'

  AND ge1_0.start_date_time <
      '2026-08-30 00:00:00';


/* ------------------------------------------------------------
 * 4-5. Content Query EXPLAIN ANALYZE
 *
 * 다음 단계에서 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-29 00:00:00'

  AND ge1_0.start_date_time <
      '2026-08-30 00:00:00'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 4-6. COUNT Query EXPLAIN ANALYZE
 *
 * 다음 단계에서 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-29 00:00:00'

  AND ge1_0.start_date_time <
      '2026-08-30 00:00:00';


/* ============================================================
 * 5. C 시나리오: 서울 지역 + 시작 시간순
 * ============================================================
 *
 * API:
 * GET /api/v1/games
 *     ?page=0
 *     &size=10
 *     &cityName=SEOUL
 *     &sortType=START_TIME_ASC
 *
 * Hibernate 바인딩:
 * - now:      2026-08-22 17:23:46.884726
 * - cityName: SEOUL
 * - offset:   0
 * - limit:    10
 *
 * API 조회 결과:
 * - totalElements: 132539
 * ============================================================
 */


/* ------------------------------------------------------------
 * 5-1. Content Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:23:46.884726'

  AND ge1_0.city_name = 'SEOUL'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 5-2. COUNT Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:23:46.884726'

  AND ge1_0.city_name = 'SEOUL';


/* ------------------------------------------------------------
 * 5-3. Content Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:23:46.884726'

  AND ge1_0.city_name = 'SEOUL'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 5-4. COUNT Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:23:46.884726'

  AND ge1_0.city_name = 'SEOUL';


/* ------------------------------------------------------------
 * 5-5. Content Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:23:46.884726'

  AND ge1_0.city_name = 'SEOUL'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 5-6. COUNT Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:23:46.884726'

  AND ge1_0.city_name = 'SEOUL';


/* ============================================================
 * 6. D 시나리오: 서울 + 모집 중 + 3대3 + 시작 시간순
 * ============================================================
 *
 * API:
 * GET /api/v1/games
 *     ?page=0
 *     &size=10
 *     &cityName=SEOUL
 *     &gameStatus=RECRUITING
 *     &matchFormat=THREE_ON_THREE
 *     &sortType=START_TIME_ASC
 *
 * Hibernate 바인딩:
 * - now:         2026-08-22 17:33:45.007681
 * - cityName:    SEOUL
 * - matchFormat: THREE_ON_THREE
 * - gameStatus:  RECRUITING
 * - offset:      0
 * - limit:       10
 *
 * API 조회 결과:
 * - totalElements: 48495
 * ============================================================
 */


/* ------------------------------------------------------------
 * 6-1. Content Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:33:45.007681'

  AND ge1_0.city_name = 'SEOUL'

  AND ge1_0.match_format = 'THREE_ON_THREE'

  AND ge1_0.game_status = 'RECRUITING'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 6-2. COUNT Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:33:45.007681'

  AND ge1_0.city_name = 'SEOUL'

  AND ge1_0.match_format = 'THREE_ON_THREE'

  AND ge1_0.game_status = 'RECRUITING';


/* ------------------------------------------------------------
 * 6-3. Content Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:33:45.007681'

  AND ge1_0.city_name = 'SEOUL'

  AND ge1_0.match_format = 'THREE_ON_THREE'

  AND ge1_0.game_status = 'RECRUITING'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 6-4. COUNT Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:33:45.007681'

  AND ge1_0.city_name = 'SEOUL'

  AND ge1_0.match_format = 'THREE_ON_THREE'

  AND ge1_0.game_status = 'RECRUITING';


/* ------------------------------------------------------------
 * 6-5. Content Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:33:45.007681'

  AND ge1_0.city_name = 'SEOUL'

  AND ge1_0.match_format = 'THREE_ON_THREE'

  AND ge1_0.game_status = 'RECRUITING'

ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 6-6. COUNT Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:33:45.007681'

  AND ge1_0.city_name = 'SEOUL'

  AND ge1_0.match_format = 'THREE_ON_THREE'

  AND ge1_0.game_status = 'RECRUITING';


/* ============================================================
 * 7. E 시나리오: 최신순
 * ============================================================
 *
 * API:
 * GET /api/v1/games
 *     ?page=0
 *     &size=10
 *     &sortType=LATEST
 *
 * Hibernate 바인딩:
 * - now:    2026-08-22 17:42:12.987040
 * - offset: 0
 * - limit:  10
 *
 * API 조회 결과:
 * - totalElements: 331348
 * ============================================================
 */


/* ------------------------------------------------------------
 * 7-1. Content Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:42:12.987040'

ORDER BY
    ge1_0.created_at DESC,
    ge1_0.game_id DESC

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 7-2. COUNT Query 워밍업
 * ------------------------------------------------------------
 */

SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:42:12.987040';


/* ------------------------------------------------------------
 * 7-3. Content Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:42:12.987040'

ORDER BY
    ge1_0.created_at DESC,
    ge1_0.game_id DESC

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 7-4. COUNT Query EXPLAIN
 * ------------------------------------------------------------
 */

EXPLAIN
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:42:12.987040';


/* ------------------------------------------------------------
 * 7-5. Content Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    ge1_0.game_id,
    ge1_0.address,
    ge1_0.city_name,
    ge1_0.content,
    ge1_0.created_at,
    ge1_0.deleted_date_time,
    ge1_0.end_date_time,
    ge1_0.field_status,
    ge1_0.game_status,
    ge1_0.head_count,
    ge1_0.latitude,
    ge1_0.longitude,
    ge1_0.match_format,
    ge1_0.match_gender_type,
    ge1_0.participant_count,
    ge1_0.place_name,
    ge1_0.start_date_time,
    ge1_0.title,
    ge1_0.updated_at,
    ge1_0.user_entity_user_id

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:42:12.987040'

ORDER BY
    ge1_0.created_at DESC,
    ge1_0.game_id DESC

    LIMIT 0, 10;


/* ------------------------------------------------------------
 * 7-6. COUNT Query EXPLAIN ANALYZE
 *
 * 워밍업 이후 총 5회 실행한다.
 * ------------------------------------------------------------
 */

EXPLAIN ANALYZE
SELECT
    COUNT(ge1_0.game_id)

FROM game_entity AS ge1_0

WHERE ge1_0.deleted_date_time IS NULL

  AND ge1_0.start_date_time >=
      '2026-08-22 17:42:12.987040';