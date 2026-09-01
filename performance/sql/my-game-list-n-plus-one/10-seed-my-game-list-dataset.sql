/*
 * ============================================================
 * 내 경기 목록 N+1 재현 데이터 생성
 * ============================================================
 *
 * 생성 데이터:
 * - 전용 경기 생성자 1명
 * - 전용 조회 사용자 1명
 * - 예정 경기 100건
 * - 지난 경기 100건
 * - 각 경기의 생성자와 조회 사용자를 ACCEPT 참가로 연결
 *
 * 조회 사용자:
 * - email: perf-my-game-user@example.test
 * - password: Perf@1234
 *
 * 주의:
 * - 로컬 성능 테스트 DB에서만 실행한다.
 * - PERF_MY_GAME_ 접두어를 가진 경기만 재생성한다.
 * - 기존 일반 사용자와 경기 데이터는 삭제하지 않는다.
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
 * 1. 1~100 순번 생성
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_my_game_sequence;

CREATE TEMPORARY TABLE perf_my_game_sequence (
    sequence_no INT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO perf_my_game_sequence (sequence_no)
WITH digits AS (
    SELECT 0 AS number
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
    UNION ALL SELECT 7
    UNION ALL SELECT 8
    UNION ALL SELECT 9
)
SELECT
    ones.number + tens.number * 10 + 1
FROM digits AS ones
         CROSS JOIN digits AS tens
WHERE ones.number + tens.number * 10 < 100;


/* ============================================================
 * 2. 전용 테스트 사용자 생성
 * ============================================================
 */

START TRANSACTION;

INSERT INTO user_entity (
    created_at,
    updated_at,
    address,
    birth,
    deleted_date_time,
    email,
    email_auth,
    gender_type,
    login_provider,
    name,
    nickname,
    password,
    phone,
    position,
    user_type
)
VALUES
    (
        NOW(6),
        NOW(6),
        '서울특별시 N+1테스트로 1',
        '1997-01-01',
        NULL,
        'perf-my-game-creator@example.test',
        b'1',
        'male',
        'local',
        'N+1테스트생성자',
        'perf_my_game_creator',
        '$2y$10$X41wLjiMCumq51wxDclm7Ok.B/ItPtLEYkAqNIpT12qftI2x1PLaW',
        '01078880001',
        'guard',
        'user'
    ),
    (
        NOW(6),
        NOW(6),
        '서울특별시 N+1테스트로 2',
        '1997-02-01',
        NULL,
        'perf-my-game-user@example.test',
        b'1',
        'female',
        'local',
        'N+1테스트조회자',
        'perf_my_game_user',
        '$2y$10$X41wLjiMCumq51wxDclm7Ok.B/ItPtLEYkAqNIpT12qftI2x1PLaW',
        '01078880002',
        'forward',
        'user'
    )
ON DUPLICATE KEY UPDATE
    updated_at = NOW(6),
    deleted_date_time = NULL,
    email_auth = b'1',
    password = VALUES(password),
    login_provider = 'local',
    user_type = 'user';

COMMIT;

SET @perf_creator_user_id = (
    SELECT user_id
    FROM user_entity
    WHERE email = 'perf-my-game-creator@example.test'
      AND deleted_date_time IS NULL
    LIMIT 1
);

SET @perf_viewer_user_id = (
    SELECT user_id
    FROM user_entity
    WHERE email = 'perf-my-game-user@example.test'
      AND deleted_date_time IS NULL
    LIMIT 1
);

SELECT
    @perf_creator_user_id AS creator_user_id,
    @perf_viewer_user_id AS viewer_user_id;


