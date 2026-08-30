/*
 * ============================================================
 * 경기 참가 동시성 테스트 사용자 생성
 * ============================================================
 *
 * 생성 데이터:
 * - 경기 생성자 1명
 * - 동시 참가 요청 사용자 1000명
 *
 * 공통 비밀번호:
 * - Perf@1234
 *
 * 주의:
 * - 로컬 성능 테스트 DB 전용
 * - 운영 DB에서 실행하지 않는다.
 * - 기존 사용자 데이터는 삭제하지 않는다.
 * ============================================================
 */


/* ============================================================
 * 0. 대상 데이터베이스 확인
 * ============================================================
 */

USE basketball;

SELECT
    DATABASE() AS target_database,
    VERSION() AS mysql_version,
    NOW(6) AS started_at;


/*
 * target_database가 basketball인지 확인한 후 진행한다.
 */


/* ============================================================
 * 1. 테스트용 숫자 임시 테이블 생성
 * ============================================================
 *
 * 1부터 1000까지의 숫자를 생성한다.
 * 임시 테이블이므로 현재 DB 연결이 종료되면 자동 삭제된다.
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_participation_sequence;

CREATE TEMPORARY TABLE perf_participation_sequence (
    sequence_no INT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;


INSERT INTO perf_participation_sequence (
    sequence_no
)
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
    ones.number
        + tens.number * 10
        + hundreds.number * 100
        + 1 AS sequence_no
FROM digits AS ones
         CROSS JOIN digits AS tens
         CROSS JOIN digits AS hundreds
WHERE (
          ones.number
              + tens.number * 10
              + hundreds.number * 100
          ) < 1000;


/* 숫자 생성 확인 */

SELECT
    COUNT(*) AS sequence_count,
    MIN(sequence_no) AS min_sequence,
    MAX(sequence_no) AS max_sequence
FROM perf_participation_sequence;


/*
 * 정상 결과:
 *
 * sequence_count = 1000
 * min_sequence   = 1
 * max_sequence   = 1000
 */


/* ============================================================
 * 2. 경기 생성자 생성
 * ============================================================
 *
 * 이메일:
 * - perf-concurrency-creator@example.test
 *
 * 비밀번호:
 * - Perf@1234
 *
 * BCrypt 암호문:
 * - 아래 password 컬럼에 저장된 문자열
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
VALUES (
           NOW(6),
           NOW(6),
           '서울특별시 동시성테스트로 1',
           '1997-01-01',
           NULL,
           'perf-concurrency-creator@example.test',
           b'1',
           'male',
           'local',
           '동시성테스트생성자',
           'concurrency_creator',
           '$2y$10$X41wLjiMCumq51wxDclm7Ok.B/ItPtLEYkAqNIpT12qftI2x1PLaW',
           '01089999999',
           'guard',
           'user'
       )
    ON DUPLICATE KEY UPDATE
                         updated_at = NOW(6),
                         deleted_date_time = NULL,
                         email_auth = b'1',
                         password = VALUES(password),
                         login_provider = 'local',
                         user_type = 'user';


/* ============================================================
 * 3. 동시 참가 사용자 1000명 생성
 * ============================================================
 *
 * 이메일:
 * - perf-concurrency-user-001@example.test
 * - ...
 * - perf-concurrency-user-1000@example.test
 *
 * 모든 사용자의 비밀번호:
 * - Perf@1234
 * ============================================================
 */

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
SELECT
    NOW(6),

    NOW(6),

    CONCAT(
            '서울특별시 동시성테스트로 ',
            sequence_no + 1
    ),

    DATE_ADD(
            '1990-01-01',
            INTERVAL sequence_no DAY
    ),

    NULL,

    CONCAT(
            'perf-concurrency-user-',
            LPAD(
                    sequence_no,
                    GREATEST(
                            3,
                            CHAR_LENGTH(CAST(sequence_no AS CHAR))
                    ),
                    '0'
            ),
            '@example.test'
    ),

    b'1',

    CASE MOD(sequence_no, 3)
        WHEN 0 THEN 'male'
        WHEN 1 THEN 'female'
        ELSE 'none'
        END,

    'local',

    CONCAT(
            '동시성테스트사용자',
            LPAD(
                    sequence_no,
                    GREATEST(
                            3,
                            CHAR_LENGTH(CAST(sequence_no AS CHAR))
                    ),
                    '0'
            )
    ),

    CONCAT(
            'concurrency_user_',
            LPAD(
                    sequence_no,
                    GREATEST(
                            3,
                            CHAR_LENGTH(CAST(sequence_no AS CHAR))
                    ),
                    '0'
            )
    ),

    '$2y$10$X41wLjiMCumq51wxDclm7Ok.B/ItPtLEYkAqNIpT12qftI2x1PLaW',

    CONCAT(
            '0109',
            LPAD(sequence_no, 7, '0')
    ),

    CASE MOD(sequence_no, 4)
        WHEN 0 THEN 'guard'
        WHEN 1 THEN 'center'
        WHEN 2 THEN 'forward'
        ELSE 'none'
        END,

    'user'

FROM perf_participation_sequence

    ON DUPLICATE KEY UPDATE
                         updated_at = NOW(6),
                         deleted_date_time = NULL,
                         email_auth = b'1',
                         password = VALUES(password),
                         login_provider = 'local',
                         user_type = 'user';


COMMIT;


/* ============================================================
 * 4. 생성 결과 확인
 * ============================================================
 */

SELECT
    COUNT(*) AS creator_count
FROM user_entity
WHERE email = 'perf-concurrency-creator@example.test'
  AND deleted_date_time IS NULL;


SELECT
    COUNT(*) AS participant_user_count,

    SUM(
            email = 'perf-concurrency-user-001@example.test'
    ) AS first_user_exists,

    SUM(
            email = 'perf-concurrency-user-1000@example.test'
    ) AS thousandth_user_exists

FROM user_entity
WHERE email LIKE 'perf-concurrency-user-%@example.test'
  AND deleted_date_time IS NULL;


/*
 * 정상 결과:
 *
 * creator_count          = 1
 * participant_user_count = 1000
 *
 * first_user_exists      = 1
 * thousandth_user_exists = 1
 */


/* ============================================================
 * 5. 임시 테이블 제거
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_participation_sequence;

SELECT NOW(6) AS completed_at;
