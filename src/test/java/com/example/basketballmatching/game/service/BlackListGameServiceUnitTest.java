package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.type.GameStatus;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlackListGameService 단위 테스트")
class BlackListGameServiceUnitTest {

    private static final Long BANNED_USER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long OTHER_CREATOR_ID = 3L;

    private static final Long CREATED_GAME_ID = 10L;
    private static final Long OTHER_GAME_ID = 20L;

    private static final LocalDateTime BANNED_AT = LocalDateTime.of(2026, 8, 27, 12, 0);

    @Mock
    private GameQueryRepository gameQueryRepository;

    private BlackListGameService blackListGameService;

    private UserEntity bannedUser;

    private UserEntity receiver;

    private UserEntity otherCreator;

    @BeforeEach
    void setUp() {

        blackListGameService = new BlackListGameService(gameQueryRepository);

        bannedUser = createUser(BANNED_USER_ID, "banned@test.com", "제재대상");

        receiver = createUser(RECEIVER_ID, "receiver@test.com", "알림대상");

        otherCreator = createUser(OTHER_CREATOR_ID, "otherCreator@test.com", "다른 경기 생성자");
    }

    @Test
    @DisplayName("제재 대상자가 생성한 예정 경기와 다른 경기 참가 상태를 정리한다.")
    void cleanUp_success() {
        // given

        GameEntity createdGame = createGame(CREATED_GAME_ID, bannedUser, "제재 대상자가 생성한 경기");

        ParticipantGameEntity creatorParticipation = ParticipantGameEntity.createCreator(createdGame, bannedUser, BANNED_AT.minusDays(1));

        ParticipantGameEntity receiverParticipation = ParticipantGameEntity.createParticipation(createdGame, receiver, BANNED_AT.minusHours(1));

        GameEntity otherGame = createGame(OTHER_GAME_ID, otherCreator, "다른 사용자가 생성한 경기");

        ParticipantGameEntity otherCreatorParticipation = ParticipantGameEntity.createCreator(otherGame, otherCreator, BANNED_AT.minusDays(1));

        ParticipantGameEntity bannedParticipation = ParticipantGameEntity.createParticipation(otherGame, bannedUser, BANNED_AT.minusHours(1));

        when(gameQueryRepository.findFutureGamesCreatedBy(BANNED_USER_ID, BANNED_AT))
                .thenReturn(List.of(createdGame));

        when(gameQueryRepository.findActiveParticipantsByGameIds(List.of(CREATED_GAME_ID)))
                .thenReturn(List.of(creatorParticipation, receiverParticipation));

        when(gameQueryRepository.findFutureParticipationExcludingCreatedGames(BANNED_USER_ID, BANNED_AT))
                .thenReturn(List.of(bannedParticipation));

        // when

        BlackListGameResultDto result = blackListGameService.cleanup(BANNED_USER_ID, BANNED_AT);

        // then

        assertEquals(BANNED_AT, createdGame.getDeletedDateTime());

        assertEquals(DELETE, creatorParticipation.getParticipantGameStatus());

        assertEquals(DELETE, receiverParticipation.getParticipantGameStatus());

        assertEquals(KICKOUT, bannedParticipation.getParticipantGameStatus());

        assertEquals(BANNED_AT, bannedParticipation.getKickoutDateTime());

        assertEquals(0, createdGame.getParticipantCount());

        assertEquals(1, otherGame.getParticipantCount());

        assertEquals(ACCEPT, otherCreatorParticipation.getParticipantGameStatus());

        GameCancelNotificationDto notice = result.notices().get(0);

        assertEquals(RECEIVER_ID, notice.receiverId());
        assertEquals(createdGame.getTitle(), notice.gameTitle());

    }

    @Test
    @DisplayName("생성된 예정 경기가 없어도 다른 경기의 참가 상태는 강퇴로 변경")
    void cleanUp_success_onlyOtherParticipation() {
        // given

        GameEntity otherGame = createGame(OTHER_GAME_ID, otherCreator, "다른 사용자가 생성한 경기");

        ParticipantGameEntity otherCreatorParticipation = ParticipantGameEntity.createCreator(otherGame, otherCreator, BANNED_AT.minusDays(1));

        ParticipantGameEntity bannedParticipation = ParticipantGameEntity.createParticipation(otherGame, bannedUser, BANNED_AT.minusHours(1));

        when(gameQueryRepository.findFutureGamesCreatedBy(BANNED_USER_ID, BANNED_AT))
                .thenReturn(List.of());

        when(gameQueryRepository.findFutureParticipationExcludingCreatedGames(BANNED_USER_ID, BANNED_AT))
                .thenReturn(List.of(bannedParticipation));
        // when

        BlackListGameResultDto result = blackListGameService.cleanup(BANNED_USER_ID, BANNED_AT);

        // then
        assertTrue(result.notices().isEmpty());

        assertEquals(KICKOUT, bannedParticipation.getParticipantGameStatus());
        assertEquals(BANNED_AT, bannedParticipation.getKickoutDateTime());

        assertEquals(1, otherGame.getParticipantCount());

        assertEquals(ACCEPT, otherCreatorParticipation.getParticipantGameStatus());

        verify(gameQueryRepository, never()).findActiveParticipantsByGameIds(anyList());


    }

    @Test
    @DisplayName("생성한 예정 경기의 활성 참가자가 없어도 경기는 취소한다")
    void cleanup_success_createdGameWithoutParticipants() {
        // given
        GameEntity createdGame =
                createGame(
                        CREATED_GAME_ID,
                        bannedUser,
                        "참가자가 없는 예정 경기"
                );

        when(gameQueryRepository
                .findFutureGamesCreatedBy(
                        BANNED_USER_ID,
                        BANNED_AT
                ))
                .thenReturn(List.of(createdGame));

        when(gameQueryRepository
                .findActiveParticipantsByGameIds(
                        List.of(CREATED_GAME_ID)
                ))
                .thenReturn(List.of());

        when(gameQueryRepository
                .findFutureParticipationExcludingCreatedGames(
                        BANNED_USER_ID,
                        BANNED_AT
                ))
                .thenReturn(List.of());

        // when
        BlackListGameResultDto result =
                blackListGameService.cleanup(
                        BANNED_USER_ID,
                        BANNED_AT
                );

        // then
        assertEquals(
                BANNED_AT,
                createdGame.getDeletedDateTime()
        );

        assertTrue(result.notices().isEmpty());
    }

    private GameEntity createGame(Long gameId, UserEntity creator, String title) {
        LocalDateTime startDateTime =
                BANNED_AT.plusDays(2);

        GameEntity game = GameEntity.create(
                title,
                "블랙리스트 경기 정리 테스트입니다.",
                6,
                INDOOR,
                THREE_ON_THREE,
                MIXED,
                startDateTime,
                startDateTime.plusHours(2),
                "테스트 농구장",
                "서울특별시 송파구",
                SEOUL,
                37.5,
                127.0,
                creator,
                BANNED_AT.minusDays(1)
        );

        ReflectionTestUtils.setField(
                game,
                "gameId",
                gameId
        );

        return game;
    }

    private UserEntity createUser(Long userId, String email, String nickname) {
        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name(nickname)
                .birth(LocalDate.of(1997, 1, 1))
                .phone("010-1234-5678")
                .address("서울특별시")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }

}