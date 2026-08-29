/*
 * ============================================================
 * 경기 참가 동시성 테스트 결과 검증
 * ============================================================
 *
 * 검증 대상:
 * - 경기 정원
 * - GameEntity의 participant_count
 * - 실제 ACCEPT 참가자 수
 * - 성공한 일반 참가자 수
 * - 참가 상태별 인원
 * - 중복 참가 데이터
 * - 경기 모집 상태
 *
 * 정상적인 최종 결과:
 * - head_count = 6
 * - participant_count = 6
 * - ACCEPT 참가자 = 6
 * - 일반 사용자 참가 성공 = 5
 * - game_status = closed
 * - 초과 승인 = 0
 * - 중복 참가 = 0
 * ============================================================
 */


/* ============================================================
 * 0. 데이터베이스 및 문자열 비교 설정
 * ============================================================
 */

USE basketball;

SET NAMES utf8mb4
    COLLATE utf8mb4_unicode_ci;


SELECT
    DATABASE() AS target_database,
    VERSION() AS mysql_version,
    NOW(6) AS verified_at;


/* ============================================================
 * 1. 테스트 경기 조회
 * ============================================================
 */

SET @test_game_title =
    _utf8mb4'PERF_CONCURRENCY_GAME'
    COLLATE utf8mb4_unicode_ci;

SET @test_game_id = (
    SELECT game_id
    FROM game_entity
    WHERE title = @test_game_title
      AND deleted_date_time IS NULL
    ORDER BY game_id DESC
    LIMIT 1
);


SELECT
    @test_game_id AS test_game_id;


/*
 * test_game_id가 NULL이면
 * 20-prepare-participation-game.sql을 다시 실행한다.
 */


/* ============================================================
 * 2. 경기 기본 상태 확인
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


/* ============================================================
 * 3. 참가 상태별 데이터 확인
 * ============================================================
 */

SELECT
    participant_game_status,
    COUNT(*) AS participant_status_count
FROM participant_game_entity
WHERE game_entity_game_id = @test_game_id
GROUP BY participant_game_status
ORDER BY participant_game_status;


/*
 * 정상적인 락 적용 후 결과:
 *
 * participant_game_status = accept
 * participant_status_count = 6
 *
 * 정원 초과로 거절된 요청은 참가 레코드가 생성되지 않는다.
 */


/* ============================================================
 * 4. 핵심 정합성 결과 확인
 * ============================================================
 */

SELECT
    g.game_id,

    g.head_count,

    g.participant_count
            AS game_participant_count,

    COUNT(
            CASE
                WHEN p.participant_game_status = 'accept'
                    THEN 1
                END
    ) AS actual_accept_count,

    COUNT(
            CASE
                WHEN p.participant_game_status = 'accept'
                    AND p.user_entity_user_id
                         <> g.user_entity_user_id
                    THEN 1
                END
    ) AS successful_join_request_count,

    g.participant_count
        - COUNT(
            CASE
                WHEN p.participant_game_status = 'accept'
                    THEN 1
                END
          ) AS participant_count_difference,

    GREATEST(
            COUNT(
                    CASE
                        WHEN p.participant_game_status = 'accept'
                            THEN 1
                        END
            ) - g.head_count,
            0
    ) AS overbooking_count,

    g.game_status

FROM game_entity AS g

         LEFT JOIN participant_game_entity AS p
                   ON p.game_entity_game_id = g.game_id

WHERE g.game_id = @test_game_id

GROUP BY
    g.game_id,
    g.head_count,
    g.participant_count,
    g.game_status;


/*
 * 정상 결과:
 *
 * head_count                   = 6
 * game_participant_count       = 6
 * actual_accept_count          = 6
 * successful_join_request_count = 5
 * participant_count_difference = 0
 * overbooking_count            = 0
 * game_status                  = closed
 */


/* ============================================================
 * 5. 동일 사용자의 중복 참가 데이터 확인
 * ============================================================
 */

SELECT
    user_entity_user_id,
    COUNT(*) AS duplicate_count
FROM participant_game_entity
WHERE game_entity_game_id = @test_game_id
GROUP BY user_entity_user_id
HAVING COUNT(*) > 1;


/*
 * 정상 결과:
 *
 * 조회 결과 0행
 */


/* ============================================================
 * 6. 참가 사용자 고유성 확인
 * ============================================================
 */

SELECT
    COUNT(*) AS total_participation_rows,

    COUNT(
            DISTINCT user_entity_user_id
    ) AS distinct_participant_users,

    COUNT(*) - COUNT(
            DISTINCT user_entity_user_id
               ) AS duplicated_participation_rows

FROM participant_game_entity

WHERE game_entity_game_id = @test_game_id;


/*
 * 정상 결과:
 *
 * total_participation_rows       = 6
 * distinct_participant_users     = 6
 * duplicated_participation_rows  = 0
 */


/* ============================================================
 * 7. ACCEPT 참가자 상세 확인
 * ============================================================
 */

SELECT
    p.participant_game_id,
    p.user_entity_user_id,
    u.email,
    p.participant_game_status,
    p.accept_date_time,
    CASE
        WHEN p.user_entity_user_id
            = g.user_entity_user_id
            THEN 'CREATOR'
        ELSE 'PARTICIPANT'
        END AS participant_role

FROM participant_game_entity AS p

         JOIN user_entity AS u
              ON u.user_id = p.user_entity_user_id

         JOIN game_entity AS g
              ON g.game_id = p.game_entity_game_id

WHERE p.game_entity_game_id = @test_game_id
  AND p.participant_game_status = 'accept'

ORDER BY
    p.accept_date_time,
    p.participant_game_id;


/* ============================================================
 * 8. 최종 성공·실패 판정
 * ============================================================
 */

WITH concurrency_result AS (
    SELECT
        g.game_id,
        g.head_count,
        g.participant_count,
        g.game_status,

        COUNT(
                CASE
                    WHEN p.participant_game_status = 'accept'
                        THEN 1
                    END
        ) AS actual_accept_count,

        COUNT(
                CASE
                    WHEN p.participant_game_status = 'accept'
                        AND p.user_entity_user_id
                             <> g.user_entity_user_id
                        THEN 1
                    END
        ) AS successful_join_request_count

    FROM game_entity AS g

             LEFT JOIN participant_game_entity AS p
                       ON p.game_entity_game_id = g.game_id

    WHERE g.game_id = @test_game_id

    GROUP BY
        g.game_id,
        g.head_count,
        g.participant_count,
        g.game_status
)

SELECT
    game_id,
    head_count,
    participant_count,
    actual_accept_count,
    successful_join_request_count,
    game_status,

    CASE
        WHEN participant_count = 6
            AND actual_accept_count = 6
            AND successful_join_request_count = 5
            AND participant_count = actual_accept_count
            AND actual_accept_count <= head_count
            AND game_status = 'closed'
            THEN 'PASS'
        ELSE 'FAIL'
        END AS concurrency_consistency_result

FROM concurrency_result;


/*
 * 락 적용 후 기대 결과:
 *
 * concurrency_consistency_result = PASS
 *
 * 락 적용 전에는 다음 문제로 FAIL이 예상된다.
 *
 * - 5건보다 많은 참가 요청 성공
 * - actual_accept_count가 6을 초과
 * - participant_count와 actual_accept_count 불일치
 * - 초과 승인 발생
 */


/* ============================================================
 * 9. 검증 완료
 * ============================================================
 */

SELECT NOW(6) AS completed_at;