/*
 * 최신순 조회 후보 인덱스
 *
 * ORDER BY:
 * created_at DESC, game_id DESC
 *
 * start_date_time은 범위 조건이므로 정렬 컬럼 앞에 넣지 않는다.
 */

USE basketball;

CREATE INDEX idx_game_list_latest
    ON game_entity (
                    deleted_date_time,
                    created_at DESC,
                    game_id DESC
        );

ANALYZE TABLE game_entity;

SHOW INDEX FROM game_entity;