/*
 * 경기 목록 지역·복합 필터 후보 2
 *
 * 후보 1 문제:
 * - deleted_date_time이 선두여서 지역 조건이 없는 조회도
 *   idx_game_list_filter를 선택함
 * - 최신순 목록 조회가 약 3.39배 느려짐
 *
 * 설계 의도:
 * - city_name이 존재하는 지역 조회에만 우선적으로 사용
 * - 지역 없는 기본·최신순 조회는 기존 인덱스 사용 유도
 */

CREATE INDEX idx_game_list_filter
    ON game_entity (
                    city_name,
                    game_status,
                    match_format,
                    deleted_date_time,
                    start_date_time,
                    game_id
        );

ANALYZE TABLE game_entity;

SHOW INDEX FROM game_entity;