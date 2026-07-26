package com.example.basketballmatching.user.service;


import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.dto.*;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceUnitTest {

    private static final Long USER_ID = 1L;

    private static final String EMAIL =
            "test@example.com";

    private static final String NICKNAME =
            "테스트회원";

    private static final String CURRENT_PASSWORD =
            "Current1234!";

    private static final String ENCODED_CURRENT_PASSWORD =
            "encoded-current-password";

    private static final String NEW_PASSWORD =
            "NewPassword1234!";

    private static final String ENCODED_NEW_PASSWORD =
            "encoded-new-password";

    private static final String EMAIL_VERIFIED_KEY =
            "email:auth:verified:" + EMAIL;

    private static final String PASSWORD_AUTH_KEY =
            "password:auth:" + EMAIL;

    private static final String PASSWORD_CHANGE_KEY =
            "password:change:" + EMAIL;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("이메일 인증 완료 후 회원가입 성공")
        void signupTest_success() {
            // given

            SignUpRequest request = createSignUpRequest(
                    EMAIL,
                    NICKNAME,
                    CURRENT_PASSWORD,
                    CURRENT_PASSWORD
            );

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(false);

            when(redisService.getData(EMAIL_VERIFIED_KEY))
                    .thenReturn("123456");

            when(passwordEncoder.encode(CURRENT_PASSWORD))
                    .thenReturn(ENCODED_CURRENT_PASSWORD);

            when(userRepository.save(any(UserEntity.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            // when

            SignUpResponse response = userService.signUp(request);

            // then

            verify(userRepository).existsByEmail(EMAIL);
            verify(userRepository).existsByNickname(NICKNAME);
            verify(redisService).getData(EMAIL_VERIFIED_KEY);
            verify(passwordEncoder).encode(CURRENT_PASSWORD);
            verify(userRepository).save(userCaptor.capture());
            verify(redisService).deleteData(EMAIL_VERIFIED_KEY);

            UserEntity savedUser = userCaptor.getValue();

            assertEquals(EMAIL, savedUser.getEmail());
            assertEquals(LoginProvider.LOCAL, savedUser.getLoginProvider());

            assertTrue(savedUser.isEmailAuth());

            assertEquals(EMAIL, response.email());
            assertEquals(NICKNAME, response.nickname());
            assertEquals("서울특별시 강남구", response.address());

        }

        @Test
        @DisplayName("이미 존재하는 이메일 예외발생")
        void signupTest_fail_alreadyExistsEmail() {
            // given

            SignUpRequest request = createDefaultSignUpRequest();

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(request));

            // then

            assertEquals(ALREADY_EXIST_EMAIL, exception.getErrorCode());

            /**
             * 이메일과 닉네임 존재 여부를 먼저 조회한 뒤 예외를 판단.
             */

            verifyNoInteractions(redisService, passwordEncoder);

            verify(userRepository, never()).save(any(UserEntity.class));

        }

        @Test
        @DisplayName("이미 존재하는 닉네임 예외 발생")
        void signupTest_fail_alreadyExistsNickname() {
            // given

            SignUpRequest request = createDefaultSignUpRequest();

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(request));

            // then

            assertEquals(ALREADY_EXIST_NICKNAME, exception.getErrorCode());

            verifyNoInteractions(redisService, passwordEncoder);

            verify(userRepository, never()).save(any(UserEntity.class));

        }

        @Test
        @DisplayName("비밀번호 확인 불일치 시 예외 발생")
        void signupTest_fail_passwordNotMatch() {
            // given

            SignUpRequest request = createSignUpRequest(
                    EMAIL, NICKNAME, CURRENT_PASSWORD, "DifferentPassword1234!"
            );

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(false);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(request));

            // then

            assertEquals(PASSWORD_NOT_MATCH, exception.getErrorCode());
            verifyNoInteractions(
                    redisService,
                    passwordEncoder
            );

            verify(
                    userRepository,
                    never()
            ).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("이메일 인증 정보 존재하지 않을 시 예외 발생")
        void signupTest_fail_emailIsNotVerified() {
            // given

            SignUpRequest request = createDefaultSignUpRequest();

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(false);

            when(redisService.getData(EMAIL_VERIFIED_KEY))
                    .thenReturn(null);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> userService.signUp(request));

            // then

            assertEquals(EMAIL_NOT_VERIFIED, exception.getErrorCode());

            verify(redisService).getData(EMAIL_VERIFIED_KEY);

            verify(redisService, never()).deleteData(EMAIL_VERIFIED_KEY);

            verify(userRepository, never()).save(any(UserEntity.class));

        }

    }

    @Nested
    @DisplayName("이메일 사용 가능 여부 확인")
    class CheckEmail {

        @Test
        @DisplayName("사용 가능한 이메일 정상 종료")
        void checkEmail_success() {
            // given

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(false);

            // when

            assertDoesNotThrow(() -> userService.checkEmail(EMAIL));

            // then

            verify(userRepository).existsByEmail(EMAIL);

        }

        @Test
        @DisplayName("이미 존재하는 이메일 예외 발생")
        void checkEmail_fail_alreadyExistsEmail() {
            // given

            when(userRepository.existsByEmail(EMAIL))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> userService.checkEmail(EMAIL));

            // then

            assertEquals(ALREADY_EXIST_EMAIL, exception.getErrorCode());

        }

    }

    @Nested
    @DisplayName("닉네임 사용 가능 여부 확인")
    class CheckNickname {

        @Test
        @DisplayName("사용 가능한 닉네임 정상 종료")
        void checkNickname_success() {

            // given
            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(false);

            // when & then
            assertDoesNotThrow(
                    () -> userService.checkNickname(NICKNAME)
            );

            verify(userRepository)
                    .existsByNickname(NICKNAME);
        }

        @Test
        @DisplayName("이미 존재하는 닉네임 예외 발생")
        void checkNickname_fail_alreadyExistsNickname() {

            // given
            when(userRepository.existsByNickname(NICKNAME))
                    .thenReturn(true);

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> userService.checkNickname(NICKNAME)
            );

            // then
            assertEquals(
                    ALREADY_EXIST_NICKNAME,
                    exception.getErrorCode()
            );
        }
    }

    @Nested
    @DisplayName("회원 정보 조회")
    class GetUserInfo {

        @Test
        @DisplayName("회원 조회 성공")
        void getUserInfo_success() {
            // given

            UserEntity user = createUser();

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            // when

            UserProfileResponse response = userService.getUserInfo(USER_ID);

            // then
            assertEquals(USER_ID, response.userId());
            assertEquals(EMAIL, response.email());
            assertEquals(NICKNAME, response.nickname());
            assertEquals(Position.GUARD, response.position());

            verify(userRepository)
                    .findByUserIdAndDeletedDateTimeIsNull(
                            USER_ID
                    );
        }

        @Test
        @DisplayName("회원 정보 조회 실패")
        void getUserInfo_fail_UserNotFound() {
            // given

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> userService.getUserInfo(USER_ID)
            );

            // then
            assertEquals(
                    USER_NOT_FOUND,
                    exception.getErrorCode()
            );

        }

    }

    @Nested
    @DisplayName("회원 정보 수정")
    class EditUserInfo {

        @Test
        @DisplayName("회원 정보 수정 성공")
        void editUserInfo_success() {
            // given

            UserEntity user = createUser();

            UpdateUserRequest request =
                    new UpdateUserRequest(
                            "변경닉네임",
                            "010-2222-3333",
                            "서울특별시 송파구",
                            GenderType.FEMALE,
                            Position.CENTER
                    );

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            when(userRepository.existsByNicknameAndUserIdNot("변경닉네임",USER_ID))
                    .thenReturn(false);


            // when

            UserProfileResponse response = userService.editUserInfo(USER_ID, request);


            // then

            assertEquals(
                    "변경닉네임",
                    user.getNickname()
            );

            assertEquals(
                    "010-2222-3333",
                    user.getPhone()
            );

            assertEquals(
                    "서울특별시 송파구",
                    user.getAddress()
            );

            assertEquals(
                    GenderType.FEMALE,
                    user.getGenderType()
            );

            assertEquals(
                    Position.CENTER,
                    user.getPosition()
            );

            assertEquals(
                    "변경닉네임",
                    response.nickname()
            );

            verify(userRepository)
                    .existsByNicknameAndUserIdNot(
                            "변경닉네임",
                            USER_ID
                    );

            /*
             * UserEntity가 영속 상태라는 전제로 Dirty Checking을
             * 사용하므로 editUserInfo는 save()를 호출하지 않는다.
             */
            verify(
                    userRepository,
                    never()
            ).save(any(UserEntity.class));

        }

        @Test
        @DisplayName("이미 사용중인 닉네임 예외 발생")
        void editUserInfo_fail_alreadyExistsNickname() {
            // given

            UserEntity user = createUser();

            UpdateUserRequest request =
                    new UpdateUserRequest(
                            "변경닉네임",
                            "010-2222-3333",
                            "서울특별시 송파구",
                            GenderType.FEMALE,
                            Position.CENTER
                    );

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            when(userRepository.existsByNicknameAndUserIdNot("변경닉네임", USER_ID))
                    .thenReturn(true);

            // when


            CustomException exception = assertThrows(CustomException.class, () -> userService.editUserInfo(USER_ID, request));

            // then
            assertEquals(
                    ALREADY_EXIST_NICKNAME,
                    exception.getErrorCode()
            );

            assertEquals(NICKNAME, user.getNickname());
            assertEquals(Position.GUARD, user.getPosition());

        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 인증번호 확인")
    class VerifyPasswordCode {

        @Test
        @DisplayName("인증 번호 일치 시 인증 정보 저장 성공")
        void verifyPasswordCode_success() {
            // given

            UserEntity user = createUser();

            String code = "12345";

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(redisService.getData(PASSWORD_AUTH_KEY))
                    .thenReturn(code);

            // when

            userService.verifyPasswordCode(
                    EMAIL, code
            );

            // then

            verify(redisService).getData(PASSWORD_AUTH_KEY);

            verify(redisService).setDataExpireMinutes(
                    PASSWORD_CHANGE_KEY,
                    code,
                    10L
            );


        }

        @Test
        @DisplayName("저장된 인증번호 존재하지 않을 시 예외발생")
        void verifyPasswordCode_fail_NotExistsCode() {
            // given

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(createUser()));

            when(redisService.getData(PASSWORD_AUTH_KEY))
                    .thenReturn(null);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> userService.verifyPasswordCode(EMAIL, "123456"));

            // then

            assertEquals(EMAIL_NOT_VERIFIED, exception.getErrorCode());

            verify(redisService, never()).setDataExpireMinutes(
                    anyString(),
                    anyString(),
                    anyLong()
            );


        }

    }

    @Nested
    @DisplayName("비밀번호 재설정")
    class ResetPassword {

        @Test
        @DisplayName("인증 완료 시 비밀번호 재설정 성공")
        void resetPassword_success() {
            // given

            UserEntity user = createUser();

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(redisService.getData(PASSWORD_CHANGE_KEY))
                    .thenReturn("verified");

            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD))
                    .thenReturn(false);

            when(passwordEncoder.encode(NEW_PASSWORD))
                    .thenReturn(ENCODED_NEW_PASSWORD);

            // when

            userService.resetPassword(EMAIL, NEW_PASSWORD, NEW_PASSWORD);

            // then
            assertEquals(
                    ENCODED_NEW_PASSWORD,
                    user.getPassword()
            );

            verify(passwordEncoder).encode(NEW_PASSWORD);
            verify(redisService).deleteData(PASSWORD_CHANGE_KEY);

            verify(
                    userRepository,
                    never()
            ).save(any(UserEntity.class));


        }

        @Test
        @DisplayName("새 비밀번호 확인 일치하지 않을 시 예외 발생")
        void resetPassword_fail_passwordNotMatch() {
            // given

            when(
                    userRepository
                            .findByEmailAndDeletedDateTimeIsNull(
                                    EMAIL
                            )
            ).thenReturn(Optional.of(createUser()));

            when(redisService.getData(PASSWORD_CHANGE_KEY))
                    .thenReturn("verified");

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> userService.resetPassword(
                            EMAIL,
                            NEW_PASSWORD,
                            "DifferentPassword1234!"
                    )
            );

            // then
            assertEquals(
                    PASSWORD_NOT_MATCH,
                    exception.getErrorCode()
            );

            verifyNoInteractions(passwordEncoder);

            verify(
                    redisService,
                    never()
            ).deleteData(PASSWORD_CHANGE_KEY);

        }

        @Test
        @DisplayName("기존 비밀번호와 동일하면 예외 발생")
        void resetPassword_fail_sameAsOldPassword() {
            // given

            UserEntity user = createUser();

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(redisService.getData(PASSWORD_CHANGE_KEY))
                    .thenReturn("verified");

            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD))
                    .thenReturn(true);

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> userService.resetPassword(
                            EMAIL,
                            CURRENT_PASSWORD,
                            CURRENT_PASSWORD
                    )
            );

            // then
            assertEquals(
                    SAME_AS_OLD_PASSWORD,
                    exception.getErrorCode()
            );

            verify(
                    passwordEncoder,
                    never()
            ).encode(anyString());

            verify(
                    redisService,
                    never()
            ).deleteData(PASSWORD_CHANGE_KEY);

            assertEquals(
                    ENCODED_CURRENT_PASSWORD,
                    user.getPassword()
            );

        }
    }

    private SignUpRequest createDefaultSignUpRequest() {

        return createSignUpRequest(
                EMAIL,
                NICKNAME,
                CURRENT_PASSWORD,
                CURRENT_PASSWORD
        );
    }

    @Nested
    @DisplayName("로그인 사용자 비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("로그인 사용자 비밀번호 변경 성공")
        void changePassword_success() {
            // given

            UserEntity user = createUser();

            ChangePasswordRequest request =
                    createChangePasswordRequest(
                            CURRENT_PASSWORD,
                            NEW_PASSWORD,
                            NEW_PASSWORD
                    );

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD))
                    .thenReturn(true);

            when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_CURRENT_PASSWORD))
                    .thenReturn(false);

            when(passwordEncoder.encode(NEW_PASSWORD))
                    .thenReturn(ENCODED_NEW_PASSWORD);

            // when

            userService.changePassword(USER_ID, request);

            // then
            assertEquals(
                    ENCODED_NEW_PASSWORD,
                    user.getPassword()
            );

            verify(passwordEncoder).encode(NEW_PASSWORD);

            verify(
                    userRepository,
                    never()
            ).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않을 시 예외 발생")
        void changePassword_fail_currentPasswordIsWrong() {

            // given
            UserEntity user = createUser();

            ChangePasswordRequest request =
                    createChangePasswordRequest(
                            "WrongPassword1234!",
                            NEW_PASSWORD,
                            NEW_PASSWORD
                    );

            when(
                    userRepository
                            .findByUserIdAndDeletedDateTimeIsNull(
                                    USER_ID
                            )
            ).thenReturn(Optional.of(user));

            when(
                    passwordEncoder.matches(
                            "WrongPassword1234!",
                            ENCODED_CURRENT_PASSWORD
                    )
            ).thenReturn(false);

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> userService.changePassword(
                            USER_ID,
                            request
                    )
            );

            // then
            assertEquals(
                    PASSWORD_NOT_MATCH,
                    exception.getErrorCode()
            );

            verify(
                    passwordEncoder,
                    never()
            ).encode(anyString());

            assertEquals(
                    ENCODED_CURRENT_PASSWORD,
                    user.getPassword()
            );
        }

        @Test
        @DisplayName("새 비밀번호 확인이 일치하지 않을 시 예외 발생")
        void changePassword_fail_passwordNotMatch() {

            // given
            UserEntity user = createUser();

            ChangePasswordRequest request =
                    createChangePasswordRequest(
                            CURRENT_PASSWORD,
                            NEW_PASSWORD,
                            "DifferentPassword1234!"
                    );

            when(
                    userRepository
                            .findByUserIdAndDeletedDateTimeIsNull(
                                    USER_ID
                            )
            ).thenReturn(Optional.of(user));

            when(
                    passwordEncoder.matches(
                            CURRENT_PASSWORD,
                            ENCODED_CURRENT_PASSWORD
                    )
            ).thenReturn(true);

            // when
            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> userService.changePassword(
                            USER_ID,
                            request
                    )
            );

            // then
            assertEquals(
                    PASSWORD_NOT_MATCH,
                    exception.getErrorCode()
            );

            verify(
                    passwordEncoder,
                    never()
            ).matches(
                    NEW_PASSWORD,
                    ENCODED_CURRENT_PASSWORD
            );

            verify(
                    passwordEncoder,
                    never()
            ).encode(anyString());

            assertEquals(
                    ENCODED_CURRENT_PASSWORD,
                    user.getPassword()
            );
        }


    }

    private SignUpRequest createSignUpRequest(
            String email,
            String nickname,
            String password,
            String checkPassword
    ) {

        return new SignUpRequest(
                email,
                password,
                checkPassword,
                nickname,
                "테스트 사용자",
                LocalDate.of(1997, 1, 1),
                "010-1111-0000",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE
        );
    }


    private ChangePasswordRequest createChangePasswordRequest(
            String currentPassword,
            String newPassword,
            String newCheckPassword
    ) {
        return new ChangePasswordRequest(
                currentPassword,
                newPassword,
                newCheckPassword
        );
    }

    private UserEntity createUser() {

        return UserEntity.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .password(ENCODED_CURRENT_PASSWORD)
                .nickname(NICKNAME)
                .name("테스트 사용자")
                .birth(LocalDate.of(1997, 1, 1))
                .phone("010-1111-0000")
                .address("서울특별시 강남구")
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .userType(UserType.USER)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();
    }


}
