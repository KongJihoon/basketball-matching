/*
 * 최신순 조회 후보 2
 *
 * 목적:
 * - created_at, game_id 순서로 최신순 정렬 지원
 * - start_date_time을 인덱스에 포함해 COUNT의 테이블 접근 방지
 *
 * start_date_time은 정렬 컬럼 뒤에 있으므로
 * 범위 탐색보다는 커버링 용도로 사용된다.
 */

USE basketball;

CREATE INDEX idx_game_list_latest
    ON game_entity (
                    deleted_date_time,
                    created_at DESC,
                    game_id DESC,
                    start_date_time
        );

ANALYZE TABLE game_entity;

SHOW INDEX FROM game_entity;