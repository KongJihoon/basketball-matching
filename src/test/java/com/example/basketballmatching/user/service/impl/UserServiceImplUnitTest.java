package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {


    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameQueryRepository gameQueryRepository;

    @InjectMocks
    private UserServiceImpl userService;





    @Test
    @DisplayName("이메일 중복 확인")
    void checkEmailTest() {
        // given

        String email = "test@test.com";

        when(userRepository.existsByEmail(email)).thenReturn(false);


        // when

        CheckResponse checkResponse = userService.checkEmail(email);


        // then

        assertEquals(true, checkResponse.isSuccess());
        assertEquals("사용가능한 이메일입니다.", checkResponse.getMessage());


    }

    @Test
    @DisplayName("이매일 중복 확인 실패 테스트")
    void checkEmailFailTest_ALREADY_EXIST_EMAIL() {
        // given

        String email = "test@test.com";


        // when
        when(userRepository.existsByEmail(email)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class, () -> userService.checkEmail(email));

        // then

        assertEquals(ALREADY_EXIST_EMAIL, exception.getErrorCode());

    }

    @Test
    @DisplayName("닉네임 중복 확인 테스트")
    void checkNicknameTest() {
        // given

        String nickname = "test";


        // when

        when(userRepository.existsByNickname(nickname)).thenReturn(false);

        CheckResponse checkResponse = userService.checkNickname(nickname);

        // then
        assertEquals(true, checkResponse.isSuccess());
        assertEquals("사용가능한 닉네임입니다.", checkResponse.getMessage());


    }

    @Test
    @DisplayName("닉네임 중복 확인 실패 테스트")
    void checkNicknameFailTest_ALREADY_EXIST_NICKNAME() {
        // given

        String nickname = "test";


        // when
        when(userRepository.existsByNickname(nickname)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class, () -> userService.checkNickname(nickname));

        // then

        assertEquals(ALREADY_EXIST_NICKNAME, exception.getErrorCode());

    }

    @Test
    @DisplayName("유저 회원가입 테스트")
    void signUpTest() {
        // given
        SignUpDto.Request req = SignUpDto.Request.builder()
                .email("test@test.com")
                .password("Test@1234")
                .checkPassword("Test@1234")
                .nickname("커리")
                .name("서장훈")
                .birth(LocalDate.of(1997,1,1))
                .phone("010-1111-0000")
                .address("서울")
                .position(Position.NONE)
                .genderType(GenderType.MALE)
                .build();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.existsByNickname(req.getNickname())).thenReturn(false);
        when(redisService.getData("email:auth:verified:" + req.getEmail())).thenReturn("123456");

        when(passwordEncoder.encode(req.getPassword())).thenReturn("ENCODED");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        when(userRepository.save(userCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));


        // when
        CommonResponse<SignUpDto.Response> commonResponse = userService.signUp(req);


        // then
        verify(redisService).deleteData("email:auth:verified:" + req.getEmail());
        verify(passwordEncoder).encode(req.getPassword());
        verify(userRepository).save(any(UserEntity.class));

        UserEntity saved = userCaptor.getValue();

        assertEquals(req.getEmail(), saved.getEmail());
        assertEquals("ENCODED",saved.getPassword());
        assertEquals(req.getNickname(), saved.getNickname());


        assertEquals("회원가입에 성공하였습니다.", commonResponse.getMessage());
        assertNotNull(commonResponse.getData());

    }

    @Test
    @DisplayName("유저 회원가입 실패 테스트 - 이메일 중복")
    void signUpFailTest_ALREADY_EXIST_EMAIL() {
        // given

        SignUpDto.Request req = SignUpDto.Request.builder()
                .email("test@test.com")
                .password("Test@1234")
                .checkPassword("Test@1234")
                .nickname("커리")
                .name("서장훈")
                .birth(LocalDate.of(1997,1,1))
                .phone("010-1111-0000")
                .address("서울")
                .position(Position.NONE)
                .genderType(GenderType.MALE)
                .build();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);


        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(req));

        // then

        assertEquals(ALREADY_EXIST_EMAIL, exception.getErrorCode());
        verify(redisService, never()).getData(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());

    }

    @Test
    @DisplayName("유저 회원가입 실패 테스트 - Redis의 값이 저장되어있지 않은 경우")
    void signUpFailTest_EMAIL_NOT_VERIFIED() {
        // given
        SignUpDto.Request req = SignUpDto.Request.builder()
                .email("test@test.com")
                .password("Test@1234")
                .checkPassword("Test@1234")
                .nickname("커리")
                .name("서장훈")
                .birth(LocalDate.of(1997,1,1))
                .phone("010-1111-0000")
                .address("서울")
                .position(Position.NONE)
                .genderType(GenderType.MALE)
                .build();

        when(redisService.getData("email:auth:verified:" + req.getEmail())).thenReturn(null);

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(req));

        // then

        assertEquals(EMAIL_NOT_VERIFIED, exception.getErrorCode());
        verify(redisService).getData("email:auth:verified:" + req.getEmail());
        verify(redisService, never()).deleteData("email:auth:verified:" + req.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(UserEntity.class));

    }


