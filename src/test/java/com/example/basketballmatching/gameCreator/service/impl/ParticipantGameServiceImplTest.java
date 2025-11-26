package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.AcceptGameUserListDto;
import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.ParticipantGameService;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.dto.SignUpDto;
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
import java.util.ArrayList;
import java.util.List;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ParticipantGameServiceImplTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GameUserService gameUserService;
    @Autowired
    private ParticipantGameService participantGameService;
    @Autowired
    private ParticipantGameRepository participantGameRepository;

    UserEntity creator;

    UserEntity participant;

    GameEntity gameEntity;

    ParticipantGameEntity participantGameEntity;

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
    @DisplayName("경기 참가 신청자 조회")
    void getApplyParticipantListTest() {
        // given

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(this.gameEntity.getGameId())
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        List<Long> userIds = new ArrayList<>();

        for (int i = 0; i < 8; i++) {

            UserEntity user = UserEntity.builder()
                    .email("test" + i + "@example.com")
                    .password(passwordEncoder.encode("Test1234!"))
                    .name("name" + i)
                    .nickname("name" + i)
                    .birth(LocalDate.of(1997,7,24))
                    .address("테스트용주소")
                    .phone("010-1111-1111")
                    .position(Position.GUARD)
                    .genderType(GenderType.MALE)
                    .loginProvider(LoginProvider.LOCAL)
                    .userType(UserType.USER)
                    .build();

            user.setEmailAuth();

            userRepository.save(user);

            userIds.add(user.getUserId());


        }

        for (int i = 0; i < userIds.size(); i++) {

            gameUserService.applyGame(gameEntity.getGameId(), userIds.get(i));
        }

        UserEntity userEntity = userRepository.findByUserIdAndDeletedDateTimeIsNull(userIds.get(0))
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        // when

        CommonResponse<List<ApplyGameUserListDto>> commonResponse = participantGameService.getApplyParticipantList(gameEntity.getGameId(), gameEntity.getUserEntity().getUserId(), PageRequest.of(0, 10));


        // then

        assertEquals("경기 신청자 조회가 완료되었습니다.", commonResponse.getMessage());
        assertEquals(userEntity.getNickname(), commonResponse.getData().get(0).getNickname());
    }

    @Test
    @DisplayName("경기 신청자 조회 실패 테스트 - 경기 생성자가 아닌 유저 조회")
    void getApplyParticipantListFailTest_Not_Creator() {
        // given

        UserEntity user = UserEntity.builder()
                .email("test1@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("name1")
                .nickname("name1")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소")
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        user.setEmailAuth();

        userRepository.save(user);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(this.gameEntity.getGameId())
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), user.getUserId());

        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.getApplyParticipantList(gameEntity.getGameId(), user.getUserId(), PageRequest.of(0, 10)));

        // then

        assertEquals(ErrorCode.NOT_GAME_CREATOR, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 수락 테스트")
    void acceptGameUserTest() {
        // given

        UserEntity creator = userRepository.findByUserIdAndDeletedDateTimeIsNull(this.creator.getUserId())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));



        UserEntity participant = userRepository.findByUserIdAndDeletedDateTimeIsNull(this.participant.getUserId())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(this.gameEntity.getGameId())
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), participant.getUserId());

        // when

        CheckResponse checkResponse = participantGameService.acceptGameUser(participant.getUserId(), creator.getUserId(), gameEntity.getGameId());

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participant.getUserId())
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        // then

        assertEquals("경기 수락이 완료되었습니다.", checkResponse.getMessage());
        assertEquals(ParticipantGameStatus.ACCEPT, participantGameEntity.getParticipantGameStatus());



    }

    @Test
    @DisplayName("경기 수락 테스트 - 이미 시작된 경기")
    void acceptGameUserFailTest_ALREADY_START_GAME() {
        // given

        UserEntity creator = userRepository.findByUserIdAndDeletedDateTimeIsNull(this.creator.getUserId())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));



        UserEntity participant = userRepository.findByUserIdAndDeletedDateTimeIsNull(this.participant.getUserId())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(this.gameEntity.getGameId())
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        gameUserService.applyGame(gameEntity.getGameId(), participant.getUserId());

        gameEntity.setStartDateTime(LocalDateTime.now().minusHours(1));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.acceptGameUser(participant.getUserId(), creator.getUserId(), gameEntity.getGameId()));

        // then

        assertEquals(ALREADY_START_GAME, exception.getErrorCode());

    }


    @Test
    @DisplayName("경기 수락자 조회 테스트")
    void getAcceptParticipantListTest() {
        // given

        Long gameId = gameEntity.getGameId();

        // 경기 생성자는 경기 자동참가
        Long creatorId = gameEntity.getUserEntity().getUserId();

        // when

        CommonResponse<List<AcceptGameUserListDto>> commonResponse = participantGameService.getAcceptParticipantList(gameId, creatorId, PageRequest.of(0, 10));

        // then

        assertEquals("경기 참가자 조회가 완료되었습니다.", commonResponse.getMessage());
        assertEquals(creator.getNickname(), commonResponse.getData().get(0).getNickname());

    }

    @Test
    @DisplayName("경기 수락자 조회 실패 테스트 - 경기 생성자만 조회 가능")
    void getAcceptParticipantListFailTest_NOT_GAME_CREATOR() {
        // given

        Long participantUserId = participant.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        participantGameService.acceptGameUser(participantUserId, creator.getUserId(), gameId);
        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.getAcceptParticipantList(gameId, participantUserId, PageRequest.of(0, 10)));
        // then


        assertEquals(NOT_GAME_CREATOR, exception.getErrorCode());
    }

    @Test
    @DisplayName("경기 참가자 거절 테스트")
    void rejectGameUserTest() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        ParticipantGameEntity participantGame = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, participantUserId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        // when


        CheckResponse checkResponse = participantGameService.rejectGameUser(participantUserId, creatorId, gameId);

        // then

        assertEquals("경기 거절을 완료하였습니다.", checkResponse.getMessage());
        assertEquals(ParticipantGameStatus.REJECT, participantGame.getParticipantGameStatus());

    }

    @Test
    @DisplayName("경기 참가자 거절 실패 테스트 - 이미 거절된 유저 거절 불가")
    void rejectGameUserFailTest_ALREADY_REJECT_USER() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        participantGameService.rejectGameUser(participantUserId, creatorId, gameId);



        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.rejectGameUser(participantUserId, creatorId, gameId));


        // then

        assertEquals(ALREADY_REJECT_USER, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 참가자 거절 실패 테스트 - 경기 시작 이후 거절 불가")
    void rejectGameUserFailTest_ALREADAY_START_GAME() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        gameEntity.setStartDateTime(LocalDateTime.now().minusHours(1));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.rejectGameUser(participantUserId, creatorId, gameId));

        // then

        assertEquals(ALREADY_START_GAME, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 참가자 강퇴 테스트")
    void kickoutGameUserTest() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        participantGameService.acceptGameUser(participantUserId, creatorId, gameId);

        ParticipantGameEntity participantGame = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, participantUserId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        // when

        CheckResponse checkResponse = participantGameService.kickOutGameUser(participantUserId, creatorId, gameId);


        // then

        assertEquals("참가자 강퇴를 완료하였습니다.", checkResponse.getMessage());
        assertEquals(ParticipantGameStatus.KICKOUT, participantGame.getParticipantGameStatus());
    }

    @Test
    @DisplayName("경기 참가자 강퇴 실패 테스트 - 경기 생성자는 강퇴 불가")
    void kickoutGameUserFailTest_NOT_KICKOUT_CREATOR() {
        // given
        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();



        // when
        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.kickOutGameUser(creatorId, creatorId, gameId));
        // then

        assertEquals(NOT_KICKOUT_CREATOR, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 참가자 강퇴 실패 테스트 - APPLY유저는 강퇴 불가")
    void kickoutGameUserFailTest_NOT_ACCEPT_USER() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.kickOutGameUser(participantUserId, creatorId, gameId));

        // then

        assertEquals(NOT_ACCEPT_USER, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 삭제 테스트")
    void deleteGameTest() {
        // given

        Long participantUserId = participant.getUserId();

        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameUserService.applyGame(gameId, participantUserId);

        ParticipantGameEntity participantGame = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, participantUserId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        // when

        CheckResponse checkResponse = participantGameService.deleteGame(creatorId, gameId);

        // then

        assertEquals("경기 삭제가 완료되었습니다.", checkResponse.getMessage());
        assertEquals(ParticipantGameStatus.DELETE, participantGame.getParticipantGameStatus());
        assertNotNull(gameEntity.getDeletedDateTime());

    }

    @Test
    @DisplayName("경기 삭제 실패 테스트 - 경기 시작 시간 30분전 삭제 불가")
    void deleteGameFailTest_NOT_DELETE_GAME() {
        // given
        Long creatorId = creator.getUserId();

        Long gameId = gameEntity.getGameId();

        gameEntity.setStartDateTime(LocalDateTime.now().minusMinutes(29));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> participantGameService.deleteGame(creatorId, gameId));

        // then

        assertEquals(NOT_DELETE_GAME, exception.getErrorCode());

    }



}