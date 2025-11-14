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
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
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
                .genderType(GenderType.MALE)
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
                .headCount(9)
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

        ApiResponse<ApplyGameUserDto> applyGame = gameUserService.applyGame(gameEntity.getGameId(), userEntity.getUserId());


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
        ApiResponse<List<CurrentGameListDto>> myCurrentGameList = gameUserService.getMyCurrentGameList(userEntity.getUserId(), PageRequest.of(0, 10));

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

        ApiResponse<List<LastGameListDto>> myLastGameList = gameUserService.getMyLastGameList(userEntity.getUserId(), PageRequest.of(0, 10));

        // then

        assertEquals(gameEntity.getGameId(), myLastGameList.getData().get(0).getGameId());
        assertEquals("지난 게임 조회가 완료되었습니다.", myLastGameList.getMessage());

    }

}