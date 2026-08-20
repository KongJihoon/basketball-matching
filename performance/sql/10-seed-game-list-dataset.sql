/*
 * ============================================================
 * 경기 목록 API 성능 테스트 데이터 생성 스크립트
 * ============================================================
 *
 * 목적:
 * - GET /api/v1/games 경기 목록 조회 성능 테스트
 * - 복합 인덱스 적용 전후 비교
 * - k6 부하 테스트용 대용량 데이터 준비
 *
 * 생성 데이터:
 * - user_entity: 1,000건
 * - game_entity: 500,000건
 *
 * 데이터 분포:
 * - 예정 경기: 약 70%
 * - 지난 경기: 약 30%
 * - 삭제 경기: 5%
 * - 서울 경기: 40%
 * - 경기 지역: 25%
 * - 나머지 지역: 35%
 * - 모집 중: 70%
 * - 마감: 30%
 *
 * 실행 조건:
 * - MySQL 8.x
 * - 로컬 성능 테스트 DB에서만 실행
 * - user_entity와 game_entity가 비어 있어야 함
 * - 전체 스크립트를 동일한 DataGrip 콘솔에서 실행
 *
 * 주의:
 * - 운영 DB에서 절대 실행하지 말 것
 * - 중복 실행하면 이메일, 닉네임, 경기 유니크 제약 오류가 발생할 수 있음
 * ============================================================
 */


/* ============================================================
 * 0. 대상 데이터베이스 선택
 * ============================================================
 */

USE basketball;


/* 현재 접속한 데이터베이스 확인 */

SELECT DATABASE() AS target_database;


/* 기존 사용자 데이터 확인: 실행 전 0이어야 함 */

SELECT COUNT(*) AS users_before
FROM user_entity;


/* 기존 경기 데이터 확인: 실행 전 0이어야 함 */

SELECT COUNT(*) AS games_before
FROM game_entity;


/*
 * 위 두 결과가 모두 0인지 확인한 뒤 아래 스크립트를 실행한다.
 */


/* ============================================================
 * 1. 1부터 500,000까지의 임시 숫자 테이블 생성
 * ============================================================
 *
 * MySQL TEMPORARY TABLE은 같은 쿼리에서 여러 번 재참조할 수 없으므로
 * 숫자 0~9는 CTE로 만들고, 최종 숫자만 임시 테이블에 저장한다.
 *
 * 임시 테이블은 현재 DataGrip 연결 세션이 종료되면 자동으로 사라진다.
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_sequence;


CREATE TEMPORARY TABLE perf_sequence (
    sequence_no INT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;


INSERT INTO perf_sequence (sequence_no)
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


/* 숫자 테이블 생성 결과 확인 */

SELECT
    COUNT(*) AS sequence_count,
    MIN(sequence_no) AS min_sequence,
    MAX(sequence_no) AS max_sequence
FROM perf_sequence;


/*
 * 정상 결과:
 *
 * sequence_count = 500000
 * min_sequence   = 1
 * max_sequence   = 500000
 */


/* ============================================================
 * 2. 테스트 사용자 1,000명 생성
 * ============================================================
 *
 * game_entity.user_entity_user_id 외래키를 만족시키기 위한 사용자다.
 *
 * 모든 테스트 사용자 이메일에는 다음 접두사를 사용한다.
 *
 * perf-user-{번호}@example.test
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
    /* 생성 시각 */
    NOW(6) AS created_at,

    /* 수정 시각 */
    NOW(6) AS updated_at,

    /* 사용자 주소 */
    CONCAT(
            '서울특별시 성능테스트로 ',
            sequence_no
    ) AS address,

    /* 생년월일 분산 */
    DATE_ADD(
            '1980-01-01',
            INTERVAL MOD(sequence_no, 9000) DAY
    ) AS birth,

    /* 탈퇴하지 않은 사용자 */
    NULL AS deleted_date_time,

    /* 고유 이메일 */
    CONCAT(
            'perf-user-',
            sequence_no,
            '@example.test'
    ) AS email,

    /* 이메일 인증 완료 */
    b'1' AS email_auth,

    /* 사용자 성별 분산 */
    CASE MOD(sequence_no, 3)
        WHEN 0 THEN 'male'
        WHEN 1 THEN 'female'
        ELSE 'none'
        END AS gender_type,

    /* 로컬 로그인 사용자 */
    'local' AS login_provider,

    /* 사용자 이름 */
    CONCAT(
            '성능테스트사용자',
            sequence_no
    ) AS name,

    /* 고유 닉네임 */
    CONCAT(
            'perf_nickname_',
            sequence_no
    ) AS nickname,

    /* 경기 목록 조회에서는 비밀번호가 필요하지 않음 */
    NULL AS password,

    /* 고유 전화번호 */
    CONCAT(
            '010',
            LPAD(sequence_no, 8, '0')
    ) AS phone,

    /* 포지션 분산 */
    CASE MOD(sequence_no, 4)
        WHEN 0 THEN 'guard'
        WHEN 1 THEN 'center'
        WHEN 2 THEN 'forward'
        ELSE 'none'
        END AS position,

    /* 일반 사용자 */
    'user' AS user_type

