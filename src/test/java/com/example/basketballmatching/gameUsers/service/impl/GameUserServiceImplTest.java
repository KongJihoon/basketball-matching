package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.ParticipantGameService;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.EvaluatePlayerDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.gameUsers.entity.LevelEntity;
import com.example.basketballmatching.gameUsers.repository.LevelRepository;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GameUserServiceImplTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameUserService gameUserService;

    @Autowired
    private ParticipantGameRepository participantGameRepository;


    UserEntity creator;

    UserEntity participant;

    GameEntity gameEntity;

    ParticipantGameEntity participantGameEntity;
    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private GameQueryRepository gameQueryRepository;
    @Autowired
    private ParticipantGameService participantGameService;

    @BeforeEach
    void setUp() {
        creator = UserEntity.builder()
                .email("creator@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("name")
                .nickname("name")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소")
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        creator.setEmailAuth();

        userRepository.save(creator);

        participant = UserEntity.builder()
                .email("participant@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("participant")
                .nickname("participant")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소")
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        participant.setEmailAuth();

        userRepository.save(participant);


        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(9)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.MALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(2L))
                .endDateTime(LocalDateTime.now().plusHours(3L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        gameEntity = CreateGameDto.Request.toEntity(request, creator);

        gameRepository.save(gameEntity);
        participantGameEntity = new ParticipantGameEntity().toGameCreatorEntity(gameEntity, creator);

        participantGameRepository.save(participantGameEntity);
    }

    @Test
    @DisplayName("경기 신청 테스트")
    void applyGameTest() {
        // given

        Long userId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        // when

        CommonResponse<ApplyGameUserDto> applyGame = gameUserService.applyGame(gameId, userId);


        // then
        assertEquals("경기 신청이 완료되었습니다.", applyGame.getMessage());

        assertEquals(userId, applyGame.getData().getUserId());


    }

    @Test
    @DisplayName("경기 신청 실패 - 남성만 참여가능 -> 여성 참가자가 신청 시")
    void applyGameTest_Fail_FEMALE_ONLY() {
        // given
        UserEntity gameUser = UserEntity.builder()
                .email("test4@example.com")
                .password(passwordEncoder.encode("Test@1234!"))
                .name("name3")
                .nickname("name3")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소23")
                .phone("010-1111-1112")
                .position(Position.GUARD)
                // 남성 유저
                .genderType(GenderType.FEMALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        userRepository.save(gameUser);

        Long gameId = gameEntity.getGameId();


        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.applyGame(gameId, gameUser.getUserId()));
        // then

        assertEquals(ONLY_MALE_GAME, exception.getErrorCode());


    }

    @Test
    @DisplayName("경기 신청 실패 테스트 - 경기 인원 초과")
    void applyGameTest_Fail_Full_HeadCount() {
        // given

        Long gameId = gameEntity.getGameId();

        int headCount = gameEntity.getHeadCount();

        for (int i = 0; i < headCount - 1; i++) {
            UserEntity extraUser = UserEntity.builder()
                    .email("extra" + i + "@example.com")
                    .password(passwordEncoder.encode("Test@1234!"))
                    .name("참가자" + i)
                    .nickname("참가자" + i)
                    .birth(LocalDate.of(1998, 1, 1))
                    .address("인천시 테스트주소" + i)
                    .phone("010-9999-99" + i)
                    .position(Position.GUARD)
                    .genderType(GenderType.MALE)
                    .loginProvider(LoginProvider.LOCAL)
                    .userType(UserType.USER)
                    .build();

            userRepository.save(extraUser);

            // 이미 참가된 상태를 DB에 반영 (status = APPLY)
            gameUserService.applyGame(gameId, extraUser.getUserId());
        }

        UserEntity user = UserEntity.builder()
                .email("overUser@example.com")
                .password(passwordEncoder.encode("Test@1234!"))
                .name("초과유저")
                .nickname("overUser")
                .birth(LocalDate.of(1999, 1, 1))
                .address("인천시 초과주소")
                .phone("010-8888-8888")
                .position(Position.GUARD)
                .genderType(GenderType.FEMALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();
        userRepository.save(user);
        // when

        CustomException exception = assertThrows(CustomException.class, () ->
                gameUserService.applyGame(gameEntity.getGameId(), user.getUserId()));

        // then

        assertEquals(ErrorCode.FULL_HEADCOUNT_GAME, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 참가 취소 테스트")
    void CancelGameTest() {
        // given

        Long userId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameEntity.getGameId(), userId);

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        // when

        CheckResponse checkResponse = gameUserService.cancelGame(userId, gameId);

        // then

        assertEquals(ParticipantGameStatus.CANCEL, participantGameEntity.getParticipantGameStatus());
        assertEquals("경기 취소가 완료되었습니다.", checkResponse.getMessage());

    }

    @Test
    @DisplayName("경기 참가 취소 실패 테스트 - 수락되지 않은 유저 접근")
    void cancelGameFailTest_Apply_User() {
        // given

        Long userId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameEntity.getGameId(), userId);

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.cancelGame(userId, gameId));

        // then

        assertEquals(NOT_ACCEPT_USER, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 참가 취소 실패테스트 - 경기시작 30분전 취소 불가")
    void cancelGameFailTest_NOT_ALLOWED_CANCEL() {
        // given
        Long userId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, userId);

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        gameEntity.setStartDateTime(LocalDateTime.now().minusMinutes(40));
        gameEntity.setEndDateTime(LocalDateTime.now().minusMinutes(40));


        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.cancelGame(userId, gameId));

        // then

        assertEquals(NOT_ALLOWED_CANCEL, exception.getErrorCode());

    }

    @Test
    @DisplayName("현재 예정 경기 조회 테스트")
    void getCurrentGameListTest() {
        // given

        Long userId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, userId);

        // when
        CommonResponse<List<CurrentGameListDto>> myCurrentGameList = gameUserService.getMyCurrentGameList(userId, PageRequest.of(0, 10));

        // then

        assertEquals(gameEntity.getGameId(), myCurrentGameList.getData().get(0).getGameId());
        assertEquals("현재 예정된 게임 조회가 완료되었습니다.", myCurrentGameList.getMessage());

    }

    @Test
    @DisplayName("현재 예정 경기 조회 테스트 - N+1 문제 확인 테스트")
    void getCurrentGameListTest_Check() {
        // given


        List<Long> gameIds = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            CreateGameDto.Request request = CreateGameDto.Request.builder()
                    .title("테스트 게임")
                    .content("테스트 게임 본문")
                    .headCount(9)
                    .fieldStatus(FieldStatus.OUTDOOR)
                    .matchGenderType(MatchGenderType.MALE_ONLY)
                    .startDateTime(LocalDateTime.now().plusHours(4L + i))
                    .endDateTime(LocalDateTime.now().plusHours(5L + i))
                    .placeName("테스트 게임 장소")
                    .address("인천광역시 테스트 게임 주소")
                    .matchFormat(MatchFormat.THREE_ON_THREE)
                    .build();

            gameEntity = CreateGameDto.Request.toEntity(request, creator);

            gameRepository.save(gameEntity);
            participantGameEntity = new ParticipantGameEntity().toGameCreatorEntity(gameEntity, creator);

            gameIds.add(gameEntity.getGameId());
        }

        for (int i = 0; i < gameIds.size(); i++) {

            gameUserService.applyGame(gameIds.get(i), participant.getUserId());

        }

        // when

        CommonResponse<List<CurrentGameListDto>> commonResponse = gameUserService.getMyCurrentGameList(participant.getUserId(), PageRequest.of(0, 10));


        // then

        assertEquals("현재 예정된 게임 조회가 완료되었습니다.", commonResponse.getMessage());
        assertEquals(10, commonResponse.getData().size());

    }

    @Test
    @DisplayName("지난 경기 조회 테스트")
    void getLastGameListTest() {
        // given
        Long userId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, userId);

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        gameEntity.setStartDateTime(LocalDateTime.now().minusHours(2L));
        gameEntity.setEndDateTime(LocalDateTime.now().minusHours(2L));

        gameRepository.save(gameEntity);
        // when

        CommonResponse<List<LastGameListDto>> myLastGameList = gameUserService.getMyLastGameList(userId, PageRequest.of(0, 10));

        // then

        assertEquals(gameId, myLastGameList.getData().get(0).getGameId());
        assertEquals("지난 게임 조회가 완료되었습니다.", myLastGameList.getMessage());

    }


    @Test
    @DisplayName("경기 참가 신청 - 동시성 이슈 테스트")
    void applyGameTest_Concurrency_Issue() throws Exception {
        // given

        Long gameId = gameEntity.getGameId();

        for (int i = 0; i < 4; i++) {
            UserEntity gameUser = UserEntity.builder()
                    .email("testa@"+ i + "example.com")
                    .password(passwordEncoder.encode("Test@1234"))
                    .name("namea" + i)
                    .nickname("namea" + i)
                    .birth(LocalDate.of(1997,7,24))
                    .address("테스트용주소a" + i)
                    .phone("010-1111-1112")
                    .position(Position.GUARD)
                    .genderType(GenderType.MALE)
                    .loginProvider(LoginProvider.LOCAL)
                    .userType(UserType.USER)
                    .build();

            userRepository.save(gameUser);

            gameUserService.applyGame(gameId, gameUser.getUserId());


        }

        // when

        int threadCount = 5;

        List<Long> userIds = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            UserEntity gameUser = UserEntity.builder()
                    .email("tests@"+ i + "example.com")
                    .password(passwordEncoder.encode("Test@1234"))
                    .name("names" + i)
                    .nickname("names" + i)
                    .birth(LocalDate.of(1997,7,24))
                    .address("테스트용주소s" + i)
                    .phone("010-1111-1112")
                    .position(Position.GUARD)
                    .genderType(GenderType.MALE)
                    .loginProvider(LoginProvider.LOCAL)
                    .userType(UserType.USER)
                    .build();

            userRepository.save(gameUser);
            userIds.add(gameUser.getUserId());

        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);


        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fullHeadCountErrorCount = new AtomicInteger(0);

        // then
        for (Long userId : userIds) {

            executorService.submit(() -> {

                try {
                    startLatch.await();
                    gameUserService.applyGame(1L, userId);
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    if (e.getErrorCode() == FULL_HEADCOUNT_GAME) {
                        fullHeadCountErrorCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }

            });

        }
        GameEntity before = gameEntity;
        System.out.println("Before concurrency - headCount=" + before.getHeadCount()
                + ", participantCount=" + before.getParticipantCount()
                + ", startDateTime=" + before.getStartDateTime());

        startLatch.countDown();

        doneLatch.await();

        executorService.shutdown();

        GameEntity gameEntity2 = gameEntity;



        assertEquals(5, gameEntity.getParticipantCount());


    }

    @Test
    @DisplayName("경기 평가 테스트")
    void evaluatePlayerTest() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();




        List<Long> gameIds = new ArrayList<>();

        for (int i = 0; i < 11; i++) {
            CreateGameDto.Request request = CreateGameDto.Request.builder()
                    .title("테스트 게임")
                    .content("테스트 게임 본문")
                    .headCount(9)
                    .fieldStatus(FieldStatus.OUTDOOR)
                    .matchGenderType(MatchGenderType.MALE_ONLY)
                    .startDateTime(LocalDateTime.now().plusHours(4L + i))
                    .endDateTime(LocalDateTime.now().plusHours(5L + i))
                    .placeName("테스트 게임 장소")
                    .address("인천광역시 테스트 게임 주소")
                    .matchFormat(MatchFormat.THREE_ON_THREE)
                    .build();

            gameEntity = CreateGameDto.Request.toEntity(request, creator);

            gameRepository.save(gameEntity);
            participantGameEntity = new ParticipantGameEntity().toGameCreatorEntity(gameEntity, creator);

            participantGameRepository.save(participantGameEntity);
            gameIds.add(gameEntity.getGameId());
        }

        for (int i = 0; i < gameIds.size(); i++) {

            gameUserService.applyGame(gameIds.get(i), participantUserId);

            participantGameService.acceptGameUser(participantUserId, creatorId, gameIds.get(i));

            GameEntity gameEntitys = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameIds.get(i))
                    .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

            gameEntitys.setStartDateTime(LocalDateTime.now().minusDays(i + 1));
            gameEntitys.setEndDateTime(LocalDateTime.now().minusDays(i + 1));

        }




        // when

        EvaluatePlayerDto evaluator = EvaluatePlayerDto.builder()
                .receiverId(participantUserId)
                .score(5)
                .build();

        for (int i = 0; i < gameIds.size(); i++) {


            gameUserService.evaluatePlayer(gameIds.get(i), creatorId, evaluator);

        }

        // then

        assertEquals(GameUserLevel.PRO, participant.getGameUserLevel());
    }



}

