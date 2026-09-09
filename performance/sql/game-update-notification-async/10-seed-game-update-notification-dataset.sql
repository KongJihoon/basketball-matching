/*
 * ============================================================
 * 경기 수정 알림 비동기 처리 성능 테스트 데이터 생성
 * ============================================================
 *
 * 생성 데이터:
 * - 전용 생성자 1명, 참가자 99명 (여러 경기에서 재사용)
 * - 수신자 1명 / 10명 / 50명인 경기 각 1건
 * - 수신자 99명인 경기 10건
 * - 경기 13건, ACCEPT 참가 정보 1064건 (생성자 포함)
 *
 * 로그인:
 * - email: perf-update-creator@example.test
 * - password: Perf@1234
 *
 * 실행 방법:
 * - MySQL 8 로컬 성능 테스트 DB에서 동일 연결로 파일 전체 실행
 * - 애플리케이션의 테이블 생성 완료 후 실행
 * - 테스트 요청과 비동기 작업이 모두 종료된 상태에서 실행
 * - 실행 오류가 발생하면 즉시 ROLLBACK 후 원인 확인
 *
 * 재실행 정책:
 * - 전용 계정과 경기 ID는 재사용한다.
 * - 전용 경기의 제목, 일정, 정원, 참가 정보를 초기화한다.
 * - 경기 식별에는 변경 가능한 제목 대신 생성자 + 고정 장소 + 주소를 사용한다.
 * - 기존 알림 및 신고 이력은 삭제하지 않는다.
 * - 알림 검증은 테스트 직전/직후 건수 차이 또는 고유 제목으로 구분한다.
 * - SQL 직접 삽입이므로 경기 생성/참가 알림 이벤트는 발생하지 않는다.
 * ============================================================
 */

USE basketball;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database, VERSION() AS mysql_version,
       NOW(6) AS started_at, @@session.time_zone AS session_time_zone;


/* ============================================================
 * 1. 사용자 순번 및 경기 시나리오 준비
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_update_sequence;
CREATE TEMPORARY TABLE perf_update_sequence (
    sequence_no INT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO perf_update_sequence (sequence_no)
WITH digits AS (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2
    UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
)
SELECT ones.n + tens.n * 10
FROM digits AS ones CROSS JOIN digits AS tens;

DROP TEMPORARY TABLE IF EXISTS perf_update_scenarios;
CREATE TEMPORARY TABLE perf_update_scenarios (
    scenario_no INT NOT NULL PRIMARY KEY,
    scenario_name VARCHAR(40) NOT NULL,
    receiver_count INT NOT NULL,
    head_count INT NOT NULL,
    place_name VARCHAR(100) NOT NULL,
    address VARCHAR(100) NOT NULL,
    game_id BIGINT NULL
) ENGINE = InnoDB;

INSERT INTO perf_update_scenarios
    (scenario_no, scenario_name, receiver_count, head_count, place_name, address)
SELECT sequence_no,
       CASE sequence_no
           WHEN 0 THEN 'receivers-001'
           WHEN 1 THEN 'receivers-010'
           WHEN 2 THEN 'receivers-050'
           ELSE CONCAT('receivers-099-game-', LPAD(sequence_no - 2, 2, '0'))
       END,
       CASE sequence_no WHEN 0 THEN 1 WHEN 1 THEN 10 WHEN 2 THEN 50 ELSE 99 END,
       CASE sequence_no WHEN 0 THEN 6 WHEN 1 THEN 11 WHEN 2 THEN 51 ELSE 100 END,
       CONCAT('PERF_UPDATE_NOTIFICATION_COURT_', LPAD(sequence_no, 2, '0')),
       CONCAT('서울특별시 비동기테스트로 ', sequence_no + 1)
FROM perf_update_sequence
WHERE sequence_no < 13;


/* ============================================================
 * 2. 전용 사용자 100명 생성 / 재사용
 * ============================================================
 */

START TRANSACTION;

