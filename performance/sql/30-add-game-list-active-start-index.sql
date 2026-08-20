/*
 * ============================================================
 * 기본 경기 목록 조회 복합 인덱스 적용
 * ============================================================
 *
 * 대상 조건:
 * - deleted_date_time IS NULL
 * - start_date_time >= ?
 *
 * 대상 정렬:
 * - start_date_time ASC
 * - game_id ASC
 *
 * InnoDB 보조 인덱스에는 PK(game_id)가 자동으로 포함되므로
 * game_id는 명시적으로 추가하지 않는다.
 *
 * 주의:
 * - CREATE INDEX는 한 번만 실행한다.
 * - 검증이 완료된 동일한 인덱스 정의를 GameEntity의 @Table에도 반영한다.
 * ============================================================
 */

USE basketball;


/* ============================================================
 * 1. 대상 환경 확인
 * ============================================================
 */

SELECT
    DATABASE() AS target_database,
    VERSION() AS mysql_version,
    NOW(6) AS index_created_at;


/* ============================================================
 * 2. 동일한 이름의 인덱스가 없는지 재확인
 * ============================================================
 *
 * 결과가 0이어야 한다.
 * ============================================================
 */

SELECT
    COUNT(*) AS existing_index_column_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'game_entity'
  AND index_name = 'idx_game_list_active_start';


/* ============================================================
 * 3. 기본 경기 목록 조회용 복합 인덱스 생성
 * ============================================================
 *
 * 앞쪽: 삭제되지 않은 경기 동등 조건
 * 뒤쪽: 예정 경기 범위 조건 및 시작 시각 정렬
 * ============================================================
 */

CREATE INDEX idx_game_list_active_start
    ON game_entity (
                    deleted_date_time,
                    start_date_time
        );


/* ============================================================
 * 4. 옵티마이저 통계 갱신
 * ============================================================
 */

ANALYZE TABLE game_entity;


/* ============================================================
 * 5. 생성 결과 확인
 * ============================================================
 */

SELECT
    index_name,
    non_unique,
    seq_in_index,
    column_name,
    collation,
    cardinality,
    index_type,
    is_visible
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'game_entity'
  AND index_name = 'idx_game_list_active_start'
ORDER BY seq_in_index;