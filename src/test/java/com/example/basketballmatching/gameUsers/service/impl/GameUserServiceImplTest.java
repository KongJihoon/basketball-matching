package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.gameUsers.service.GameUserService;
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
@Rollback(value = false)
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

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        // given: 테스트용 유저 데이터 삽입
        UserEntity creator = UserEntity.builder()
                .email("test2@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("name")
                .nickname("name")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소")
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .genderType(GenderType.NONE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        UserEntity gameUser = UserEntity.builder()
                .email("test3@example.com")
                .password(passwordEncoder.encode("Test@1234"))
                .name("name")
                .nickname("name2")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소2")
                .phone("010-1111-1112")
                .position(Position.GUARD)
                .genderType(GenderType.FEMALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();


        creator.setEmailAuth();
        gameUser.setEmailAuth();

        userRepository.save(creator);
        userRepository.save(gameUser);

        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(6)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.FEMALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(1L))
                .endDateTime(LocalDateTime.now().plusHours(2L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        GameEntity gameEntity = CreateGameDto.Request.toEntity(request, creator);

        gameRepository.save(gameEntity);
    }

    @Test
    @DisplayName("경기 신청 테스트")
    void applyGameTest() {
        // given

        UserEntity userEntity = userRepository.findById(2L)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        // when

        CommonResponse<ApplyGameUserDto> applyGame = gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());


        // then
        assertEquals("경기 신청이 완료되었습니다.", applyGame.getMessage());

        assertEquals(userEntity.getUserId(), applyGame.getData().getUserId());


    }

    @Test
    @DisplayName("경기 신청 실패 - 여성만 참여가능 -> 남성 참가자가 신청 시")
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
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        userRepository.save(gameUser);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));


        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.applyGame(gameEntity.getGameId(), gameUser.getUserId()));
        // then

        assertEquals(ErrorCode.ONLY_FEMALE_GAME, exception.getErrorCode());


    }

    @Test
    @DisplayName("경기 신청 실패 테스트 - 경기 인원 초과")
    void applyGameTest_Fail_Full_HeadCount() {
        // given

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        int headCount = gameEntity.getHeadCount();

        for (int i = 0; i < headCount; i++) {
            UserEntity extraUser = UserEntity.builder()
                    .email("extra" + i + "@example.com")
                    .password(passwordEncoder.encode("Test@1234!"))
                    .name("참가자" + i)
                    .nickname("참가자" + i)
                    .birth(LocalDate.of(1998, 1, 1))
                    .address("인천시 테스트주소" + i)
                    .phone("010-9999-99" + i)
                    .position(Position.GUARD)
                    .genderType(GenderType.FEMALE)
                    .loginProvider(LoginProvider.LOCAL)
                    .userType(UserType.USER)
                    .build();

            userRepository.save(extraUser);

            // 이미 참가된 상태를 DB에 반영 (status = APPLY)
            gameUserService.applyGame(gameEntity.getGameId(), extraUser.getUserId());
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

        UserEntity userEntity = userRepository.findById(2L)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), userEntity.getUserId())
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        // when

        CheckResponse checkResponse = gameUserService.cancelGame(userEntity.getUserId(), gameEntity.getGameId());

        // then

        assertEquals(ParticipantGameStatus.CANCEL, participantGameEntity.getParticipantGameStatus());
        assertEquals("경기 취소가 완료되었습니다.", checkResponse.getMessage());

    }

    @Test
    @DisplayName("경기 참가 취소 실패 테스트 - 수락되지 않은 유저 접근")
    void cancelGameFailTest_Apply_User() {
        // given

        UserEntity userEntity = userRepository.findById(2L)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.cancelGame(userEntity.getUserId(), gameEntity.getGameId()));

        // then

        assertEquals(NOT_ACCEPT_USER, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 참가 취소 실패테스트 - 경기시작 30분전 취소 불가")
    void cancelGameFailTest_NOT_ALLOWED_CANCEL() {
        // given
        UserEntity userEntity = userRepository.findById(2L)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), userEntity.getUserId())
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        gameEntity.setStartDateTime(LocalDateTime.now().minusMinutes(40));
        gameEntity.setEndDateTime(LocalDateTime.now().minusMinutes(40));


        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.cancelGame(userEntity.getUserId(), gameEntity.getGameId()));

        // then

        assertEquals(NOT_ALLOWED_CANCEL, exception.getErrorCode());

    }

    @Test
    @DisplayName("현재 예정 경기 조회 테스트")
    void getCurrentGameListTest() {
        // given

        UserEntity userEntity = userRepository.findById(2L)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());

        // when
        CommonResponse<List<CurrentGameListDto>> myCurrentGameList = gameUserService.getMyCurrentGameList(userEntity.getUserId(), PageRequest.of(0, 10));

        // then

        assertEquals(gameEntity.getGameId(), myCurrentGameList.getData().get(0).getGameId());
        assertEquals("현재 예정된 게임 조회가 완료되었습니다.", myCurrentGameList.getMessage());

    }

    @Test
    @DisplayName("지난 경기 조회 테스트")
    void getLastGameListTest() {
        // given
        UserEntity userEntity = userRepository.findById(2L)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), userEntity.getUserId())
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        gameEntity.setStartDateTime(LocalDateTime.now().minusHours(2L));
        gameEntity.setEndDateTime(LocalDateTime.now().minusHours(2L));

        gameRepository.save(gameEntity);
        // when

        CommonResponse<List<LastGameListDto>> myLastGameList = gameUserService.getMyLastGameList(userEntity.getUserId(), PageRequest.of(0, 10));

        // then

        assertEquals(gameEntity.getGameId(), myLastGameList.getData().get(0).getGameId());
        assertEquals("지난 게임 조회가 완료되었습니다.", myLastGameList.getMessage());

    }


    @Test
    @DisplayName("경기 참가 신청 - 동시성 이슈 테스트")
    void applyGameTest_Concurrency_Issue() throws Exception {
        // given

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
                    .genderType(GenderType.FEMALE)
                    .loginProvider(LoginProvider.LOCAL)
                    .userType(UserType.USER)
                    .build();

            userRepository.save(gameUser);

            gameUserService.applyGame(1L, gameUser.getUserId());


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
                    .genderType(GenderType.FEMALE)
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
        GameEntity before = gameRepository.findById(1L).orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        System.out.println("Before concurrency - headCount=" + before.getHeadCount()
                + ", participantCount=" + before.getParticipantCount()
                + ", startDateTime=" + before.getStartDateTime());

        startLatch.countDown();

        doneLatch.await();

        executorService.shutdown();

        GameEntity gameEntity = gameRepository.findById(1L)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));



        assertEquals(6, gameEntity.getParticipantCount());


    }


}