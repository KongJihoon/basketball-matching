package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
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
import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@IntegrationTest
@DisplayName("BlackListGameService 통합 테스트")
class BlackListGameServiceIntegrationTest {

    @Autowired
    private BlackListGameService blackListGameService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ParticipantGameRepository participantGameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private UserEntity bannedUser;
    private UserEntity receiver;
    private UserEntity otherCreator;

    private LocalDateTime bannedAt;

    @BeforeEach
    void setUp() {
        bannedUser = saveUser(
                "banned-user@test.com",
                "제재대상",
                "010-1111-1111"
        );

        receiver = saveUser(
                "receiver@test.com",
                "알림대상",
                "010-2222-2222"
        );

        otherCreator = saveUser(
                "other-creator@test.com",
                "다른경기생성자",
                "010-3333-3333"
        );

        bannedAt = LocalDateTime.now(clock)
                .withSecond(0)
                .withNano(0);

        flushAndClear();
    }

    @Test
    @DisplayName("제재 대상자가 생성한 예정 경기와 다른 예정 경기 참가 상태를 정리한다.")
    void cleanUp_success() {
        // given

        GameEntity createdGame = saveFutureGame(
                "제재 대상자가 생성한 경기",
                "제재 대상자 경기장",
                bannedUser,
                bannedAt.plusDays(2)
        );

        ParticipantGameEntity creatorParticipation = participantGameRepository.save(
                ParticipantGameEntity.createCreator(
                        createdGame, bannedUser, bannedAt.minusDays(1)
                )
        );

        ParticipantGameEntity receiverParticipation = participantGameRepository.save(
                ParticipantGameEntity.createParticipation(
                        createdGame, receiver, bannedAt.minusHours(1)
                )
        );

        GameEntity otherGame = saveFutureGame(
                "다른 사용자가 생성한 경기",
                "다른 사용자 경기장",
                otherCreator,
                bannedAt.plusDays(3)
        );

        ParticipantGameEntity otherCreatorParticipation = participantGameRepository.save(
                ParticipantGameEntity.createCreator(
                        otherGame, otherCreator, bannedAt.minusDays(1)
                )
        );

        ParticipantGameEntity bannedParticipation = participantGameRepository.save(
                ParticipantGameEntity.createParticipation(
                        otherGame, bannedUser, bannedAt.minusHours(1)
                )
        );

        Long createdGameId = createdGame.getGameId();
        Long otherGameId = otherGame.getGameId();

        Long creatorParticipationId =
                creatorParticipation.getParticipantGameId();

        Long receiverParticipationId =
                receiverParticipation.getParticipantGameId();

        Long bannedParticipationId =
                bannedParticipation.getParticipantGameId();

        Long otherCreatorParticipationId =
                otherCreatorParticipation.getParticipantGameId();

        flushAndClear();
        // when

        BlackListGameResultDto result = blackListGameService.cleanup(bannedUser.getUserId(), bannedAt);

        flushAndClear();

        // then

        GameEntity canceledCreatedGame = gameRepository.findById(createdGameId)
                .orElseThrow();

        GameEntity remainedOtherGame =
                gameRepository.findById(otherGameId)
                        .orElseThrow();

        ParticipantGameEntity deletedCreatorParticipation =
                participantGameRepository.findById(
                        creatorParticipationId
                ).orElseThrow();

        ParticipantGameEntity deletedReceiverParticipation =
                participantGameRepository.findById(
                        receiverParticipationId
                ).orElseThrow();

        ParticipantGameEntity kickedOutParticipation =
                participantGameRepository.findById(
                        bannedParticipationId
                ).orElseThrow();

        ParticipantGameEntity remainedCreatorParticipation =
                participantGameRepository.findById(
                        otherCreatorParticipationId
                ).orElseThrow();

        assertEquals(bannedAt, canceledCreatedGame.getDeletedDateTime());

        assertEquals(DELETE, deletedCreatorParticipation.getParticipantGameStatus());
        assertEquals(DELETE, deletedReceiverParticipation.getParticipantGameStatus());

        assertEquals(KICKOUT, kickedOutParticipation.getParticipantGameStatus());
        assertEquals(bannedAt, kickedOutParticipation.getKickoutDateTime());

        assertEquals(ACCEPT, remainedCreatorParticipation.getParticipantGameStatus());

        assertEquals(0, canceledCreatedGame.getParticipantCount());
        assertEquals(1, remainedOtherGame.getParticipantCount());

        GameCancelNotificationDto notice = result.notices().get(0);

        assertEquals(receiver.getUserId(), notice.receiverId());
        assertEquals(createdGame.getTitle(), notice.gameTitle());

    }

