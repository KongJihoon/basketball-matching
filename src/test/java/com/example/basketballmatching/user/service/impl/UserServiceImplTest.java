package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Transactional
@ActiveProfiles("local-test")
class UserServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockBean
    private MailService mailService;

    @MockBean
    private RedisService redisService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
                .userType(UserType.USER)
                .loginProvider(LoginProvider.LOCAL)
                .build();

        user.setEmailAuth();

        userRepository.save(user);
    }


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
                .address("테스트용주소")
                .phone("010-1111-1111")
                .birth(LocalDate.now())
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
        // when

        mailService.sendAuthMail(request.getEmail());

        mailService.verifyEmailAuth(request.getEmail(), redisService.getData("email:auth:" + "test@test.com"));

        CommonResponse<SignUpDto.Response> response = userService.signUp(request);
        // then

        assertEquals("회원가입에 성공하였습니다.", response.getMessage());


    }

    @Test
    @DisplayName("중복 이메일 확인")
    void NotValidEmail() {
        // given



        // when

        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.checkEmail("test2@example.com");
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
                .loginProvider(LoginProvider.LOCAL)
                .address("테스트 주소")
                .birth(LocalDate.now())
                .genderType(GenderType.MALE)
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
                .loginProvider(LoginProvider.LOCAL)
                .birth(LocalDate.now())
                .genderType(GenderType.MALE)
                .position(Position.GUARD)
                .build();

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(request1));

        // then

        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, exception.getErrorCode());

    }

    @Test
    @DisplayName("회원 정보 조회 성공 테스트")
    void getUserInfoTest() {
        // given

        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");




        // when

        CommonResponse<UserDto> userInfo = userService.getUserInfo(tokenDto.getUserDto().getUserId());

        // then

        assertEquals("회원정보 조회에 성공하였습니다.", userInfo.getMessage());

    }

    @Test
    @DisplayName("회원정보 조회 실패 케이스 - 로그인이 안된 사용자 조회")
    void getUserInfo_Fail_Test() {
        // given


        Long userId = 9999L;

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserInfo(userId));
        // then


        assertEquals(exception.getErrorCode(), ErrorCode.USER_NOT_FOUND);




    }

    @Test
    @DisplayName("회원 정보 수정 테스트")
    void editUserInfoTest() {
        // given

        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");

        // when

        CommonResponse<UserDto> response = userService.editUserInfo(tokenDto.getUserDto().getUserId()
                , EditUserDto.builder()
                        .nickname("테스트 닉네임2")
                        .position(Position.CENTER)
                        .build());


        // then

        assertEquals("회원정보 수정이 완료되었습니다.", response.getMessage());
        assertEquals(Position.CENTER, response.getData().getPosition());

    }

    @Test
    @DisplayName("회원정보 수정 실패 테스트 - 존재하는 닉네임으로 변경")
    void editUserInfo_Fail_Test() {
        // given

        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");

        // when

        EditUserDto editUserDto = EditUserDto.builder()
                .nickname("name")
                .position(Position.CENTER)
                .build();

        CustomException exception = assertThrows(CustomException.class, () -> userService.editUserInfo(tokenDto.getUserDto().getUserId(), editUserDto));

        // then

        assertEquals(ErrorCode.ALREADY_EXIST_NICKNAME, exception.getErrorCode());

    }


    @Test
    @DisplayName("로그인 사용자 비밀번호 변경")
    void changePasswordTest() {
        // given

        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");

        ChangePasswordDto changePasswordDto = ChangePasswordDto.builder()
                .currentPassword("Test1234!")
                .newPassword("Test@1234")
                .newCheckPassword("Test@1234")
                .build();

        // when

        CheckResponse checkResponse = userService.changePassword(tokenDto.getUserDto().getUserId(), changePasswordDto);

        // then

        assertEquals("비밀번호 변경을 완료하였습니다.", checkResponse.getMessage());
        assertEquals(true, checkResponse.isSuccess());

    }

    @Test
    @DisplayName("로그인 사용자 비밀번호 변경 실패 테스트 - 새로운 비밀번호 불일치")
    void changePassword_Fail_Test() {
        // given
        TokenDto tokenDto = authService.loginUser("test2@example.com", "Test1234!");

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.changePassword(tokenDto.getUserDto().getUserId(),
                ChangePasswordDto.builder()
                        .currentPassword("Test1234!")
                        .newPassword("Test@1234")
                        .newCheckPassword("Tet@1234")
                        .build()));

        // then


        assertEquals(ErrorCode.PASSWORD_NOT_MATCH, exception.getErrorCode());

    }


}