FROM perf_sequence

WHERE sequence_no <= 1000;


COMMIT;


/* 생성된 테스트 사용자 확인 */

SELECT
    COUNT(*) AS total_test_users,
    MIN(user_id) AS min_user_id,
    MAX(user_id) AS max_user_id
FROM user_entity
WHERE email LIKE 'perf-user-%@example.test';


/*
 * 정상 결과:
 *
 * total_test_users = 1000
 */


/* ============================================================
 * 3. 경기 데이터 500,000건 생성
 * ============================================================
 *
 * 경기 목록 조회 쿼리에서 사용하는 주요 컬럼의 값을 분산한다.
 *
 * 대상 컬럼:
 * - deleted_date_time
 * - start_date_time
 * - city_name
 * - match_format
 * - field_status
 * - match_gender_type
 * - game_status
 * - created_at
 *
 * 아직 성능 개선용 인덱스는 생성하지 않는다.
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
    /*
     * 경기 생성 시각
     *
     * 경기 시작 30~119일 전에 생성된 것으로 설정한다.
     */
    TIMESTAMPADD(
        DAY,
            -(30 + MOD(game_seed.sequence_no, 90)),
            game_seed.start_date_time
    ) AS created_at,

    /*
     * 경기 수정 시각
     *
     * 생성 이후 0~9일 안에 수정된 것으로 설정한다.
     */
    TIMESTAMPADD(
        DAY,
            MOD(game_seed.sequence_no, 10),
            TIMESTAMPADD(
                DAY,
                    -(30 + MOD(game_seed.sequence_no, 90)),
                    game_seed.start_date_time
            )
    ) AS updated_at,

    /*
     * 경기 주소
     *
     * 지역별 접두사를 사용하고 sequence_no를 포함해
     * 장소·시간 유니크 제약 충돌을 방지한다.
     */
    CONCAT(
            CASE
                WHEN MOD(game_seed.sequence_no, 100) < 40
                    THEN '서울특별시'
                WHEN MOD(game_seed.sequence_no, 100) < 65
                    THEN '경기도'
                WHEN MOD(game_seed.sequence_no, 100) < 75
                    THEN '인천광역시'
                WHEN MOD(game_seed.sequence_no, 100) < 85
                    THEN '부산광역시'
                WHEN MOD(game_seed.sequence_no, 100) < 92
                    THEN '대구광역시'
                ELSE '대전광역시'
                END,
            ' 성능테스트로 ',
            game_seed.sequence_no
    ) AS address,

    /*
     * 지역 분포
     *
     * 서울    40%
     * 경기    25%
     * 인천    10%
     * 부산    10%
     * 대구     7%
     * 대전     8%
     */
    CASE
        WHEN MOD(game_seed.sequence_no, 100) < 40
            THEN 'seoul'
        WHEN MOD(game_seed.sequence_no, 100) < 65
            THEN 'gyeonggi'
        WHEN MOD(game_seed.sequence_no, 100) < 75
            THEN 'incheon'
        WHEN MOD(game_seed.sequence_no, 100) < 85
            THEN 'busan'
        WHEN MOD(game_seed.sequence_no, 100) < 92
            THEN 'daegu'
        ELSE 'daejeon'
        END AS city_name,

    /* 경기 내용 */
    CONCAT(
            '경기 목록 성능 테스트용 경기 내용 ',
            game_seed.sequence_no
    ) AS content,

    /*
     * 삭제 상태
     *
     * 전체 경기의 5%를 삭제된 경기로 만든다.
     */
    CASE
        WHEN MOD(game_seed.sequence_no, 20) = 0
            THEN TIMESTAMPADD(
            DAY,
                -1,
                NOW(6)
                 )
        ELSE NULL
        END AS deleted_date_time,

    /*
     * 경기 종료 시각
     *
     * 모든 경기는 시작 2시간 후 종료한다.
     */
    TIMESTAMPADD(
        HOUR,
            2,
            game_seed.start_date_time
    ) AS end_date_time,

    /* 실내·실외 조건 분산 */
    CASE MOD(game_seed.sequence_no, 2)
        WHEN 0 THEN 'indoor'
        ELSE 'outdoor'
        END AS field_status,

    /*
     * 모집 상태
     *
     * 모집 중 70%
     * 마감    30%
     */
    CASE
        WHEN MOD(game_seed.sequence_no, 10) < 7
            THEN 'recruiting'
        ELSE 'closed'
        END AS game_status,

    /*
     * 경기 정원
     *
     * 3대3: 6명
     * 5대5: 10명
     */
    CASE MOD(game_seed.sequence_no, 2)
        WHEN 0 THEN 6
        ELSE 10
        END AS head_count,

    /* 테스트용 위도 */
    37.400000
        + MOD(game_seed.sequence_no, 1000) / 100000.0
        AS latitude,

    /* 테스트용 경도 */
    126.800000
        + MOD(game_seed.sequence_no, 1000) / 100000.0
        AS longitude,

    /* 경기 형식 분산 */
    CASE MOD(game_seed.sequence_no, 2)
        WHEN 0 THEN 'three_on_three'
        ELSE 'five_on_five'
        END AS match_format,

    /* 참가 성별 조건 분산 */
    CASE MOD(game_seed.sequence_no, 3)
        WHEN 0 THEN 'male_only'
        WHEN 1 THEN 'female_only'
        ELSE 'mixed'
        END AS match_gender_type,

    /*
     * 현재 참가 인원
     *
     * 3대3 경기는 0~6명
     * 5대5 경기는 0~10명
     */
    CASE MOD(game_seed.sequence_no, 2)
        WHEN 0 THEN MOD(game_seed.sequence_no, 7)
        ELSE MOD(game_seed.sequence_no, 11)
        END AS participant_count,

    /*
     * 경기 장소명
     *
     * 모든 장소명을 고유하게 만들어 기존 복합 유니크 제약과
     * 충돌하지 않도록 한다.
     */
    CONCAT(
            'PERF_COURT_',
            game_seed.sequence_no
    ) AS place_name,

    /* 경기 시작 시각 */
    game_seed.start_date_time,

    /*
     * 경기 제목
     *
     * 일부 키워드가 반복되도록 구성한다.
     */
    CONCAT(
            'PERF_GAME_',
            game_seed.sequence_no,
            CASE MOD(game_seed.sequence_no, 5)
                WHEN 0 THEN '_주말농구'
                WHEN 1 THEN '_직장인농구'
                WHEN 2 THEN '_초보환영'
                WHEN 3 THEN '_저녁경기'
                ELSE '_친선경기'
                END
    ) AS title,

    /*
     * 경기 생성자
     *
     * 생성된 테스트 사용자 1,000명에게 경기를 순환 배정한다.
     */
    u.user_id

