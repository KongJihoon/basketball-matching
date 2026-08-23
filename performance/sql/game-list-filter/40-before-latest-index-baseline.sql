/*
 * 최신순 전용 인덱스 적용 전 기준선
 *
 * 고정 조건:
 * - now: 2026-08-22 17:42:12.987040
 * - offset: 0
 * - limit: 10
 */

USE basketball;

ANALYZE TABLE game_entity;


/* 1. Content Query 워밍업 */

SELECT
    ge.game_id,
    ge.address,
    ge.city_name,
    ge.content,
    ge.created_at,
    ge.deleted_date_time,
    ge.end_date_time,
    ge.field_status,
    ge.game_status,
    ge.head_count,
    ge.latitude,
    ge.longitude,
    ge.match_format,
    ge.match_gender_type,
    ge.participant_count,
    ge.place_name,
    ge.start_date_time,
    ge.title,
    ge.updated_at,
    ge.user_entity_user_id
FROM game_entity AS ge
WHERE ge.deleted_date_time IS NULL
  AND ge.start_date_time >= '2026-08-22 17:42:12.987040'
ORDER BY
    ge.created_at DESC,
    ge.game_id DESC
    LIMIT 0, 10;


/* 2. COUNT Query 워밍업 */

SELECT COUNT(ge.game_id)
FROM game_entity AS ge
WHERE ge.deleted_date_time IS NULL
  AND ge.start_date_time >= '2026-08-22 17:42:12.987040';


/* 3. Content Query EXPLAIN */

EXPLAIN
SELECT
    ge.game_id,
    ge.address,
    ge.city_name,
    ge.content,
    ge.created_at,
    ge.deleted_date_time,
    ge.end_date_time,
    ge.field_status,
    ge.game_status,
    ge.head_count,
    ge.latitude,
    ge.longitude,
    ge.match_format,
    ge.match_gender_type,
    ge.participant_count,
    ge.place_name,
    ge.start_date_time,
    ge.title,
    ge.updated_at,
    ge.user_entity_user_id
FROM game_entity AS ge
WHERE ge.deleted_date_time IS NULL
  AND ge.start_date_time >= '2026-08-22 17:42:12.987040'
ORDER BY
    ge.created_at DESC,
    ge.game_id DESC
    LIMIT 0, 10;


/* 4. COUNT Query EXPLAIN */

EXPLAIN
SELECT COUNT(ge.game_id)
FROM game_entity AS ge
WHERE ge.deleted_date_time IS NULL
  AND ge.start_date_time >= '2026-08-22 17:42:12.987040';


/* 5. Content Query EXPLAIN ANALYZE
 * 동일 쿼리를 총 5회 실행한다.
 */

EXPLAIN ANALYZE
SELECT
    ge.game_id,
    ge.address,
    ge.city_name,
    ge.content,
    ge.created_at,
    ge.deleted_date_time,
    ge.end_date_time,
    ge.field_status,
    ge.game_status,
    ge.head_count,
    ge.latitude,
    ge.longitude,
    ge.match_format,
    ge.match_gender_type,
    ge.participant_count,
    ge.place_name,
    ge.start_date_time,
    ge.title,
    ge.updated_at,
    ge.user_entity_user_id
FROM game_entity AS ge
WHERE ge.deleted_date_time IS NULL
  AND ge.start_date_time >= '2026-08-22 17:42:12.987040'
ORDER BY
    ge.created_at DESC,
    ge.game_id DESC
    LIMIT 0, 10;


/* 6. COUNT Query EXPLAIN ANALYZE
 * 동일 쿼리를 총 5회 실행한다.
 */

EXPLAIN ANALYZE
SELECT COUNT(ge.game_id)
FROM game_entity AS ge
WHERE ge.deleted_date_time IS NULL
  AND ge.start_date_time >= '2026-08-22 17:42:12.987040';