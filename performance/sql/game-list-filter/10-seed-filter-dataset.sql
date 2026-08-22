/*
 * ============================================================
 * 경기 목록 필터 성능 테스트 데이터 생성
 * ============================================================
 *
 * 목적:
 * - 경기 목록 필터별 성능 기준선 측정
 * - 기존 복합 인덱스 상태에서 실행계획 확인
 * - 필터 컬럼 간 과도한 데이터 상관관계 제거
 *
 * 생성 데이터:
 * - 사용자 1,000건
 * - 경기 500,000건
 *
 * 주의:
 * - 로컬 성능 테스트 DB 전용
 * - 운영 DB에서 절대 실행하지 않는다.
 * - 기존 로컬 데이터는 모두 제거된다.
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
 * target_database가 basketball인지 직접 확인한 후
 * 아래 초기화 SQL을 실행한다.
 */


/* ============================================================
 * 1. 기존 데이터 제거
 * ============================================================
 *
 * 성능 테스트 전용 로컬 DB를 초기화한다.
 *
 * 외래키가 연결된 자식 테이블을 함께 비운다.
 * ============================================================
 */

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE participant_game_entity;
TRUNCATE TABLE notification_entity;
TRUNCATE TABLE report_entity;
TRUNCATE TABLE black_list_entity;
TRUNCATE TABLE oauth_account_entity;
TRUNCATE TABLE game_entity;
TRUNCATE TABLE user_entity;

SET FOREIGN_KEY_CHECKS = 1;


/* 초기화 확인 */

SELECT COUNT(*) AS users_before
FROM user_entity;

SELECT COUNT(*) AS games_before
FROM game_entity;


/*
 * 두 결과가 모두 0이어야 한다.
 */


/* ============================================================
 * 2. 1부터 500,000까지 임시 숫자 테이블 생성
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_filter_sequence;

CREATE TEMPORARY TABLE perf_filter_sequence (
    sequence_no INT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;


INSERT INTO perf_filter_sequence (
    sequence_no
)
WITH digits AS (
    SELECT 0 AS n
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
    d0.n
        + d1.n * 10
        + d2.n * 100
        + d3.n * 1000
        + d4.n * 10000
        + d5.n * 100000
        + 1 AS sequence_no
FROM digits AS d0
         CROSS JOIN digits AS d1
         CROSS JOIN digits AS d2
         CROSS JOIN digits AS d3
         CROSS JOIN digits AS d4
         CROSS JOIN digits AS d5
WHERE (
          d0.n
              + d1.n * 10
              + d2.n * 100
              + d3.n * 1000
              + d4.n * 10000
              + d5.n * 100000
          ) < 500000;


/* 숫자 테이블 확인 */

SELECT
    COUNT(*) AS sequence_count,
    MIN(sequence_no) AS min_sequence,
    MAX(sequence_no) AS max_sequence
FROM perf_filter_sequence;


/*
 * 정상 결과:
 *
 * sequence_count = 500000
 * min_sequence   = 1
 * max_sequence   = 500000
 */


/* ============================================================
 * 3. 성능 테스트 사용자 1,000명 생성
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
SELECT
    NOW(6),

    NOW(6),

    CONCAT(
            '서울특별시 성능테스트로 ',
            sequence_no
    ),

    DATE_ADD(
            '1980-01-01',
            INTERVAL MOD(sequence_no * 37, 9000) DAY
    ),

    NULL,

    CONCAT(
            'perf-filter-user-',
            sequence_no,
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
            '필터테스트사용자',
            sequence_no
    ),

    CONCAT(
            'filter_nickname_',
            sequence_no
    ),

    NULL,

    CONCAT(
            '010',
            LPAD(sequence_no, 8, '0')
    ),

    CASE MOD(sequence_no, 4)
        WHEN 0 THEN 'guard'
        WHEN 1 THEN 'center'
        WHEN 2 THEN 'forward'
        ELSE 'none'
        END,

    'user'

FROM perf_filter_sequence

WHERE sequence_no <= 1000;


COMMIT;


/* 사용자 생성 결과 확인 */

SELECT
    COUNT(*) AS total_test_users,
    MIN(user_id) AS min_user_id,
    MAX(user_id) AS max_user_id
FROM user_entity
WHERE email LIKE 'perf-filter-user-%@example.test';


/*
 * 정상 결과:
 *
 * total_test_users = 1000
 */