    @Test
    @DisplayName("이미 종료된 경기와 종료된 경기의 참가 상태는 변경하지 않는다.")
    void cleanUp_success_excludeCompletedGames() {
        // given

        LocalDateTime completedStart =
                bannedAt.minusDays(2);

        GameEntity completedCreatedGame = saveCompletedGame(
                "제재 대상자가 생성했던 종료 경기",
                "종료된 생성 경기장",
                bannedUser,
                completedStart
        );

        ParticipantGameEntity completedCreatorParticipation =
                participantGameRepository.save(
                        ParticipantGameEntity.createCreator(
                                completedCreatedGame,
                                bannedUser,
                                completedStart.minusDays(1)
                        )
                );

        ParticipantGameEntity completedReceiverParticipation =
                participantGameRepository.save(
                        ParticipantGameEntity.createParticipation(
                                completedCreatedGame,
                                receiver,
                                completedStart.minusHours(1)
                        )
                );

        GameEntity otherCompletedGame = saveCompletedGame(
                "다른 사용자의 종료 경기",
                "다른 종료 경기장",
                otherCreator,
                completedStart.minusDays(1)
        );

        participantGameRepository.save(
                ParticipantGameEntity.createCreator(
                        otherCompletedGame,
                        otherCreator,
                        completedStart.minusDays(2)
                )
        );

        ParticipantGameEntity completedBannedParticipation =
                participantGameRepository.save(
                        ParticipantGameEntity.createParticipation(
                                otherCompletedGame,
                                bannedUser,
                                completedStart.minusHours(2)
                        )
                );

        Long completedCreatedGameId =
                completedCreatedGame.getGameId();

        Long completedCreatorParticipationId =
                completedCreatorParticipation
                        .getParticipantGameId();

        Long completedReceiverParticipationId =
                completedReceiverParticipation
                        .getParticipantGameId();

        Long completedBannedParticipationId =
                completedBannedParticipation
                        .getParticipantGameId();

        flushAndClear();

        // when

        BlackListGameResultDto result = blackListGameService.cleanup(bannedUser.getUserId(), bannedAt);

        // then
        GameEntity remainedCompletedGame =
                gameRepository.findById(
                        completedCreatedGameId
                ).orElseThrow();

        ParticipantGameEntity remainedCreator =
                participantGameRepository.findById(
                        completedCreatorParticipationId
                ).orElseThrow();

        ParticipantGameEntity remainedReceiver =
                participantGameRepository.findById(
                        completedReceiverParticipationId
                ).orElseThrow();

        ParticipantGameEntity remainedBannedParticipant =
                participantGameRepository.findById(
                        completedBannedParticipationId
                ).orElseThrow();

        assertNull(remainedCompletedGame.getDeletedDateTime());

        assertEquals(ACCEPT, remainedCreator.getParticipantGameStatus());

        assertEquals(ACCEPT, remainedReceiver.getParticipantGameStatus());

        assertEquals(ACCEPT, remainedBannedParticipant.getParticipantGameStatus());

        assertTrue(result.notices().isEmpty());
    }


    private GameEntity saveFutureGame(
            String title,
            String placeName,
            UserEntity creator,
            LocalDateTime startDateTime
    ) {
        return saveGame(
                title,
                placeName,
                creator,
                startDateTime,
                bannedAt
        );
    }

    private GameEntity saveCompletedGame(
            String title,
            String placeName,
            UserEntity creator,
            LocalDateTime startDateTime
    ) {
        return saveGame(
                title,
                placeName,
                creator,
                startDateTime,
                startDateTime.minusDays(1)
        );
    }

    private GameEntity saveGame(
            String title,
            String placeName,
            UserEntity creator,
            LocalDateTime startDateTime,
            LocalDateTime createdAt
    ) {
        GameEntity game = GameEntity.create(
                title,
                "블랙리스트 경기 정리 통합 테스트입니다.",
                6,
                INDOOR,
                THREE_ON_THREE,
                MIXED,
                startDateTime,
                startDateTime.plusHours(2),
                placeName,
                "서울특별시 송파구 " + placeName,
                SEOUL,
                37.515,
                127.073,
                creator,
                createdAt
        );

        return gameRepository.save(game);
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

        user.setEmailAuth();

        return userRepository.save(user);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }



}