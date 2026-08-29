/*
 * ============================================================
 * 경기 참가 동시성 테스트 경기 준비
 * ============================================================
 *
 * 테스트 조건:
 * - 경기 정원: 6명
 * - 생성자: 이미 ACCEPT 상태
 * - 현재 참가 인원: 1명
 * - 남은 자리: 5명
 * - 동시 참가 요청: 100명
 *
 * 기대 결과:
 * - 성공: 5건
 * - 정원 초과 거절: 95건
 * - 최종 participant_count: 6
 * - 최종 ACCEPT 참가자: 6
 *
 * 주의:
 * - 로컬 성능 테스트 DB 전용
 * - 실행할 때마다 이전 테스트 경기와 참가 기록을 초기화한다.
 * ============================================================
 */


/* ============================================================
 * 0. 대상 데이터베이스 확인
 * ============================================================
 */

USE basketball;
SET NAMES utf8mb4
    COLLATE utf8mb4_unicode_ci;

SELECT
    DATABASE() AS target_database,
    VERSION() AS mysql_version,
    NOW(6) AS started_at;


/* ============================================================
 * 1. 테스트 사용자 존재 여부 확인
 * ============================================================
 */

SET @creator_email =
    'perf-concurrency-creator@example.test';

SET @creator_user_id = (
    SELECT user_id
    FROM user_entity
    WHERE email = @creator_email
      AND deleted_date_time IS NULL
    LIMIT 1
);


SELECT
    @creator_user_id AS creator_user_id,
    (
        SELECT COUNT(*)
        FROM user_entity
        WHERE email LIKE
              'perf-concurrency-user-%@example.test'
          AND deleted_date_time IS NULL
    ) AS participant_user_count;


/*
 * 정상 결과:
 *
 * creator_user_id       = NULL이 아닌 값
 * participant_user_count = 100
 */


/* ============================================================
 * 2. 이전 동시성 테스트 데이터 제거
 * ============================================================
 */

SET @test_game_title =
    'PERF_CONCURRENCY_GAME';

SET @previous_game_id = (
    SELECT game_id
    FROM game_entity
    WHERE title = @test_game_title
    LIMIT 1
);


START TRANSACTION;


/*
 * 신고 데이터가 존재할 경우 경기 삭제보다 먼저 제거한다.
 */

DELETE FROM report_entity
WHERE game_entity_game_id = @previous_game_id;


/*
 * 이전 참가 기록 제거
 */

DELETE FROM participant_game_entity
WHERE game_entity_game_id = @previous_game_id;


/*
 * 이전 테스트 경기 제거
 */

DELETE FROM game_entity
WHERE game_id = @previous_game_id;


COMMIT;


/* 이전 경기 제거 확인 */

SELECT COUNT(*) AS previous_game_count
FROM game_entity
WHERE title = @test_game_title;


/*
 * 정상 결과:
 *
 * previous_game_count = 0
 */


/* ============================================================
 * 3. 테스트 경기 시간 설정
 * ============================================================
 *
 * 실행일 기준 30일 후 19시부터 21시까지 경기로 생성한다.
 * 따라서 경기 시작 시간 검증에 걸리지 않는다.
 * ============================================================
 */

SET @game_start_date_time =
    TIMESTAMP(
        DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY),
        '19:00:00'
    );

SET @game_end_date_time =
    DATE_ADD(
        @game_start_date_time,
        INTERVAL 2 HOUR
    );


SELECT
    @game_start_date_time AS game_start_date_time,
    @game_end_date_time AS game_end_date_time;


/* ============================================================
 * 4. 정원 6명의 테스트 경기 생성
 * ============================================================
 */

START TRANSACTION;


INSERT INTO game_entity (
    created_at,
    updated_at,
    address,
    city_name,
    content,
    deleted_date_time,
    end_date_time,
    field_status,
    game_status,
    head_count,
    latitude,
    longitude,
    match_format,
    match_gender_type,
    participant_count,
    place_name,
    start_date_time,
    title,
    user_entity_user_id
)
VALUES (
           NOW(6),
           NOW(6),
           '서울특별시 송파구 동시성테스트로 1',
           'seoul',
           '경기 참가 동시성 테스트용 경기입니다.',
           NULL,
           @game_end_date_time,
           'indoor',
           'recruiting',
           6,
           37.515,
           127.073,
           'three_on_three',
           'mixed',
           1,
           'PERF_CONCURRENCY_COURT',
           @game_start_date_time,
           @test_game_title,
           @creator_user_id
       );


SET @test_game_id = LAST_INSERT_ID();


/* ============================================================
 * 5. 경기 생성자의 ACCEPT 참가 기록 생성
 * ============================================================
 */

INSERT INTO participant_game_entity (
    created_at,
    updated_at,
    accept_date_time,
    canceled_date_time,
    deleted_date_time,
    kickout_date_time,
    participant_game_status,
    game_entity_game_id,
    user_entity_user_id
)
VALUES (
           NOW(6),
           NOW(6),
           NOW(6),
           NULL,
           NULL,
           NULL,
           'accept',
           @test_game_id,
           @creator_user_id
       );


COMMIT;


/* ============================================================
 * 6. 생성 결과 확인
 * ============================================================
 */

SELECT
    game_id,
    title,
    head_count,
    participant_count,
    game_status,
    start_date_time,
    end_date_time,
    user_entity_user_id AS creator_user_id
FROM game_entity
WHERE game_id = @test_game_id;


SELECT
    COUNT(*) AS accepted_participant_count
FROM participant_game_entity
WHERE game_entity_game_id = @test_game_id
  AND participant_game_status = 'accept';


SELECT
    @test_game_id AS k6_game_id;


/*
 * 정상 결과:
 *
 * head_count                = 6
 * participant_count         = 1
 * game_status               = recruiting
 * accepted_participant_count = 1
 *
 * 마지막에 출력된 k6_game_id는 다음 k6 실행에 사용한다.
 */


/* ============================================================
 * 7. 준비 완료 시각
 * ============================================================
 */

SELECT NOW(6) AS completed_at;