/* ============================================================
 * 4. 경기 데이터 500,000건 생성
 * ============================================================
 *
 * 분포:
 * - 삭제 경기: 약 5%
 * - 예정 경기: 약 70%
 * - 지난 경기: 약 30%
 * - 서울: 약 40%
 * - 경기: 약 25%
 * - 나머지 지역: 약 35%
 * - 모집 중: 약 70%
 * - 마감: 약 30%
 *
 * 필터 데이터 상관관계 제거:
 * - match_format은 sequence_no의 홀짝
 * - field_status는 2개 행 단위로 변경
 * - match_gender_type은 4개 행 단위로 변경
 * - game_status는 12개 행 단위로 변경
 *
 * 따라서 다음 조합이 모두 생성된다.
 *
 * - INDOOR + THREE_ON_THREE
 * - INDOOR + FIVE_ON_FIVE
 * - OUTDOOR + THREE_ON_THREE
 * - OUTDOOR + FIVE_ON_FIVE
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
    /* 생성 시각 */
    TIMESTAMPADD(
        DAY,
            -(30 + MOD(seed.sequence_no * 13, 90)),
            seed.start_date_time
    ),

    /* 수정 시각 */
    TIMESTAMPADD(
        DAY,
            MOD(seed.sequence_no * 7, 10),
            TIMESTAMPADD(
                DAY,
                    -(30 + MOD(seed.sequence_no * 13, 90)),
                    seed.start_date_time
            )
    ),

    /* 주소 */
    CONCAT(
            CASE seed.city_bucket
                WHEN 0 THEN '서울특별시'
                WHEN 1 THEN '경기도'
                WHEN 2 THEN '인천광역시'
                WHEN 3 THEN '부산광역시'
                WHEN 4 THEN '대구광역시'
                ELSE '대전광역시'
                END,
            ' 필터성능테스트로 ',
            seed.sequence_no
    ),

    /* 정규화된 지역 */
    CASE seed.city_bucket
        WHEN 0 THEN 'seoul'
        WHEN 1 THEN 'gyeonggi'
        WHEN 2 THEN 'incheon'
        WHEN 3 THEN 'busan'
        WHEN 4 THEN 'daegu'
        ELSE 'daejeon'
        END,

    /* 내용 */
    CONCAT(
            '경기 목록 필터 성능 테스트용 내용 ',
            seed.sequence_no
    ),

    /* 삭제 여부: 약 5% */
    CASE
        WHEN seed.deleted_bucket = 0
            THEN TIMESTAMPADD(
            DAY,
                -1,
                NOW(6)
                 )
        ELSE NULL
        END,

    /* 종료 시각 */
    TIMESTAMPADD(
        HOUR,
            2,
            seed.start_date_time
    ),

    /* 실내·실외 */
    CASE seed.field_bucket
        WHEN 0 THEN 'indoor'
        ELSE 'outdoor'
        END,

    /* 모집 상태 */
    CASE
        WHEN seed.status_bucket < 7
            THEN 'recruiting'
        ELSE 'closed'
        END,

    /* 경기 정원 */
    CASE seed.match_bucket
        WHEN 0 THEN 6
        ELSE 10
        END,

    /* 위도 */
    37.400000
        + MOD(seed.sequence_no * 17, 1000) / 100000.0,

    /* 경도 */
    126.800000
        + MOD(seed.sequence_no * 19, 1000) / 100000.0,

    /* 경기 형식 */
    CASE seed.match_bucket
        WHEN 0 THEN 'three_on_three'
        ELSE 'five_on_five'
        END,

    /* 참가 성별 */
    CASE seed.gender_bucket
        WHEN 0 THEN 'male_only'
        WHEN 1 THEN 'female_only'
        ELSE 'mixed'
        END,

    /*
     * 참가 인원
     *
     * 마감 경기:
     * - 정원과 동일
     *
     * 모집 중 경기:
     * - 최소 1명
     * - 정원 미만
     */
    CASE
        WHEN seed.status_bucket >= 7
            THEN CASE seed.match_bucket
                     WHEN 0 THEN 6
                     ELSE 10
            END

        WHEN seed.match_bucket = 0
            THEN 1 + MOD(seed.sequence_no * 7, 5)

        ELSE 1 + MOD(seed.sequence_no * 11, 9)
        END,

    /* 장소명 */
    CONCAT(
            'FILTER_PERF_COURT_',
            seed.sequence_no
    ),

    /* 시작 시각 */
    seed.start_date_time,

    /* 제목 */
    CONCAT(
            'FILTER_PERF_GAME_',
            seed.sequence_no,
            CASE MOD(seed.sequence_no * 23, 5)
                WHEN 0 THEN '_주말농구'
                WHEN 1 THEN '_직장인농구'
                WHEN 2 THEN '_초보환영'
                WHEN 3 THEN '_저녁경기'
                ELSE '_친선경기'
                END
    ),

    /* 생성자 */
    test_user.user_id

