package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.request.UpdateGameRequest;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.dto.response.GameDetailResponse;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.DELETE;
import static com.example.basketballmatching.global.exception.ErrorCode.PLACE_SCHEDULE_OVERLAP;
import static org.junit.jupiter.api.Assertions.*;
@IntegrationTest
@Transactional
@DisplayName("GameService 통합 테스트")
class GameServiceIntegrationTest {

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
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private UserEntity creator;
    private LocalDateTime startDateTime;

    @BeforeEach
    void setUp() {

        creator = saveUser(
                "creator@test.com", "경기 생성자", "010-1111-1111"
        );

        startDateTime = LocalDateTime.now(clock).plusDays(2).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("경기 생성 시 경기와 생성자 참가 정보가 함께 저장")
    void createGame_success() {
        // given

        CreateGameRequest request = createGameRequest();



        // when
        CreateGameResponse response = gameService.createGame(creator.getUserId(), request);

        flushAndClear();


        // then

        GameEntity savedGame = gameRepository.findByGameIdAndDeletedDateTimeIsNull(response.gameId())
                .orElseThrow();

        ParticipantGameEntity creatorParticipation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(response.gameId(), creator.getUserId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(response.gameId()),
                () -> assertEquals(request.title(), savedGame.getTitle()),
                () -> assertEquals(SEOUL, savedGame.getCityName()),
                () -> assertEquals(1, savedGame.getParticipantCount()),
                () -> assertEquals(ACCEPT, creatorParticipation.getParticipantGameStatus()),
                () -> assertEquals(creator.getUserId(), creatorParticipation.getUserEntity().getUserId())
        );

    }

    @Test
    @DisplayName("동일 장소에 경기 시간이 겹치면 예외 발생")
    void createGame_fail_scheduleOverlap() {
        // given

        CreateGameRequest request = createGameRequest();

        CreateGameResponse response = gameService.createGame(creator.getUserId(), request);


        flushAndClear();

        long gameCountBefore = gameRepository.count();
        long participationCountBefore = participantGameRepository.count();

        CreateGameRequest overlapRequest =
                new CreateGameRequest(
                        "시간이 겹치는 경기",
                        "동일 장소의 시간이 겹치는 경기입니다.",
                        6,
                        INDOOR,
                        THREE_ON_THREE,
                        MIXED,
                        request.startDateTime().plusMinutes(30),
                        request.endDateTime().plusMinutes(30),
                        request.placeName(),
                        request.address(),
                        request.latitude(),
                        request.longitude()
                );

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameService.createGame(creator.getUserId(), overlapRequest));

        // then

        assertAll(
                () -> assertEquals(PLACE_SCHEDULE_OVERLAP, exception.getErrorCode()),
                () -> assertEquals(gameCountBefore, gameRepository.count()),
                () -> assertEquals(participationCountBefore, participantGameRepository.count())
        );

    }

    @Test
    @DisplayName("경기 수정 성공")
    void updateGame_success() {
        // given

        CreateGameResponse createGame = gameService.createGame(creator.getUserId(), createGameRequest());

        flushAndClear();

        UpdateGameRequest request =
                new UpdateGameRequest(
                        "수정된 경기 제목",
                        "수정된 경기 내용",
                        null,
                        null,
                        null,
                        null,
                        null
                );
        // when

        GameDetailResponse response = gameService.updateGame(request, createGame.gameId(), creator.getUserId());

        flushAndClear();
        // then

        GameEntity updateGame = gameRepository.findByGameIdAndDeletedDateTimeIsNull(createGame.gameId())
                .orElseThrow();

        assertEquals("수정된 경기 제목", updateGame.getTitle());
        assertEquals("수정된 경기 제목", response.title());
        assertEquals("수정된 경기 내용", updateGame.getContent());



    }

    @Test
    @DisplayName("경기 삭제 시 경기와 모든 참가 정보가 삭제 상태로 변경")
    void deleteGame_success() {
        // given

        UserEntity participant = saveUser("participant@test.com", "경기 참가자", "010-2222-2222");

        CreateGameResponse createdGame = gameService.createGame(creator.getUserId(), createGameRequest());

        gameParticipantService.join(createdGame.gameId(), participant.getUserId());

        flushAndClear();

        // when

        gameService.deleteGame(createdGame.gameId(), creator.getUserId());

        flushAndClear();

        // then

        GameEntity deletedGame = gameRepository.findById(createdGame.gameId())
                .orElseThrow();

        ParticipantGameEntity creatorParticipation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(createdGame.gameId(), creator.getUserId())
                .orElseThrow();

        ParticipantGameEntity participantParticipation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(createdGame.gameId(), participant.getUserId())
                .orElseThrow();

        assertAll(
                () -> assertNotNull(deletedGame.getDeletedDateTime()),
                () -> assertEquals(0, deletedGame.getParticipantCount()),
                () -> assertTrue(gameRepository.findByGameIdAndDeletedDateTimeIsNull(createdGame.gameId()).isEmpty()),
                () -> assertEquals(DELETE, creatorParticipation.getParticipantGameStatus()),
                () -> assertEquals(DELETE, participantParticipation.getParticipantGameStatus()),
                () -> assertNotNull(participantParticipation.getDeletedDateTime())
        );
    }

    private CreateGameRequest createGameRequest() {
        return new CreateGameRequest(
                "잠실 주말 농구",
                "즐겁게 농구하실 분을 모집합니다.",
                6,
                INDOOR,
                THREE_ON_THREE,
                MIXED,
                startDateTime,
                startDateTime.plusHours(2),
                "잠실종합운동장 농구장",
                "서울특별시 송파구 올림픽로 25",
                37.515,
                127.073
        );
    }

    private UserEntity saveUser(
            String email,
            String nickname,
            String phone
    ) {
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

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}