INSERT INTO user_entity (
    created_at, updated_at, address, birth, deleted_date_time,
    email, email_auth, gender_type, login_provider, name, nickname,
    password, phone, position, user_type
)
SELECT NOW(6), NOW(6), '서울특별시 비동기테스트로', '1997-01-01', NULL,
       CASE WHEN sequence_no = 0 THEN 'perf-update-creator@example.test'
            ELSE CONCAT('perf-update-user-', LPAD(sequence_no, 3, '0'), '@example.test') END,
       b'1', 'MALE', 'LOCAL',
       CONCAT('수정알림테스트', LPAD(sequence_no, 3, '0')),
       CASE WHEN sequence_no = 0 THEN 'perf_update_creator'
            ELSE CONCAT('perf_update_user_', LPAD(sequence_no, 3, '0')) END,
       '$2y$10$X41wLjiMCumq51wxDclm7Ok.B/ItPtLEYkAqNIpT12qftI2x1PLaW',
       CONCAT('0107666', LPAD(sequence_no, 4, '0')), 'GUARD', 'USER'
FROM perf_update_sequence
ON DUPLICATE KEY UPDATE
    updated_at = NOW(6), deleted_date_time = NULL, email_auth = b'1',
    password = VALUES(password), login_provider = 'LOCAL', user_type = 'USER';

SET @perf_update_creator_id = (
    SELECT user_id FROM user_entity
    WHERE email = 'perf-update-creator@example.test' AND deleted_date_time IS NULL
);


/* ============================================================
 * 3. 경기 생성 / 기존 ID 유지
 * ============================================================
 * 시작: 오늘로부터 7~19일 뒤 19시, 종료: 같은 날 21시
 * 수신자 1명 경기의 실제 참가자는 2명이지만 모집 정원은 최소 정책인 6명이다.
 */

INSERT INTO game_entity (
    created_at, updated_at, address, city_name, content, deleted_date_time,
    end_date_time, field_status, game_status, head_count, latitude, longitude,
    match_format, match_gender_type, participant_count, place_name,
    start_date_time, title, user_entity_user_id
)
SELECT NOW(6), NOW(6), scenario.address, 'SEOUL',
       '경기 수정 알림 비동기 처리 성능 테스트 전용 경기', NULL,
       TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL scenario.scenario_no + 7 DAY), '21:00:00'),
       'INDOOR', 'RECRUITING', scenario.head_count, 37.515, 127.073,
       'THREE_ON_THREE', 'MIXED', 0, scenario.place_name,
       TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL scenario.scenario_no + 7 DAY), '19:00:00'),
       CONCAT('PERF_UPDATE_NOTIFICATION_', scenario.scenario_name), @perf_update_creator_id
FROM perf_update_scenarios AS scenario
WHERE NOT EXISTS (
    SELECT 1 FROM game_entity AS existing
    WHERE existing.user_entity_user_id = @perf_update_creator_id
      AND existing.place_name = scenario.place_name
      AND existing.address = scenario.address
);

UPDATE perf_update_scenarios AS scenario
INNER JOIN game_entity AS game
    ON game.user_entity_user_id = @perf_update_creator_id
   AND game.place_name = scenario.place_name AND game.address = scenario.address
SET scenario.game_id = game.game_id;

UPDATE game_entity AS game
INNER JOIN perf_update_scenarios AS scenario ON scenario.game_id = game.game_id
SET game.title = CONCAT('PERF_UPDATE_NOTIFICATION_', scenario.scenario_name),
    game.content = '경기 수정 알림 비동기 처리 성능 테스트 전용 경기',
    game.updated_at = NOW(6), game.deleted_date_time = NULL,
    game.head_count = scenario.head_count,
    game.participant_count = scenario.receiver_count + 1,
    game.game_status = CASE WHEN scenario.receiver_count + 1 = scenario.head_count
                           THEN 'CLOSED' ELSE 'RECRUITING' END,
    game.match_format = 'THREE_ON_THREE', game.match_gender_type = 'MIXED',
    game.start_date_time = TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL scenario.scenario_no + 7 DAY), '19:00:00'),
    game.end_date_time = TIMESTAMP(DATE_ADD(CURRENT_DATE, INTERVAL scenario.scenario_no + 7 DAY), '21:00:00');