FROM (
         SELECT
             sequence_no,

             /*
              * 지역 분포
              *
              * 0: 서울 40%
              * 1: 경기 25%
              * 2: 인천 10%
              * 3: 부산 10%
              * 4: 대구 7%
              * 5: 대전 8%
              */
             CASE
                 WHEN MOD(sequence_no * 37, 100) < 40
                     THEN 0
                 WHEN MOD(sequence_no * 37, 100) < 65
                     THEN 1
                 WHEN MOD(sequence_no * 37, 100) < 75
                     THEN 2
                 WHEN MOD(sequence_no * 37, 100) < 85
                     THEN 3
                 WHEN MOD(sequence_no * 37, 100) < 92
                     THEN 4
                 ELSE 5
                 END AS city_bucket,

             /* 3대3 / 5대5 */
             MOD(sequence_no, 2)
                     AS match_bucket,

             /*
              * 실내·실외
              *
              * match_bucket과 같은 홀짝식을 사용하지 않는다.
              */
             MOD(
                     FLOOR((sequence_no - 1) / 2),
                     2
             ) AS field_bucket,

             /* 참가 성별 */
             MOD(
                     FLOOR((sequence_no - 1) / 4),
                     3
             ) AS gender_bucket,

             /* 모집 중 70%, 마감 30% */
             MOD(
                     FLOOR((sequence_no - 1) / 12),
                     10
             ) AS status_bucket,

             /* 삭제 5% */
             MOD(
                     sequence_no * 43 + 7,
                     20
             ) AS deleted_bucket,

             /*
              * 시작 시각
              *
              * - 과거 54일
              * - 미래 125일
              * - 09시부터 22시까지
              */
             TIMESTAMPADD(
                 HOUR,
                     9 + MOD(sequence_no * 11, 14),
                     TIMESTAMPADD(
                         DAY,
                             MOD(sequence_no * 37, 180) - 54,
                             CURRENT_DATE
                     )
             ) AS start_date_time

         FROM perf_filter_sequence
     ) AS seed

         JOIN user_entity AS test_user
              ON test_user.email = CONCAT(
                      'perf-filter-user-',
                      MOD(seed.sequence_no - 1, 1000) + 1,
                      '@example.test'
                                   );


COMMIT;


/* ============================================================
 * 5. 옵티마이저 통계 갱신
 * ============================================================
 */

ANALYZE TABLE user_entity;
ANALYZE TABLE game_entity;


/* ============================================================
 * 6. 기본 데이터 분포 확인
 * ============================================================
 */

SELECT
    COUNT(*) AS total_games,

    COALESCE(
            SUM(deleted_date_time IS NULL),
            0
    ) AS active_games,

    COALESCE(
            SUM(deleted_date_time IS NOT NULL),
            0
    ) AS deleted_games,

    COALESCE(
            SUM(start_date_time >= NOW()),
            0
    ) AS upcoming_games,

    COALESCE(
            SUM(start_date_time < NOW()),
            0
    ) AS past_games

FROM game_entity;


/* 지역별 분포 */

SELECT
    city_name,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY city_name
ORDER BY game_count DESC;


/* 경기 형식별 분포 */

SELECT
    match_format,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY match_format
ORDER BY match_format;


/* 실내·실외별 분포 */

SELECT
    field_status,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY field_status
ORDER BY field_status;


/* 참가 성별 조건별 분포 */

SELECT
    match_gender_type,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY match_gender_type
ORDER BY match_gender_type;


/* 모집 상태별 분포 */

SELECT
    game_status,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY game_status
ORDER BY game_status;


/* ============================================================
 * 7. 필터 간 상관관계 확인
 * ============================================================
 */

/*
 * 다음 네 조합이 모두 존재해야 한다.
 */

SELECT
    match_format,
    field_status,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY
    match_format,
    field_status
ORDER BY
    match_format,
    field_status;


/* 지역 + 모집 상태 + 경기 형식 */

SELECT
    city_name,
    game_status,
    match_format,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY
    city_name,
    game_status,
    match_format
ORDER BY
    city_name,
    game_status,
    match_format;


/* ============================================================
 * 8. 성능 시나리오별 대상 데이터 확인
 * ============================================================
 */

/* A. 기본 시작 시간순 */

SELECT COUNT(*) AS default_target_count
FROM game_entity
WHERE deleted_date_time IS NULL
  AND start_date_time >= NOW();


/*
 * B. 날짜 필터
 *
 * k6 FILTER_DATE도 같은 날짜를 사용한다.
 */

SET @filter_date =
    DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY);


SELECT
    @filter_date AS filter_date,
    COUNT(*) AS date_target_count
FROM game_entity
WHERE deleted_date_time IS NULL
  AND start_date_time >= @filter_date
  AND start_date_time <
      DATE_ADD(@filter_date, INTERVAL 1 DAY);


/* C. 서울 + 시작 시간순 */

SELECT COUNT(*) AS city_target_count
FROM game_entity
WHERE deleted_date_time IS NULL
  AND start_date_time >= NOW()
  AND city_name = 'seoul';


/* D. 서울 + 모집 중 + 3대3 */

SELECT COUNT(*) AS detailed_filter_target_count
FROM game_entity
WHERE deleted_date_time IS NULL
  AND start_date_time >= NOW()
  AND city_name = 'seoul'
  AND game_status = 'recruiting'
  AND match_format = 'three_on_three';


/* E. 최신순 */

SELECT COUNT(*) AS latest_target_count
FROM game_entity
WHERE deleted_date_time IS NULL
  AND start_date_time >= NOW();


/* 현재 인덱스 확인 */

SHOW INDEX FROM game_entity;


/* ============================================================
 * 9. 임시 테이블 제거
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_filter_sequence;


/* 완료 시각 */

SELECT NOW(6) AS completed_at;