package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.sql.In;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.GameStatus.CLOSED;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@IntegrationTest
@DisplayName("경기 참가 동시성 테스트 - 락 없는 기준선")
class GameParticipantConcurrencyBaseLineTest {

    // 동시 요청 참가자 수
    private static final int REQUEST_COUNT = 100;

    // 경기 정원
    private static final int HEAD_COUNT = 6;

    // 경기 생성자를 제외한 참가 가능 인원 수
    private static final int AVAILABLE_SEATS = 5;


    @Autowired
    private GameService gameService;

    @Autowired
    private GameParticipantService gameParticipantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ParticipantGameRepository participantGameRepository;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("100명이 남은 자리 5자리에 동시 신청 요성 시 응답과 DB 상태 검증")
    void joinConcurrency_test() throws InterruptedException {
        // given

        String runId = UUID.randomUUID()
                .toString().substring(0, 8);

        UserEntity creator = createUser("concurrency-creator-" + runId + "@test.com", "생성자_" + runId, "010-0000-0000");
        userRepository.save(creator);

        List<Long> applicantIds = userRepository.saveAll(
                IntStream.rangeClosed(1, REQUEST_COUNT)
                        .mapToObj(number -> createUser(
                                "concurrency-" + runId + "-%03d@test.com".formatted(number),
                                "참가자-" + runId + "-%03d".formatted(number),
                                "010-1000-%04d".formatted(number)
                        )).toList()
        ).stream().map(UserEntity::getUserId).toList();

        LocalDateTime gameStart = LocalDateTime.now(clock)
                .plusDays(2)
                .withSecond(0)
                .withNano(0);

        CreateGameRequest request = new CreateGameRequest(
                "동시성 기준선 경기 " + runId,
                "락 없는 경기 참가의 정합성을 검증합니다.",
                HEAD_COUNT,
                INDOOR,
                THREE_ON_THREE,
                MIXED,
                gameStart,
                gameStart.plusHours(2),
                "동시성 테스트 농구장 " + runId,
                "서울특별시 송파구 테스트로 1",
                37.515,
                127.073
        );

        Long gameId = gameService.createGame(creator.getUserId(), request).gameId();

        GameEntity initialGame = gameRepository.findById(gameId)
                .orElseThrow();

        assertAll(
                () -> assertEquals(HEAD_COUNT, initialGame.getHeadCount()),
                () -> assertEquals(1, initialGame.getParticipantCount()),
                () -> assertEquals(REQUEST_COUNT, applicantIds.size())
        );
        // when


        /*
         * REQUEST_COUNT가 100이므로 최대 100개의 작업 스레드를 만든다.
         * 단, 스레드 100개가 DB 연결 100개를 가진다는 뜻은 아니다.
         */
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);

        // 100개 작업이 모두 출발선에 도착했는지 확인한다.
        // 각 작업이 준비되면 ready.countDown()을 호출한다.
        /*
         * 100개 작업이 모두 출발선에 도착했는지 확인
         * 각 작업이 준비되면 ready.countDown()을 호출한다.
         */
        CountDownLatch ready = new CountDownLatch(REQUEST_COUNT);

        // 모든 작업을 대기시켰다가 한 번에 출발시키는 신호다.
        // 테스트 스레드가 start.countDown()을 호출하면 대기가 풀린다.
        CountDownLatch start = new CountDownLatch(1);

        // 100개 작업이 모두 끝났는지 확인한다.
        // 각 작업은 성공·실패와 관계없이 finally에서 done.countDown()을 호출한다.
        CountDownLatch done = new CountDownLatch(REQUEST_COUNT);

        // 여러 스레드가 동시에 성공 건수를 증가시켜도 값이 유실되지 않게 한다.
        AtomicInteger successCount = new AtomicInteger();

