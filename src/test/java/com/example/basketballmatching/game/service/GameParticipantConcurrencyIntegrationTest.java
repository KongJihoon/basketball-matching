package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.support.concurrency.ConcurrentTestExecutor;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.GameStatus.CLOSED;
import static com.example.basketballmatching.game.type.GameStatus.RECRUITING;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.global.exception.ErrorCode.FULL_HEADCOUNT_GAME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


@IntegrationTest
@DisplayName("경기 참가 동시성 통합 테스트")
@Slf4j
public class GameParticipantConcurrencyIntegrationTest {

    private static final int CONCURRENT_USER_COUNT = 100;
    private static final int GAME_HEAD_COUNT = 6;

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

    private Long gameId;
    private Long creatorId;
    private List<Long> participantIds;

    @BeforeEach
    void setUp() {

        UserEntity creator = saveUser("concurrency-creator@test.com",
                "동시성경기생성자",
                "010-0000-0000");

        creatorId = creator.getUserId();

        List<UserEntity> participants = IntStream.rangeClosed(1, CONCURRENT_USER_COUNT)
                .mapToObj(this::createParticipant)
                .toList();

        participantIds = userRepository.saveAll(participants)
                .stream()
                .map(UserEntity::getUserId)
                .toList();

        LocalDateTime startDateTime = LocalDateTime.now(clock)
                .plusDays(2)
                .withSecond(0)
                .withNano(0);

        LocalDateTime endDateTime = startDateTime.plusHours(2);

        CreateGameRequest request =
                new CreateGameRequest(
                        "동시성 참가 테스트 경기",
                        "동시 참가 요청 정합성을 검증합니다.",
                        GAME_HEAD_COUNT,
                        INDOOR,
                        THREE_ON_THREE,
                        MIXED,
                        startDateTime,
                        endDateTime,
                        "동시성 테스트 농구장",
                        "서울특별시 송파구 테스트로 1",
                        37.515,
                        127.073
                );

        CreateGameResponse response = gameService.createGame(creatorId, request);

        gameId = response.gameId();

    }

    @AfterEach
    void cleanup() {
        participantGameRepository.deleteAllInBatch();
        gameRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시성 테스트 필요 데이터 검증")
    void setUp_success() {
        // given

        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow();

        List<ParticipantGameEntity> acceptedParticipants = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(
                List.of(ACCEPT), gameId
        );



        // when

        // then

        assertAll(
                () -> assertEquals(GAME_HEAD_COUNT, game.getHeadCount()),
                () -> assertEquals(1, game.getParticipantCount()),
                () -> assertEquals(RECRUITING, game.getGameStatus()),
                () -> assertEquals(1, acceptedParticipants.size()),
                () -> assertEquals(creatorId, game.getUserEntity().getUserId()),
                () -> assertEquals(CONCURRENT_USER_COUNT, participantIds.size()),
                () -> assertEquals(CONCURRENT_USER_COUNT + 1, userRepository.count())
        );

    }

    @Disabled("락 적용 전 동시성 문제 재현 테스트 - 락 적용 단계에서 활성화")
    @Test
    @DisplayName("100명이 동시에 참가해도 경기 정원이 보장된다")
    void join_concurrently_preservesConsistency() throws InterruptedException {
        // given

        int availableCount = GAME_HEAD_COUNT - 1;

        // when

        ConcurrentTestExecutor.ConcurrentTestResult result = ConcurrentTestExecutor.execute(participantIds,
                participantId -> gameParticipantService.join(gameId, participantId));


        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow();

        List<ParticipantGameEntity> acceptedParticipants = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(
                List.of(ACCEPT), gameId
        );

        long fullHeadCountFailureCount = result
                .failures().stream()
                .filter(CustomException.class::isInstance)
                .map(CustomException.class::cast)
                .filter(exception -> exception.getErrorCode() == FULL_HEADCOUNT_GAME)
                .count();

        Map<String, Long> failureSummary = result.failures()
                .stream()
                .map(this::resolveFailureName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));


        log.info("=== 락 적용 전 동시 참가 결과 ===");

        log.info("전체 요청 수: {}", result.totalCount());
        log.info("성공 요청 수: {}", result.successCount());
        log.info("실패 요청 수: {}", result.failureCount());
        log.info("실패 유형: {}", failureSummary);
        log.info("실행 시간: {}ms", result.executionTimeMillis());
        log.info("경기 participantCount: {}", game.getParticipantCount());
        log.info("실제 ACCEPT 참가자 수: {}", acceptedParticipants.size());
        log.info("경기 상태: {}", game.getGameStatus());

        // then

        assertAll(
                () -> assertEquals(availableCount, result.successCount()),
                () -> assertEquals(CONCURRENT_USER_COUNT - availableCount, result.failureCount()),
                () -> assertEquals(CONCURRENT_USER_COUNT - availableCount, fullHeadCountFailureCount),
                () -> assertEquals(GAME_HEAD_COUNT, game.getParticipantCount()),
                () -> assertEquals(GAME_HEAD_COUNT, acceptedParticipants.size()),
                () -> assertEquals(game.getParticipantCount(), acceptedParticipants.size()),
                () -> assertEquals(CLOSED, game.getGameStatus())
        );

    }

    private String resolveFailureName(Throwable throwable) {
        if (throwable instanceof CustomException exception) {
            return exception.getErrorCode().name();
        }

        return throwable.getClass().getSimpleName();
    }

    private UserEntity createParticipant(int sequence) {

        return UserEntity.create(
                "concurrency-participant-%03d@test.com".formatted(sequence),
                "encoded-password",
                "동시성참가자%03d".formatted(sequence),
                "동시성참가자%03d".formatted(sequence),
                LocalDate.of(1997,1,1),
                "010-1000-%04d".formatted(sequence),
                "서울특별시 송파구",
                Position.GUARD,
                GenderType.MALE
        );

    }

    private UserEntity saveUser(String email, String nickname, String phone) {

        UserEntity user = UserEntity.create(
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

        return userRepository.save(user);

    }

}
