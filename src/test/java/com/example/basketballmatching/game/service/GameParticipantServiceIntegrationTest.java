package com.example.basketballmatching.game.service;

import com.example.basketballmatching.blackList.repository.BlackListRepository;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.dto.response.GameParticipantResponse;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@IntegrationTest
@DisplayName("GameParticipantService 통합 테스트")
class GameParticipantServiceIntegrationTest {

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

    private Long creatorId;
    private Long participantId;
    private Long gameId;


    @BeforeEach
    void setUp() {

        UserEntity creator = saveUser("creator@test.com", "경기생성자", "010-1111-1111");

        UserEntity participant = saveUser("participant@test.com", "경기 참가자", "010-2222-2222");

        creatorId = creator.getUserId();
        participantId = participant.getUserId();

        LocalDateTime startDateTime = LocalDateTime.now(clock).plusDays(2).withSecond(0).withNano(0);

        CreateGameRequest request =
                new CreateGameRequest(
                        "잠실 참가 테스트 경기",
                        "경기 참가 통합 테스트입니다.",
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

        CreateGameResponse response = gameService.createGame(creatorId, request);

        gameId = response.gameId();

        flushAndClear();
    }

    @Test
    @DisplayName("경기 참가 시 참가 정보 저장 후 경기 인원 증가")
    void join_success() {
        // given


        // when

        GameParticipantResponse response = gameParticipantService.join(gameId, participantId);

        flushAndClear();

        // then

        ParticipantGameEntity participation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, participantId)
                .orElseThrow();

        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow();

        assertNotNull(response.participationId());
        assertEquals(ACCEPT, participation.getParticipantGameStatus());
        assertEquals(ACCEPT, response.status());
        assertNotNull(participation.getAcceptDateTime());
        assertEquals(2, game.getParticipantCount());

    }

    @Test
    @DisplayName("경기 참가 취소 시 상태 변경 후 인원 감소")
    void cancelParticipation_success() {
        // given

        GameParticipantResponse joined = gameParticipantService.join(gameId, participantId);

        flushAndClear();

        // when

        gameParticipantService.cancelParticipation(gameId, participantId);

        flushAndClear();
        // then

        ParticipantGameEntity participation = participantGameRepository.findById(joined.participationId())
                .orElseThrow();

        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow();

        assertEquals(CANCEL, participation.getParticipantGameStatus());

        assertNotNull(participation.getCanceledDateTime());

        assertEquals(1, game.getParticipantCount());

    }

    @Test
    @DisplayName("경기 참가 취소 후 재참가하면 참가 정보를 재사용")
    void rejoin_success() {
        // given

        GameParticipantResponse firstJoin = gameParticipantService.join(gameId, participantId);

        gameParticipantService.cancelParticipation(gameId, participantId);

        flushAndClear();

        // when

        GameParticipantResponse rejoined = gameParticipantService.join(gameId, participantId);

        flushAndClear();

        // then

        ParticipantGameEntity participation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, participantId)
                .orElseThrow();

        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow();


        assertEquals(firstJoin.participationId(), rejoined.participationId());

        assertEquals(ACCEPT, participation.getParticipantGameStatus());

        assertNull(participation.getCanceledDateTime());

        assertEquals(2, game.getParticipantCount());

        assertEquals(2, participantGameRepository.count());

    }

    @Test
    @DisplayName("경기 생성자가 참가자 강퇴 시 상태 변경 및 인원 감소")
    void kickoutParticipant_success() {
        // given

        GameParticipantResponse joined = gameParticipantService.join(gameId, participantId);

        flushAndClear();



        // when

        gameParticipantService.kickoutParticipant(gameId, joined.participationId(), creatorId);

        flushAndClear();
        // then

        ParticipantGameEntity participation = participantGameRepository.findById(joined.participationId())
                .orElseThrow();

        GameEntity game = gameRepository.findById(gameId)
                .orElseThrow();

        assertEquals(KICKOUT, participation.getParticipantGameStatus());

        assertNotNull(participation.getKickoutDateTime());
        assertEquals(1, game.getParticipantCount());

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

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }


}