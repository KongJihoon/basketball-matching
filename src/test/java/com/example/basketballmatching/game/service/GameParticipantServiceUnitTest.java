package com.example.basketballmatching.game.service;

import com.example.basketballmatching.blacklist.service.BlackListStore;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.response.GameParticipantResponse;
import com.example.basketballmatching.game.event.GameParticipantKickedOutEvent;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.NOT_CANCEL_GAME_CREATOR;
import static com.example.basketballmatching.global.exception.ErrorCode.NOT_GAME_CREATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameParticipantServiceUnitTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long PARTICIPANT_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;

    private static final Long GAME_ID = 1L;
    private static final Long PARTICIPATION_ID = 10L;

    private static final LocalDateTime NOW =
            LocalDateTime.of(
                    2026,
                    8,
                    18,
                    12,
                    0
            );

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Seoul");

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private BlackListStore blackListStore;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GameParticipantService gameParticipantService;

    private ParticipantGameEntity creatorParticipation;

    private UserEntity creator;
    private UserEntity participant;
    private GameEntity game;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(
                NOW.atZone(ZONE_ID).toInstant(), ZONE_ID);

        gameParticipantService = new GameParticipantService(
                userRepository, gameRepository, participantGameRepository, blackListStore, eventPublisher, clock
        );

        creator = createUser(CREATOR_ID, "creator@test.com", "경기생성자");


        participant = createUser(
                PARTICIPANT_ID,
                "participant@test.com",
                "참가자"
        );

        game = createGame(creator);

        creatorParticipation = ParticipantGameEntity.createCreator(
                game,
                creator,
                NOW.minusHours(1)
        );
    }

    @Test
    @DisplayName("경기 참가 성공 시 승인 상태 저장 후 참가 인원 증가")
    void join_success() {
        // given

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(PARTICIPANT_ID))
                .thenReturn(Optional.of(participant));

        when(blackListStore.isBlacklisted("participant@test.com"))
                .thenReturn(false);

        when(gameRepository.findActiveGameWithPessimisticLock(GAME_ID))
                .thenReturn(Optional.of(game));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(GAME_ID, PARTICIPANT_ID))
                .thenReturn(Optional.empty());

        when(participantGameRepository.save(any(ParticipantGameEntity.class)))
                .thenAnswer(invocation -> {
                    ParticipantGameEntity participation = invocation.getArgument(0);

                    ReflectionTestUtils.setField(
                            participation, "participantGameId", PARTICIPATION_ID
                    );

                    return participation;
                });


        // when

        GameParticipantResponse response = gameParticipantService.join(GAME_ID, PARTICIPANT_ID);

        // then

        ArgumentCaptor<ParticipantGameEntity> captor = ArgumentCaptor.forClass(ParticipantGameEntity.class);

        verify(participantGameRepository).save(captor.capture());

        ParticipantGameEntity savedParticipation = captor.getValue();

        assertEquals(PARTICIPATION_ID, response.participationId());

        assertEquals(GAME_ID, response.gameId());

        assertEquals(ACCEPT, response.status());

        assertEquals(NOW, response.joinedAt());

        assertEquals(ACCEPT, savedParticipation.getParticipantGameStatus());

        assertEquals(2, game.getParticipantCount());

    }

    @Test
    @DisplayName("참가 취소 후 재참가 가능")
    void join_success_rejoinCanceledParticipation() {
        // given

        ParticipantGameEntity existingParticipation = ParticipantGameEntity.createParticipation(game, participant, NOW.minusHours(2));

        existingParticipation.cancelParticipation(NOW.minusHours(1));

        ReflectionTestUtils.setField(
                existingParticipation,
                "participantGameId",
                PARTICIPATION_ID
        );

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(PARTICIPANT_ID))
                .thenReturn(Optional.of(participant));

        when(blackListStore.isBlacklisted("participant@test.com"))
                .thenReturn(false);
        when(
                gameRepository
                        .findActiveGameWithPessimisticLock(
                                GAME_ID
                        )
        ).thenReturn(Optional.of(game));

        when(
                participantGameRepository
                        .findByGameEntity_GameIdAndUserEntity_UserId(
                                GAME_ID,
                                PARTICIPANT_ID
                        )
        ).thenReturn(
                Optional.of(existingParticipation)
        );
        // when

        GameParticipantResponse response = gameParticipantService.join(GAME_ID, PARTICIPANT_ID);

        // then

        assertEquals(ACCEPT, response.status());

        assertEquals(NOW, response.joinedAt());

        assertEquals(ACCEPT, existingParticipation.getParticipantGameStatus());

        assertEquals(2, game.getParticipantCount());

        verify(participantGameRepository, never()).save(any(ParticipantGameEntity.class));

    }

    @Test
    @DisplayName("참가자 참가 취소 시 상태 변경 후 인원감소")
    void cancelParticipation_success() {
        // given

        ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(game, participant, NOW.minusHours(1));

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(PARTICIPANT_ID))
                .thenReturn(Optional.of(participant));

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(game));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(GAME_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(participation));



        // when

        gameParticipantService.cancelParticipation(GAME_ID, PARTICIPANT_ID);

        // then

        assertEquals(CANCEL, participation.getParticipantGameStatus());

        assertEquals(NOW, participation.getCanceledDateTime());

        assertEquals(1, game.getParticipantCount());

    }

    @Test
    @DisplayName("경기 생성자는 자신의 경기를 취소할 수 없다.")
    void cancelParticipation_fail_creator() {
        // given


        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(CREATOR_ID))
                .thenReturn(Optional.of(creator));

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(game));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(GAME_ID, CREATOR_ID))
                .thenReturn(Optional.of(creatorParticipation));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameParticipantService.cancelParticipation(GAME_ID, CREATOR_ID));

        // then

        assertEquals(NOT_CANCEL_GAME_CREATOR, exception.getErrorCode());

        assertEquals(ACCEPT, creatorParticipation.getParticipantGameStatus());

        assertEquals(1, game.getParticipantCount());

    }

    @Test
    @DisplayName("경기 생성자는 경기 참가자를 강퇴할 수 있다. 강퇴 시 인원 감소 후 이벤트 발행")
    void kickoutParticipant_success() {
        // given

        ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(
                game, participant, NOW.minusHours(1)
        );

        ReflectionTestUtils.setField(participation, "participantGameId", PARTICIPATION_ID);

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(CREATOR_ID))
                .thenReturn(Optional.of(creator));

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(game));

        when(participantGameRepository.findByParticipantGameIdAndGameEntity_GameId(PARTICIPATION_ID, GAME_ID))
                .thenReturn(Optional.of(participation));

        // when

        gameParticipantService.kickoutParticipant(GAME_ID, PARTICIPATION_ID, CREATOR_ID);

        // then

        ArgumentCaptor<GameParticipantKickedOutEvent> eventCaptor = ArgumentCaptor.forClass(GameParticipantKickedOutEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        GameParticipantKickedOutEvent event = eventCaptor.getValue();

        assertEquals(KICKOUT, participation.getParticipantGameStatus());

        assertEquals(NOW, participation.getKickoutDateTime());

        assertEquals(1, game.getParticipantCount());

        assertEquals(GAME_ID, event.gameId());

        assertEquals(game.getTitle(), event.gameTitle());

        assertEquals(PARTICIPANT_ID, event.participantUserId());

    }

    @Test
    @DisplayName("경기 생성자가 아닌 참가자는 강퇴 불가능")
    void kickoutParticipant_fail_notCreator() {
        // given

        UserEntity requestor = createUser(OTHER_USER_ID, "other@test.com", "다른 사용자");

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(OTHER_USER_ID))
                .thenReturn(Optional.of(requestor));

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(game));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameParticipantService.kickoutParticipant(GAME_ID, PARTICIPATION_ID, OTHER_USER_ID));

        // then

        assertEquals(NOT_GAME_CREATOR, exception.getErrorCode());

        verify(participantGameRepository, never()).findByParticipantGameIdAndGameEntity_GameId(PARTICIPATION_ID, GAME_ID);

        verify(eventPublisher, never()).publishEvent(any());

    }




    private GameEntity createGame(
            UserEntity creator
    ) {
        LocalDateTime startDateTime =
                NOW.plusDays(2);

        GameEntity createdGame =
                GameEntity.create(
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
                        creator,
                        NOW
                );

        ReflectionTestUtils.setField(
                createdGame,
                "gameId",
                GAME_ID
        );

        return createdGame;
    }

    private UserEntity createUser(
            Long userId,
            String email,
            String nickname
    ) {
        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name("testUser")
                .birth(LocalDate.of(1997, 1, 1))
                .phone("010-1234-5678")
                .address("test address")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }
}
