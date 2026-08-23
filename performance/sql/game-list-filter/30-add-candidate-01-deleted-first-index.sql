/*
 * 경기 목록 지역·복합 필터 후보 인덱스
 *
 * 대상:
 * C. 서울 + 시작 시간순
 * D. 서울 + 모집 중 + 3대3 + 시작 시간순
 *
 * 아직 최종 인덱스가 아닌 검증 후보이다.
 */

CREATE INDEX idx_game_list_filter
    ON game_entity (
                    deleted_date_time,
                    city_name,
                    game_status,
                    match_format,
                    start_date_time,
                    game_id
        );

ANALYZE TABLE game_entity;

SHOW INDEX FROM game_entity;