FROM (
         SELECT
             sequence_no,

             /*
              * 경기 시작 시각
              *
              * 기준일로부터:
              * - 과거 54일
              * - 미래 125일
              *
              * 지난 경기 약 30%, 예정 경기 약 70%가 만들어진다.
              *
              * 경기 시간:
              * - 09시부터 22시까지 분산
              */
             TIMESTAMPADD(
                 HOUR,
                     9 + MOD(sequence_no, 14),
                     TIMESTAMPADD(
                         DAY,
                             MOD(sequence_no, 180) - 54,
                             CURRENT_DATE
                     )
             ) AS start_date_time

         FROM perf_sequence
     ) AS game_seed

         JOIN user_entity AS u
              ON u.email = CONCAT(
                      'perf-user-',
                      MOD(game_seed.sequence_no - 1, 1000) + 1,
                      '@example.test'
                           );


COMMIT;


/* ============================================================
 * 4. MySQL 옵티마이저 통계 갱신
 * ============================================================
 *
 * 대량 INSERT 후 통계정보를 갱신해 실행계획 측정값의 신뢰도를 높인다.
 * ============================================================
 */

ANALYZE TABLE user_entity;
ANALYZE TABLE game_entity;


/* ============================================================
 * 5. 생성 결과 검증
 * ============================================================
 */


/* 전체·삭제·예정·지난 경기 분포 */

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


/* 지역별 데이터 분포 */

SELECT
    city_name,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY city_name
ORDER BY game_count DESC;


/* 모집 상태별 데이터 분포 */

SELECT
    game_status,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY game_status
ORDER BY game_status;


/* 경기 형식별 데이터 분포 */

SELECT
    match_format,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY match_format
ORDER BY match_format;


/* 실내·실외 데이터 분포 */

SELECT
    field_status,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY field_status
ORDER BY field_status;


/* 참가 성별 조건 분포 */

SELECT
    match_gender_type,
    COUNT(*) AS game_count
FROM game_entity
GROUP BY match_gender_type
ORDER BY match_gender_type;


/*
 * 실제 기본 경기 목록 조회 쿼리의 대상 데이터 수
 *
 * GameQueryRepository의 기본 조건:
 * - deleted_date_time IS NULL
 * - start_date_time >= NOW()
 */
SELECT COUNT(*) AS default_query_target_count
FROM game_entity
WHERE deleted_date_time IS NULL
  AND start_date_time >= NOW();


/* 현재 game_entity 인덱스 확인 */

SHOW INDEX FROM game_entity;


/* ============================================================
 * 6. 임시 숫자 테이블 제거
 * ============================================================
 */

DROP TEMPORARY TABLE IF EXISTS perf_sequence;


/* ============================================================
 * 완료
 * ============================================================
 *
 * 예상 결과:
 *
 * user_entity
 * - 테스트 사용자: 1,000건
 *
 * game_entity
 * - 전체 경기: 500,000건
 * - 활성 경기: 약 475,000건
 * - 삭제 경기: 약 25,000건
 * - 예정 경기: 약 350,000건
 * - 지난 경기: 약 150,000건
 *
 * 실제 예정/지난 경기 개수는 실행 시각에 따라 조금 달라질 수 있다.
 * ============================================================
 */