//    @Test
//    @DisplayName("회원 정보 조회 테스트")
//    void getUserInfoTest() {
//        // given
//
//        Long userId = 1L;
//
//        UserEntity userEntity = UserEntity.builder()
//                .userId(1L)
//                .email("test@test.com")
//                .nickname("커리")
//                .name("testName")
//                .birth(LocalDate.parse("1997-01-01"))
//                .phone("010-0000-0000")
//                .address("testAddress")
//                .position(Position.GUARD)
//                .userType(UserType.USER)
//                .genderType(GenderType.MALE)
//                .build();
//
//        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)).thenReturn(Optional.of(userEntity));
//
//
//        // when
//
//        UserDto userDto = userCacheService.getUserDtoCached(userId);
//
//
//        // then
//
//
//        assertNotNull(userDto);
//        verify(userRepository, times(1)).findByUserIdAndDeletedDateTimeIsNull(userId);
//
//
//    }

//    @Test
//    @DisplayName("회원 정보 조회 실패 테스트 - USER_NOT_FOUND")
//    void getUserInfoFailTest() {
//        // given
//
//        Long userId = 1L;
//
//        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId))
//                .thenReturn(Optional.empty());
//
//        // when
//
//        CustomException exception = assertThrows(CustomException.class, () -> userCacheService.getUserDtoCached(userId));
//
//        // then
//
//        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
//
//    }


    @Test
    @DisplayName("회원 정보 수정 테스트")
    void editUserInfoTest() {
        // given

        Long userId = 1L;

        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .email("test@test.com")
                .nickname("커리")
                .name("testName")
                .birth(LocalDate.parse("1997-01-01"))
                .phone("010-0000-0000")
                .address("testAddress")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .build();

        EditUserDto editUserDto = EditUserDto.builder()
                .nickname("커리2")
                .position(Position.CENTER)
                .build();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByNicknameAndUserIdNot(editUserDto.getNickname(), userId)).thenReturn(false);

        // when

        CommonResponse<UserDto> commonResponse = userService.editUserInfo(userId, editUserDto);

        // then

        assertEquals("회원정보 수정이 완료되었습니다.", commonResponse.getMessage());
        assertNotNull(commonResponse.getData());

    }

    @Test
    @DisplayName("회원정보 수정 실패 테스트 - 중복 닉네임 존재")
    void editUserInfoFailTest_ALREADY_EXIST_NICKNAME() {
        // given

        Long userId = 1L;

        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .email("test@test.com")
                .nickname("커리2")
                .name("testName")
                .birth(LocalDate.parse("1997-01-01"))
                .phone("010-0000-0000")
                .address("testAddress")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .build();

        EditUserDto editUserDto = EditUserDto.builder()
                .nickname("커리2")
                .position(Position.CENTER)
                .build();


        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)).thenReturn(Optional.of(userEntity));
        when(userRepository.existsByNicknameAndUserIdNot(editUserDto.getNickname(), userId)).thenReturn(true);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> userService.editUserInfo(userId, editUserDto));

        // then

        assertEquals(ALREADY_EXIST_NICKNAME, exception.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호 변경 테스트")
    void changePasswordTest() {
        // given

        Long userId = 1L;

        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .password("ENCODED_OLD")
                .build();

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("old@123")
                .newPassword("test@12")
                .newCheckPassword("test@12")
                .build();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches("old@123", "ENCODED_OLD")).thenReturn(true);

        when(passwordEncoder.encode("test@12"))
                .thenReturn("ENCODED_NEW");
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // when

        CheckResponse checkResponse = userService.changePassword(userId, changePasswordDto);

        // then



        verify(passwordEncoder).encode("test@12");
        verify(userRepository).save(any(UserEntity.class));

        assertEquals("ENCODED_NEW", captor.getValue().getPassword());
        assertEquals("비밀번호 변경을 완료하였습니다.", checkResponse.getMessage());
    }


    @Test
    @DisplayName("회원 탈퇴 테스트")
    void deleteUserTest() {
        // given

        Long userId = 1L;
        String token = "accessToken";
        long remainingMills = 10_000L;

        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .email("test@test.com")
                .build();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)).thenReturn(Optional.of(userEntity));
        when(tokenProvider.getRemainingTime(token)).thenReturn(remainingMills);

        when(gameRepository.findByUserEntity_UserIdAndDeletedDateTimeIsNull(userId))
                .thenReturn(List.of());

        when(participantGameRepository.findByUserEntity_UserIdAndParticipantGameStatusIn(eq(userId), eq(List.of(ParticipantGameStatus.ACCEPT, ParticipantGameStatus.APPLY))))
                .thenReturn(List.of());


        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // when

        CheckResponse checkResponse = userService.deleteUser(userId, token);

        // then

        verify(tokenProvider).getRemainingTime(token);

        verify(redisService).setDataExpireMillis(
                "logout:access:" + token,
                "LOGOUT",
                remainingMills
        );

        verify(redisService).deleteData("refreshToken:" + userEntity.getEmail());

        UserEntity saved = captor.getValue();

        assertNotNull(saved.getDeletedDateTime());

        assertTrue(checkResponse.isSuccess());
        assertEquals("회원탈퇴에 성공하였습니다.", checkResponse.getMessage());

    }

    @Test
    @DisplayName("회원 탈퇴 실패 테스트 - 토큰이 null인 경우")
    void deleteUserFailTest_NOT_FOUND_TOKEN() {
        // given
        Long userId = 1L;

        UserEntity user = UserEntity.builder()
                .userId(userId)
                .email("test@test.com")
                .build();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId))
                .thenReturn(Optional.of(user));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.deleteUser(userId, null));

        // then

        assertEquals(NOT_FOUND_TOKEN, exception.getErrorCode());

        verifyNoInteractions(tokenProvider, redisService);
        verifyNoInteractions(gameRepository, participantGameRepository, gameQueryRepository);
        verify(userRepository, never()).save(any(UserEntity.class));
    }



}