        // 각 작업에서 발생한 예외를 스레드 안전하게 모은다.
        // 나중에 정상적인 '정원 초과'와 예상치 못한 오류를 구분한다.
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try {
            for (Long applicantId : applicantIds) {
                executor.submit(() -> {
                    // 이 작업이 출발선에 도착
                    ready.countDown();

                    try {
                        // 테스트 스레드의 출발 신호를 기다린다.
                        start.await();

                        gameParticipantService.join(gameId, applicantId);

                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failures.add(e);
                    } catch (Throwable e) {
                        failures.add(e);
                    } finally {
                        // 성공 여부와 상관없이 완료를 알린다.
                        done.countDown();
                    }
                });
            }

            assertTrue(ready.await(20, TimeUnit.SECONDS), "모든 참가 작업이 출발선에 모이지 못했습니다.");

            // 준비된 작업을 거의 동시에 출발시킨다.
            start.countDown();

            assertTrue(done.await(40, TimeUnit.SECONDS), "모든 참가 작업이 제한 시간 안에 끝나지 않았습니다.");

        } finally {
            // 중간에 테스트가 실패해도 대기 중인 스레드를 풀어준다.
            start.countDown();

            executor.shutdown();

            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "작업 스레드가 종료되지 않았습니다.");

        }

        // then

        GameEntity finalGame = gameRepository.findById(gameId)
                .orElseThrow();

        int actualAcceptCount = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(
                List.of(ParticipantGameStatus.ACCEPT), gameId
        ).size();

        long fullRejectionCount = failures.stream()
                .filter(CustomException.class::isInstance)
                .map(CustomException.class::cast)
                .filter(e -> e.getErrorCode() == ErrorCode.FULL_HEADCOUNT_GAME)
                .count();

        long unexpectedFailureCount = failures.size() - fullRejectionCount;

        Map<String, Integer> failureType = new ConcurrentHashMap<>();

        for (Throwable failure : failures) {
            String name;
            if (failure instanceof CustomException customException) {
                name = customException.getErrorCode().name();
            } else {
                name = failure.getClass().getSimpleName();
            }

            failureType.merge(name, 1, Integer::sum);

        }

        log.info("요청 수 : {}", REQUEST_COUNT);
        log.info("참가 성공 : {}", successCount.get());
        log.info("정원 초과 정상 거절 : {}", fullRejectionCount);
        log.info("기타 실패 : {}", unexpectedFailureCount);
        log.info("실패 유형 : {}", failureType);
        log.info("game.participantCount : {}", finalGame.getParticipantCount());
        log.info("실제 ACCEPT : {}", actualAcceptCount);
        log.info("경기 상태 : {}", finalGame.getGameStatus());

        // 이 조건은 서비스가 지켜야 하는 불변식이다.
        // 락 없는 기준선에서 실패하면 그 결과를 그대로 기록한다.
        assertAll(
                () -> assertEquals(
                        REQUEST_COUNT,
                        successCount.get() + failures.size(),
                        "전체 요청 집계"
                ),
                () -> assertEquals(
                        AVAILABLE_SEATS,
                        successCount.get(),
                        "참가 성공 수"
                ),
                () -> assertEquals(
                        REQUEST_COUNT - AVAILABLE_SEATS,
                        fullRejectionCount,
                        "정원 초과 정상 거절 수"
                ),
                () -> assertEquals(
                        0,
                        unexpectedFailureCount,
                        "시스템 오류 등 예상하지 못한 실패"
                ),
                () -> assertEquals(
                        HEAD_COUNT,
                        finalGame.getParticipantCount(),
                        "경기에 저장된 참가 인원"
                ),
                () -> assertEquals(
                        HEAD_COUNT,
                        actualAcceptCount,
                        "실제 ACCEPT 참가자 수"
                ),
                () -> assertEquals(
                        finalGame.getParticipantCount(),
                        actualAcceptCount,
                        "집계값과 실제 참가 데이터의 일치"
                ),
                () -> assertEquals(
                        CLOSED,
                        finalGame.getGameStatus(),
                        "경기 상태"
                )
        );
    }


    private UserEntity createUser(String email, String nickname, String phone) {
        return UserEntity.create(
                email,
                "encoded-password",
                nickname,
                nickname,
                LocalDate.of(1997, 1, 1),
                phone,
                "서울특별시 송파구",
                Position.GUARD,
                GenderType.MALE
        );
    }

}
