/*
 * 경기 목록 지역·복합 필터 후보 인덱스 제거
 */

DROP INDEX idx_game_list_filter
    ON game_entity;

ANALYZE TABLE game_entity;

SHOW INDEX FROM game_entity;