package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.dto.GameDto;
import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.gameCreator.type.CityName;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.global.dto.ApiResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GameServiceImplTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // given: 테스트용 유저 데이터 삽입
        UserEntity user = UserEntity.builder()
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


        user.setEmailAuth();

        userRepository.save(user);

        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(9)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.FEMALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(2L))
                .endDateTime(LocalDateTime.now().plusHours(3L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        GameEntity gameEntity = CreateGameDto.Request.toEntity(request, user);

        gameRepository.save(gameEntity);
    }


    @Test
    @DisplayName("경기 생성 테스트")
    void createGameTest() {
        // given

        UserEntity userEntity = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(9)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.FEMALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(2L))
                .endDateTime(LocalDateTime.now().plusHours(3L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();




        // when

        ApiResponse<CreateGameDto.Response> game = gameService.createGame(userEntity.getUserId(), request);


        // then

        assertEquals(userEntity.getNickname(), game.getData().getCreatorNickname());
        assertEquals("경기 생성이 완료되었습니다.", game.getMessage());

    }

    @Test
    @DisplayName("경기 생성 실패 테스트 - 경기 형식 & 인원수")
    void createGameTest_Fail_MatchFormat() {
        // given
        UserEntity userEntity = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(15)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.FEMALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(2L))
                .endDateTime(LocalDateTime.now().plusHours(3L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();
        // when

        // 경기 형식 -> THREE_ON_THREE -> 6명 이상 9명 이하

        CustomException exception = assertThrows(CustomException.class, () -> gameService.createGame(userEntity.getUserId(), request));

        // then

        assertEquals(ErrorCode.INVALID_HEADCOUNT, exception.getErrorCode());

    }

    @Test
    @DisplayName("경기 생성 실패 테스트 - 게임 시간")
    void createGameTest_Fail_GameTime_Invalid() {
        // given

        UserEntity userEntity = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(9)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.FEMALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(2L))
                .endDateTime(LocalDateTime.now().plusHours(2L).plusMinutes(30L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameService.createGame(userEntity.getUserId(), request));

        // then

        assertEquals(ErrorCode.INVALID_GAME_TIME, exception.getErrorCode());

    }


    @Test
    @DisplayName("경기 상세조회 테스트")
    void detailGame() {
        // given

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));


        // when

        ApiResponse<GameDto> detailGame = gameService.detailGame(gameEntity.getGameId());

        // then

        assertEquals("경기 상세조회에 성공하였습니다.", detailGame.getMessage());



    }

    @Test
    @DisplayName("경기 상세조회 실패 테스트 - 경기 삭제 시간 존재")
    void detailGame_Fail_DeletedDateTime_NotNull() {
        // given
        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));


        // when

        gameEntity.setDeletedDateTime(gameEntity.getStartDateTime().plusHours(10L));

        CustomException exception = assertThrows(CustomException.class, () ->
                gameService.detailGame(gameEntity.getGameId())
        );
        // then


        assertEquals(ErrorCode.GAME_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("경기 검색 테스트")
    void searchGameTest() {
        // given



        // when

        ApiResponse<Page<SearchGameDto>> searchedGame1 = gameService.searchGame(LocalDate.now(), CityName.INCHEON, null, null, null, null, PageRequest.of(0, 10));


        // 검색 결과 존재하지 않을 시
        ApiResponse<Page<SearchGameDto>> searchedGame2 = gameService.searchGame(LocalDate.now().plusDays(2), CityName.INCHEON, null, null, null, null, PageRequest.of(0, 10));


        // then

        assertEquals("경기 검색이 완료되었습니다.", searchedGame1.getMessage());

        assertEquals("경기 검색결과가 없습니다.", searchedGame2.getMessage());
    }

    @Test
    @DisplayName("경기 수정 테스트")
    void editGameTest() {



        // given

        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));



        // when

        EditGameDto request = EditGameDto.builder()
                .matchFormat(MatchFormat.FIVE_ON_FIVE)
                .headCount(10)
                .build();

        ApiResponse<GameDto> editedGame = gameService.editGame(request, gameEntity.getGameId(), tokenDto.getUserDto().getUserId());


        // then

        assertEquals(MatchFormat.FIVE_ON_FIVE, editedGame.getData().getMatchFormat());
        assertEquals("경기 수정이 완료되었습니다.", editedGame.getMessage());

    }


    @Test
    @DisplayName("경기 수정 실패 테스트 - 경기 형식 변경 시 경기 인원수 미변경")
    void editGameTest_Fail_MatchFormat_Not_Updated_HeadCount() {
        // given

        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));



        // when

        EditGameDto request = EditGameDto.builder()
                .matchFormat(MatchFormat.FIVE_ON_FIVE)
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> gameService.editGame(request, gameEntity.getGameId(), tokenDto.getUserDto().getUserId()));

        // then

        assertEquals(ErrorCode.UPDATE_GAME_HEAD_COUNT, exception.getErrorCode());

    }


}