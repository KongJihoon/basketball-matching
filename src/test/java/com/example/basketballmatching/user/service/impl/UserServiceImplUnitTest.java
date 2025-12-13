package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

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

}