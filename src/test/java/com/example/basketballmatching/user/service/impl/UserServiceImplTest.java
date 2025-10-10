package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import com.example.basketballmatching.user.type.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private RedisService redisService;



    @Test
    @DisplayName("회원가입 테스트")
    void signUpTest() {
        // given

        SignUpDto.Request request = SignUpDto.Request.builder()
                .email("test@test.com")
                .password("Test@123")
                .checkPassword("Test@123")
                .nickname("JI")
                .name("test")
                .phone("010-1111-1111")
                .birth(LocalDate.now())
                .position(Position.GUARD)
                .build();
        // when

        ApiResponse<SignUpDto.Response> response = userService.signUp(request);
        // then

        assertEquals("회원가입에 성공하였습니다.", response.getMessage());


    }

    @Test
    @DisplayName("중복 이메일 확인")
    void NotValidEmail() {
        // given
        SignUpDto.Request request1 = SignUpDto.Request.builder()
                .email("test@naver.com")
                .password("Test@123")
                .checkPassword("Test@123")
                .nickname("JI")
                .name("test")
                .phone("010-1111-1111")
                .birth(LocalDate.now())
                .position(Position.GUARD)
                .build();

        SignUpDto.Request request2 = SignUpDto.Request.builder()
                .email("test@naver.com")
                .password("Test@123")
                .checkPassword("Test@123")
                .nickname("JI1234")
                .name("test4")
                .phone("010-1111-2222")
                .birth(LocalDate.now())
                .position(Position.GUARD)
                .build();

        userService.signUp(request1);


        // when

        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.signUp(request2);
        });


        // then

        assertEquals(ErrorCode.ALREADY_EXIST_EMAIL, exception.getErrorCode());

    }

    @Test
    @DisplayName("이메일 전송 확인 테스트")
    void sendMailTest() {
        // given

        mailService.sendAuthMail("rwg1279@naver.com");

        String data = redisService.getData("email:auth:" + "rwg1279@naver.com");


        // when

        CheckResponse checkResponse = mailService.verifyEmailAuth("rwg1279@naver.com", data);

        SignUpDto.Request request1 = SignUpDto.Request.builder()
                .email("rwg1279@naver.com")
                .password("Test@123")
                .checkPassword("Test@123")
                .nickname("JI")
                .name("test")
                .phone("010-1111-1111")
                .birth(LocalDate.now())
                .position(Position.GUARD)
                .build();

        userService.signUp(request1);

        // then

        assertEquals("이메일 인증에 성공하였습니다.", checkResponse.getMessage());

        assertEquals(null, redisService.getData("email:auth:verified:" + request1.getEmail()));

    }

    @Test
    @DisplayName("이메일 인증 없이 회원가입 시도 시 예외 발생")
    void signUpWithoutEmailVerification() {
        // given

        SignUpDto.Request request1 = SignUpDto.Request.builder()
                .email("rwg1279@naver.com")
                .password("Test@123")
                .checkPassword("Test@123")
                .nickname("JI")
                .name("test")
                .phone("010-1111-1111")
                .birth(LocalDate.now())
                .position(Position.GUARD)
                .build();

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(request1));

        // then

        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());

    }


}