/*
 * ============================================================
 * 경기 목록 API 인덱스 적용 전 DB 기준선 측정
 * ============================================================
 *
 * 실제 API:
 * GET /api/v1/games?page=0&size=10
 *
 * 실제 Hibernate 바인딩:
 * - 기준 시각: 2026-08-19 19:58:26.033731
 * - offset: 0
 * - limit: 10
 *
 * 실행되는 SQL:
 * 1. 경기 목록 콘텐츠 조회
 * 2. 전체 결과 COUNT
 *
 * 주의:
 * - 아직 새로운 인덱스를 생성하지 않는다.
 * - 각 쿼리를 블록별로 따로 실행한다.
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


/* 전체 경기 수와 실제 조회 대상 수 */

SELECT
    COUNT(*) AS total_games,
    COALESCE(
            SUM(
                    deleted_date_time IS NULL
                        AND start_date_time >= '2026-08-19 19:58:26.033731'
            ),
            0
    ) AS target_games
FROM game_entity;


/* ============================================================
 * 1. 옵티마이저 통계 갱신
 * ============================================================
 */

ANALYZE TABLE game_entity;


/* ============================================================
 * 2. 인덱스 적용 전 현재 인덱스 확인
 * ============================================================
 *
 * 이 결과는 캡처하거나 별도로 기록한다.
 * ============================================================
 */

SHOW INDEX FROM game_entity;


/* ============================================================
 * 3. 워밍업: 실제 경기 목록 SQL
 * ============================================================
 *
 * 첫 실행 결과는 Buffer Pool과 디스크 상태의 영향을 받을 수 있으므로
 * 측정 전에 실제 쿼리를 한 번 실행한다.
 * ============================================================
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
  AND ge1_0.start_date_time >= '2026-08-19 19:58:26.033731'
ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id
    LIMIT 0, 10;


/* ============================================================
 * 4. 워밍업: 실제 COUNT SQL
 * ============================================================
 */

SELECT
    COUNT(ge1_0.game_id)
FROM game_entity AS ge1_0
WHERE ge1_0.deleted_date_time IS NULL
  AND ge1_0.start_date_time >= '2026-08-19 19:58:26.033731';


/* ============================================================
 * 5. 목록 쿼리의 예상 실행계획
 * ============================================================
 *
 * 확인할 항목:
 * - type
 * - key
 * - rows
 * - filtered
 * - Extra
 * ============================================================
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
  AND ge1_0.start_date_time >= '2026-08-19 19:58:26.033731'
ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id
    LIMIT 0, 10;


/* ============================================================
 * 6. 목록 쿼리의 실제 실행계획
 * ============================================================
 *
 * 이 쿼리를 워밍업 이후 5회 실행한다.
 * 각 실행 결과를 따로 저장한다.
 * ============================================================
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
  AND ge1_0.start_date_time >= '2026-08-19 19:58:26.033731'
ORDER BY
    ge1_0.start_date_time,
    ge1_0.game_id
    LIMIT 0, 10;


/* ============================================================
 * 7. COUNT 쿼리의 예상 실행계획
 * ============================================================
 */

EXPLAIN
SELECT
    COUNT(ge1_0.game_id)
FROM game_entity AS ge1_0
WHERE ge1_0.deleted_date_time IS NULL
  AND ge1_0.start_date_time >= '2026-08-19 19:58:26.033731';


/* ============================================================
 * 8. COUNT 쿼리의 실제 실행계획
 * ============================================================
 *
 * 이 쿼리도 워밍업 이후 5회 실행한다.
 * ============================================================
 */

EXPLAIN ANALYZE
SELECT
    COUNT(ge1_0.game_id)
FROM game_entity AS ge1_0
WHERE ge1_0.deleted_date_time IS NULL
  AND ge1_0.start_date_time >= '2026-08-19 19:58:26.033731';