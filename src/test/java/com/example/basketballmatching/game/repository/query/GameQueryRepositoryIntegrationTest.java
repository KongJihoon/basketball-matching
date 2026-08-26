package com.example.basketballmatching.game.repository.query;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.dto.request.GameListCondition;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.basketballmatching.game.type.CityName.GYEONGGI;
import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.FieldStatus.OUTDOOR;
import static com.example.basketballmatching.game.type.GameSortType.LATEST;
import static com.example.basketballmatching.game.type.GameSortType.START_TIME_ASC;
import static com.example.basketballmatching.game.type.GameStatus.RECRUITING;
import static com.example.basketballmatching.game.type.MatchFormat.FIVE_ON_FIVE;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
@Transactional
@DisplayName("GameQueryRepository 통합 테스트")
class GameQueryRepositoryIntegrationTest {

    @Autowired
    private GameQueryRepository gameQueryRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private UserEntity creator;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(
                UserEntity.create(
                        "query-creator@test.com",
                        "encoded-password",
                        "조회테스트생성자",
                        "조회테스트생성자",
                        LocalDate.of(1997, 1, 1),
                        "010-3333-3333",
                        "서울특별시 송파구",
                        Position.GUARD,
                        GenderType.MALE
                )
        );

        now = LocalDateTime.now(clock)
                .withSecond(0)
                .withNano(0);
    }


    @Test
    @DisplayName("기본 조회 시 삭제되지 않은 미래 경기를 시작 시간순으로 조회")
    void findGames_success_defaultCondition() {
        // given

        GameEntity earlyGame = saveGame(
                "빠른 미래 경기",
                "잠실 1경기장",
                "서울특별시 송파구 테스트로 1",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(2)
        );

        GameEntity lateGame = saveGame(
                "늦은 미래 경기",
                "잠실 2경기장",
                "서울특별시 송파구 테스트로 2",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(4)
        );

        saveGame(
                "지난 경기",
                "잠실 3경기장",
                "서울특별시 송파구 테스트로 3",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.minusDays(1)
        );

        GameEntity deletedGame = saveGame(
                "삭제된 미래 경기",
                "잠실 4경기장",
                "서울특별시 송파구 테스트로 4",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(3)
        );

        deletedGame.cancelByCreatorUnavailable(now);

        flushAndClear();

        GameListCondition condition = condition(
                null,
                null,
                null,
                null,
                null,
                null,
                START_TIME_ASC
        );

        // when

        Page<GameEntity> result = gameQueryRepository.findGames(condition, PageRequest.of(0, 10), now);

        // then

        List<Long> gameIds = result.getContent()
                .stream()
                .map(GameEntity::getGameId)
                .toList();

        assertAll(
                () -> assertEquals(
                        List.of(
                                earlyGame.getGameId(),
                                lateGame.getGameId()
                        ),
                        gameIds
                ),
                () -> assertEquals(
                        2,
                        result.getTotalElements()
                ),
                () -> assertEquals(
                        1,
                        result.getTotalPages()
                )
        );

    }

    @Test
    @DisplayName("키워드, 지역 및 경기 조건을 조합하여 조회")
    void findGames_success_combinedCondition() {
        // given
        GameEntity expectedGame = saveGame(
                "잠실 초보 농구",
                "잠실체육관",
                "서울특별시 송파구 테스트로 10",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(2)
        );

        saveGame(
                "강남 직장인 농구",
                "강남체육관",
                "서울특별시 강남구 테스트로 11",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(3)
        );

        saveGame(
                "잠실과 이름이 비슷한 경기",
                "경기체육관",
                "경기도 성남시 테스트로 12",
                GYEONGGI,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(4)
        );

        saveGame(
                "잠실 5대5 농구",
                "잠실 5대5 체육관",
                "서울특별시 송파구 테스트로 13",
                SEOUL,
                FIVE_ON_FIVE,
                OUTDOOR,
                MIXED,
                now.plusDays(5)
        );

        flushAndClear();

        GameListCondition condition = condition(
                "잠실",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                RECRUITING,
                START_TIME_ASC
        );

        // when

        Page<GameEntity> result =
                gameQueryRepository.findGames(
                        condition,
                        PageRequest.of(0, 10),
                        now
                );

        // then

        assertEquals(1, result.getTotalElements());

        assertEquals(expectedGame.getGameId(), result.getContent().get(0).getGameId());


    }

    @Test
    @DisplayName("최신순 정렬 및 페이지 사이즈에 맞게 조회")
    void findGames_success_latestPagination() {
        // given

        GameEntity firstCreatedGame = saveGame(
                "첫 번째 생성 경기",
                "경기장 20",
                "서울특별시 송파구 테스트로 20",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(2)
        );

        GameEntity secondCreatedGame = saveGame(
                "두 번째 생성 경기",
                "경기장 21",
                "서울특별시 송파구 테스트로 21",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(3)
        );

        GameEntity lastCreatedGame = saveGame(
                "마지막 생성 경기",
                "경기장 22",
                "서울특별시 송파구 테스트로 22",
                SEOUL,
                THREE_ON_THREE,
                INDOOR,
                MIXED,
                now.plusDays(4)
        );

        flushAndClear();

        GameListCondition condition = condition(
                null,
                null,
                null,
                null,
                null,
                null,
                LATEST
        );

        // when

        Page<GameEntity> firstPage =
                gameQueryRepository.findGames(
                        condition,
                        PageRequest.of(0, 2),
                        now
                );

        Page<GameEntity> secondPage =
                gameQueryRepository.findGames(
                        condition,
                        PageRequest.of(1, 2),
                        now
                );

        // then

        List<Long> firstPageIds =
                firstPage.getContent()
                        .stream()
                        .map(GameEntity::getGameId)
                        .toList();

        List<Long> secondPageIds =
                secondPage.getContent()
                        .stream()
                        .map(GameEntity::getGameId)
                        .toList();

        assertAll(
                () -> assertEquals(
                        List.of(
                                lastCreatedGame.getGameId(),
                                secondCreatedGame.getGameId()
                        ),
                        firstPageIds
                ),
                () -> assertEquals(
                        List.of(firstCreatedGame.getGameId()),
                        secondPageIds
                ),
                () -> assertEquals(
                        3,
                        firstPage.getTotalElements()
                ),
                () -> assertEquals(
                        2,
                        firstPage.getTotalPages()
                ),
                () -> assertEquals(
                        2,
                        firstPage.getNumberOfElements()
                ),
                () -> assertEquals(
                        1,
                        secondPage.getNumberOfElements()
                )
        );

    }

    private GameEntity saveGame(
            String title,
            String placeName,
            String address,
            CityName cityName,
            MatchFormat matchFormat,
            FieldStatus fieldStatus,
            MatchGenderType matchGenderType,
            LocalDateTime startDateTime
    ) {
        int headCount =
                matchFormat == THREE_ON_THREE
                        ? 6
                        : 10;

        GameEntity game = GameEntity.create(
                title,
                "경기 조회 통합 테스트입니다.",
                headCount,
                fieldStatus,
                matchFormat,
                matchGenderType,
                startDateTime,
                startDateTime.plusHours(2),
                placeName,
                address,
                cityName,
                37.515,
                127.073,
                creator,
                startDateTime.minusDays(2)
        );

        return gameRepository.save(game);
    }

    private GameListCondition condition(
            String keyword,
            CityName cityName,
            MatchFormat matchFormat,
            FieldStatus fieldStatus,
            MatchGenderType matchGenderType,
            com.example.basketballmatching.game.type.GameStatus gameStatus,
            GameSortType sortType
    ) {
        return new GameListCondition(
                null,
                keyword,
                cityName,
                matchFormat,
                fieldStatus,
                matchGenderType,
                gameStatus,
                sortType
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