/* ============================================================
 * 3. 이전 N+1 테스트 경기 정리
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_my_game_previous_ids;

CREATE TEMPORARY TABLE perf_my_game_previous_ids (
    game_id BIGINT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO perf_my_game_previous_ids (game_id)
SELECT game_id
FROM game_entity
WHERE title LIKE 'PERF\\_MY\\_GAME\\_%' ESCAPE '\\';

START TRANSACTION;

DELETE report
FROM report_entity AS report
         INNER JOIN perf_my_game_previous_ids AS previous_game
                    ON previous_game.game_id = report.game_entity_game_id;

DELETE participation
FROM participant_game_entity AS participation
         INNER JOIN perf_my_game_previous_ids AS previous_game
                    ON previous_game.game_id = participation.game_entity_game_id;

DELETE game
FROM game_entity AS game
         INNER JOIN perf_my_game_previous_ids AS previous_game
                    ON previous_game.game_id = game.game_id;

COMMIT;


/* ============================================================
 * 4. 예정 경기 100건 생성
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
SELECT
    DATE_SUB(NOW(6), INTERVAL sequence_no MINUTE),
    NOW(6),
    CONCAT('서울특별시 N+1 예정경기로 ', sequence_no),
    'seoul',
    '내 예정 경기 목록 N+1 재현용 경기입니다.',
    NULL,
    DATE_ADD(
        TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL sequence_no + 30 DAY), '21:00:00'),
        INTERVAL 0 SECOND
    ),
    'indoor',
    'recruiting',
    6,
    37.515,
    127.073,
    'three_on_three',
    'mixed',
    2,
    CONCAT('PERF_MY_GAME_UPCOMING_COURT_', LPAD(sequence_no, 3, '0')),
    TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL sequence_no + 30 DAY), '19:00:00'),
    CONCAT('PERF_MY_GAME_UPCOMING_', LPAD(sequence_no, 3, '0')),
    @perf_creator_user_id
FROM perf_my_game_sequence;


/* ============================================================
 * 5. 지난 경기 100건 생성
 * ============================================================
 */

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
SELECT
    DATE_SUB(NOW(6), INTERVAL sequence_no + 200 DAY),
    NOW(6),
    CONCAT('서울특별시 N+1 지난경기로 ', sequence_no),
    'seoul',
    '내 지난 경기 목록 N+1 재현용 경기입니다.',
    NULL,
    TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL sequence_no + 30 DAY), '21:00:00'),
    'indoor',
    'closed',
    6,
    37.515,
    127.073,
    'three_on_three',
    'mixed',
    2,
    CONCAT('PERF_MY_GAME_COMPLETED_COURT_', LPAD(sequence_no, 3, '0')),
    TIMESTAMP(DATE_SUB(CURRENT_DATE, INTERVAL sequence_no + 30 DAY), '19:00:00'),
    CONCAT('PERF_MY_GAME_COMPLETED_', LPAD(sequence_no, 3, '0')),
    @perf_creator_user_id
FROM perf_my_game_sequence;


/* ============================================================
 * 6. 각 경기의 생성자 ACCEPT 참가 생성
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
SELECT
    game.created_at,
    game.created_at,
    game.created_at,
    NULL,
    NULL,
    NULL,
    'accept',
    game.game_id,
    @perf_creator_user_id
FROM game_entity AS game
WHERE game.title LIKE 'PERF\\_MY\\_GAME\\_%' ESCAPE '\\';


/* ============================================================
 * 7. 조회 사용자 ACCEPT 참가 생성
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
SELECT
    game.created_at,
    game.created_at,
    game.created_at,
    NULL,
    NULL,
    NULL,
    'accept',
    game.game_id,
    @perf_viewer_user_id
FROM game_entity AS game
WHERE game.title LIKE 'PERF\\_MY\\_GAME\\_%' ESCAPE '\\';

COMMIT;


/* ============================================================
 * 8. 생성 결과 검증
 * ============================================================
 */

SELECT
    SUM(game.title LIKE 'PERF\\_MY\\_GAME\\_UPCOMING\\_%' ESCAPE '\\')
        AS upcoming_game_count,
    SUM(game.title LIKE 'PERF\\_MY\\_GAME\\_COMPLETED\\_%' ESCAPE '\\')
        AS completed_game_count,
    COUNT(*) AS total_game_count
FROM game_entity AS game
WHERE game.title LIKE 'PERF\\_MY\\_GAME\\_%' ESCAPE '\\';

SELECT
    SUM(game.start_date_time > NOW(6)) AS viewer_upcoming_count,
    SUM(game.end_date_time <= NOW(6)) AS viewer_completed_count,
    COUNT(*) AS viewer_total_participation_count
FROM participant_game_entity AS participation
         INNER JOIN game_entity AS game
                    ON game.game_id = participation.game_entity_game_id
WHERE participation.user_entity_user_id = @perf_viewer_user_id
  AND participation.participant_game_status = 'accept'
  AND game.deleted_date_time IS NULL
  AND game.title LIKE 'PERF\\_MY\\_GAME\\_%' ESCAPE '\\';

/*
 * 정상 결과:
 *
 * upcoming_game_count              = 100
 * completed_game_count             = 100
 * total_game_count                 = 200
 * viewer_upcoming_count            = 100
 * viewer_completed_count           = 100
 * viewer_total_participation_count = 200
 */

DROP TEMPORARY TABLE IF EXISTS perf_my_game_previous_ids;
DROP TEMPORARY TABLE IF EXISTS perf_my_game_sequence;

SELECT NOW(6) AS completed_at;