/* ============================================================
 * 4. 전용 경기 참가 정보 초기화 (일반 경기 참가 정보는 유지)
 * ============================================================
 */

DELETE participation
FROM participant_game_entity AS participation
INNER JOIN perf_update_scenarios AS scenario
    ON scenario.game_id = participation.game_entity_game_id;

INSERT INTO participant_game_entity (
    created_at, updated_at, accept_date_time, canceled_date_time,
    deleted_date_time, kickout_date_time, participant_game_status,
    game_entity_game_id, user_entity_user_id
)
SELECT NOW(6), NOW(6), NOW(6), NULL, NULL, NULL, 'ACCEPT',
       scenario.game_id, users.user_id
FROM perf_update_scenarios AS scenario
INNER JOIN perf_update_sequence AS seq ON seq.sequence_no <= scenario.receiver_count
INNER JOIN user_entity AS users
    ON users.email = CASE WHEN seq.sequence_no = 0 THEN 'perf-update-creator@example.test'
                         ELSE CONCAT('perf-update-user-', LPAD(seq.sequence_no, 3, '0'), '@example.test') END
   AND users.deleted_date_time IS NULL;

COMMIT;


/* ============================================================
 * 5. 생성 결과 검증 및 다음 단계에 사용할 경기 ID 출력
 * ============================================================
 * 정상 결과: 13행 모두 PASS, actual_participants 합계 1064
 * 한 행이라도 FAIL이면 테스트를 실행하지 말고 생성 결과를 확인한다.
 */

SELECT scenario.scenario_name, game.game_id, game.head_count,
       game.participant_count AS stored_participants,
       COUNT(participation.participant_game_id) AS actual_participants,
       COUNT(DISTINCT participation.user_entity_user_id) AS distinct_participants,
       COUNT(DISTINCT CASE WHEN participation.user_entity_user_id <> @perf_update_creator_id
                           THEN participation.user_entity_user_id END) AS actual_receivers,
       scenario.receiver_count AS expected_receivers,
       game.start_date_time,
       CASE WHEN COUNT(participation.participant_game_id) = scenario.receiver_count + 1
                 AND COUNT(DISTINCT participation.user_entity_user_id) = scenario.receiver_count + 1
                 AND SUM(participation.user_entity_user_id = @perf_update_creator_id) = 1
                 AND game.participant_count = scenario.receiver_count + 1
                 AND game.head_count >= game.participant_count
                 AND game.start_date_time > DATE_ADD(NOW(), INTERVAL 1 DAY)
            THEN 'PASS' ELSE 'FAIL' END AS validation
FROM perf_update_scenarios AS scenario
INNER JOIN game_entity AS game ON game.game_id = scenario.game_id
LEFT JOIN participant_game_entity AS participation
    ON participation.game_entity_game_id = game.game_id
   AND participation.participant_game_status = 'ACCEPT'
   AND participation.deleted_date_time IS NULL
GROUP BY scenario.scenario_no, scenario.scenario_name, scenario.receiver_count,
         game.game_id, game.head_count, game.participant_count, game.start_date_time
ORDER BY scenario.scenario_no;

SELECT @perf_update_creator_id AS creator_user_id,
       'perf-update-creator@example.test' AS creator_email,
       COUNT(game_id) AS game_count,
       SUM(receiver_count + 1) AS expected_participation_count,
       MAX(CASE WHEN receiver_count = 1 THEN game_id END) AS smoke_game_id,
       GROUP_CONCAT(CASE WHEN receiver_count = 99 THEN game_id END
                    ORDER BY scenario_no SEPARATOR ',') AS load_game_ids
FROM perf_update_scenarios;

/*
 * 실행 후 위 두 결과를 보관한다.
 * 이후 PATCH는 매번 고유한 제목을 사용해야 실제 변경 이벤트가 발행된다.
 * 현재 파일은 알림을 생성하지 않으며, HTTP/SSE 테스트는 다음 단계에서 수행한다.
 */
