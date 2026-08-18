package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.request.UpdateGameRequest;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.dto.response.GameDetailResponse;
import com.example.basketballmatching.game.event.GameCreateEvent;
import com.example.basketballmatching.game.event.GameDeletedEvent;
import com.example.basketballmatching.game.event.UpdateGameEvent;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceUnitTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long PARTICIPANT_ID = 2L;
    private static final Long GAME_ID = 1L;

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 17, 12, 0);

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Seoul");
    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GameQueryRepository gameQueryRepository;

    private GameService gameService;

    private UserEntity creator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.from(NOW.atZone(ZONE_ID)), ZONE_ID);

        gameService = new GameService(
                userRepository, gameRepository, participantGameRepository, eventPublisher, clock, gameQueryRepository
        );

        creator = createUser(CREATOR_ID, "test@test.com", "testUser");
    }

    @Test
    @DisplayName("경기 생성 시 경기와 참가자 정보 저장 후 이벤트 발행")
    void createGame_success() {
        // given

        CreateGameRequest request = createGameRequest();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(CREATOR_ID))
                .thenReturn(Optional.of(creator));

        when(gameRepository.existsBySamePlaceAtSameTime(
                request.placeName(), request.address(), request.startDateTime(), request.endDateTime()
        )).thenReturn(false);

        when(gameRepository.save(any(GameEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(participantGameRepository.save(any(ParticipantGameEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // when

        CreateGameResponse response = gameService.createGame(CREATOR_ID, request);


        // then

        ArgumentCaptor<GameEntity> gameCaptor = ArgumentCaptor.forClass(GameEntity.class);

        ArgumentCaptor<ParticipantGameEntity> participationCaptor = ArgumentCaptor.forClass(ParticipantGameEntity.class);

        ArgumentCaptor<GameCreateEvent> eventCaptor = ArgumentCaptor.forClass(GameCreateEvent.class);

        verify(gameRepository).save(gameCaptor.capture());

        verify(participantGameRepository).save(participationCaptor.capture());

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        GameEntity savedGame = gameCaptor.getValue();

        ParticipantGameEntity savedParticipation = participationCaptor.getValue();

        GameCreateEvent event = eventCaptor.getValue();

        assertEquals(request.title(), savedGame.getTitle());

        assertEquals(SEOUL, savedGame.getCityName());

        assertEquals(1, response.participantCount());

        assertEquals(savedGame, savedParticipation.getGameEntity());

        assertEquals(ParticipantGameStatus.ACCEPT, savedParticipation.getParticipantGameStatus());

        assertEquals(CREATOR_ID, event.creatorId());

        assertEquals(request.title(), event.title());



    }

    @Test
    @DisplayName("동일 장소와 시간이 겹치면 경기 생성 불가")
    void createGame_fail_scheduleOverlap() {
        // given

        CreateGameRequest request = createGameRequest();


        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(CREATOR_ID))
                .thenReturn(Optional.of(creator));

        when(gameRepository.existsBySamePlaceAtSameTime(
                request.placeName(), request.address(), request.startDateTime(), request.endDateTime()
        )).thenReturn(true);
        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameService.createGame(CREATOR_ID, request));

        // then

        assertEquals(PLACE_SCHEDULE_OVERLAP, exception.getErrorCode());

        verify(gameRepository, never()).save(any(GameEntity.class));

        verify(participantGameRepository, never()).save(any(ParticipantGameEntity.class));

        verify(eventPublisher, never()).publishEvent(any());

    }

    @Test
    @DisplayName("경기 수정 성공 시 경기를 변경하고 참가자 알림 이벤트 발행")
    void updateGame_success() {
        // given

        GameEntity game = createGame(creator);

        UserEntity participant = createUser(PARTICIPANT_ID, "participant@test.com", "참가자");

        ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(game, participant, NOW);

        UpdateGameRequest request =
                new UpdateGameRequest(
                        "수정된 경기 제목",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(game));

        when(participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(List.of(ParticipantGameStatus.ACCEPT), game.getGameId()))
                .thenReturn(List.of(participation));

        // when

        GameDetailResponse response = gameService.updateGame(request, GAME_ID, CREATOR_ID);

        // then

        ArgumentCaptor<UpdateGameEvent> eventCaptor = ArgumentCaptor.forClass(UpdateGameEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());


        UpdateGameEvent event = eventCaptor.getValue();

        assertEquals("수정된 경기 제목", response.title());
        assertEquals("수정된 경기 제목", game.getTitle());
        assertEquals("수정된 경기 제목", event.title());

        assertEquals(List.of(PARTICIPANT_ID), event.receiverIds());



    }


    @Test
    @DisplayName("수정할 필드가 없으면 경기 수정 불가")
    void updateGame_fail_noUpdateFields() {
        // given
        GameEntity game =
                createGame(creator);

        UpdateGameRequest request =
                new UpdateGameRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(
                gameRepository
                        .findByGameIdAndDeletedDateTimeIsNull(
                                GAME_ID
                        )
        ).thenReturn(Optional.of(game));

        // when
        CustomException exception =
                assertThrows(
                        CustomException.class,
                        () -> gameService.updateGame(
                                request,
                                GAME_ID,
                                CREATOR_ID
                        )
                );

        // then
        assertEquals(
                NO_GAME_UPDATE_FIELDS,
                exception.getErrorCode()
        );

        verify(eventPublisher, never())
                .publishEvent(any());
    }

    @Test
    @DisplayName("경기 삭제 시 참가 상태 변경 후 이벤트 발행")
    void deleteGame_success() {
        // given

        GameEntity game = createGame(creator);

        UserEntity participant = createUser(PARTICIPANT_ID, "participant@test.com", "participant");

        ParticipantGameEntity creatorParticipation = ParticipantGameEntity.createCreator(game, creator, NOW);

        ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(game, participant, NOW);

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(CREATOR_ID))
                .thenReturn(Optional.of(creator));

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(game));

        when(participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(List.of(ParticipantGameStatus.ACCEPT), GAME_ID))
                .thenReturn(List.of(creatorParticipation, participation));

        // when

        gameService.deleteGame(GAME_ID, CREATOR_ID);

        // then

        ArgumentCaptor<GameDeletedEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        GameDeletedEvent.class
                );

        verify(eventPublisher)
                .publishEvent(eventCaptor.capture());

        GameDeletedEvent event =
                eventCaptor.getValue();

        assertEquals(NOW, game.getDeletedDateTime());


        assertEquals(ParticipantGameStatus.DELETE, participation.getParticipantGameStatus());

        assertEquals(0, game.getParticipantCount());

        assertEquals(List.of(PARTICIPANT_ID), event.receiverIds());

    }

    private CreateGameRequest createGameRequest() {

        LocalDateTime startDateTime = NOW.plusDays(2);

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

    private GameEntity createGame(UserEntity user) {
        LocalDateTime startDateTime =
                NOW.plusDays(2);

        return GameEntity.create(
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
                SEOUL,
                37.515,
                127.073,
                user,
                NOW
        );
    }

    private UserEntity createUser(
            Long userId, String email, String nickname
    ) {

        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name("testUser")
                .birth(LocalDate.of(1997,01,01))
                .phone("010-1234-5678")
                .address("test address")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }

}