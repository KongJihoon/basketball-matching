package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.support.IntegrationTestSupport;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.dto.SignUpRequest;
import com.example.basketballmatching.user.dto.SignUpResponse;
import com.example.basketballmatching.user.dto.UpdateUserRequest;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@DisplayName("UserService 통합 테스트")
class UserServiceIntegrationTest extends IntegrationTestSupport {

    private static final String EMAIL_VERIFIED_PREFIX =
            "email:auth:verified:";

    private static final String PASSWORD_AUTH_PREFIX =
            "password:auth:";

    private static final String PASSWORD_CHANGE_PREFIX =
            "password:change:";


    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    /**
     * DB데이터는 @Transactional로 롤백되지만
     * Redis 데이터는 롤백 되지않는다.
     */

    private final Set<String> redisKey = new HashSet<>();

    @AfterEach
    void cleanRedis() {
        redisKey.forEach(redisService::deleteData);
        redisKey.clear();
    }

    @Test
    @DisplayName("이메일 인증 후 회원가입 성공")
    void signUp_success() {
        // given

        String email = "signup@test.com";

        String verifiedKey = EMAIL_VERIFIED_PREFIX + email;

        saveRedis(verifiedKey, "123456", 10L);

        SignUpRequest request = new SignUpRequest(
                email,
                "Signup1234!",
                "Signup1234!",
                "testNickname",
                "테스트 사용자",
                LocalDate.of(1997, 1, 1),
                "010-1111-0000",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE
        );

        // when

        SignUpResponse response = userService.signUp(request);

        /**
         * 영속성 컨택스트의 캐시를 비운 후
         * 실제 DB에서 다시 조회
         */
        flushAndClear();
        // then

        UserEntity savedUser = userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow();

        assertEquals(email, response.email());

        assertEquals("testNickname", response.nickname());

        assertNotNull(response.createdAt());

        assertEquals(email, savedUser.getEmail());

        assertEquals(LoginProvider.LOCAL, savedUser.getLoginProvider());

        assertEquals(
                UserType.USER,
                savedUser.getUserType()
        );

        assertTrue(savedUser.isEmailAuth());

        assertTrue(
                passwordEncoder.matches(
                        "Signup1234!",
                        savedUser.getPassword()
                )
        );

        /**
         * JPA Auditing 확인
         */
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());

        /**
         * 회원가입에 사용한 이메일 인증 완료 키는 일회용이므로 삭제되어야 한다.
         */
        assertNull(redisService.getData(verifiedKey));
    }

    @Test
    @DisplayName("탈퇴 회원 조회 시 예외 발생")
    void getUserInfo_fail_withdrawnUser() {
        // given

        UserEntity user = saveUser(
                "withdrawn@test.com",
                "탈퇴회원",
                "Password1234!"
        );

        Long userId = user.getUserId();

        user.withdraw(LocalDateTime.of(2026, 1, 10, 12, 0));

        flushAndClear();
        // when

        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserInfo(userId));


        // then

        assertEquals(USER_NOT_FOUND, exception.getErrorCode());

        assertTrue(
                userRepository.findByUserIdAndDeletedDateTimeIsNull(userId).isEmpty()
        );

    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    void editUserInfo_success() {
        // given

        UserEntity user = saveUser(
                "edit@test.com",
                "수정전닉네임",
                "Password1234!"
        );

        Long userId = user.getUserId();

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "변경닉네임",
                        "010-2222-3333",
                        "서울특별시 송파구",
                        GenderType.FEMALE,
                        Position.CENTER
                );


        // when

        userService.editUserInfo(
                userId, request
        );

        flushAndClear();

        // then

        UserEntity updatedUser = userRepository.findByUserIdAndDeletedDateTimeIsNull(userId).orElseThrow();


        assertEquals(
                "변경닉네임",
                updatedUser.getNickname()
        );

        assertEquals(
                "010-2222-3333",
                updatedUser.getPhone()
        );

        assertEquals(
                "서울특별시 송파구",
                updatedUser.getAddress()
        );

        assertEquals(
                GenderType.FEMALE,
                updatedUser.getGenderType()
        );

        assertEquals(
                Position.CENTER,
                updatedUser.getPosition()
        );
    }


    @Test
    @DisplayName("인증버호 인증 시 비밀번호 변경 권한 획득 및 TTL 저장")
    void verifyPasswordCode_success() {
        // given

        String email = "verify@test.com";
        String code = "123456";

        saveUser(
                email,
                "인증회원",
                "Password1234!"
        );

        String authKey =
                PASSWORD_AUTH_PREFIX + email;

        String changeKey =
                PASSWORD_CHANGE_PREFIX + email;

        saveRedis(
                authKey,
                code,
                3L
        );

        redisKey.add(changeKey);

        // when

        userService.verifyPasswordCode(email, code);

        // then

        assertEquals(code, redisService.getData(changeKey));

        Long expiration = redisService.getExpiration(changeKey);

        assertNotNull(expiration);

        assertTrue(expiration > 0);

    }

    @Test
    @DisplayName("비밀번호 재설정 성공")
    void resetPassword_success() {
        // given
        String email = "reset@test.com";

        String oldPassword =
                "OldPassword1234!";

        String newPassword =
                "NewPassword1234!";

        saveUser(
                email,
                "재설정회원",
                oldPassword
        );

        String changeKey =
                PASSWORD_CHANGE_PREFIX + email;

        saveRedis(
                changeKey,
                "verified",
                10L
        );


        // when

        userService.resetPassword(email, newPassword, newPassword);

        flushAndClear();
        // then

        UserEntity updatedUser = userRepository.findByEmailAndDeletedDateTimeIsNull(email).orElseThrow();

        assertTrue(
                passwordEncoder.matches(
                        newPassword,
                        updatedUser.getPassword()
                )
        );

        assertFalse(
                passwordEncoder.matches(
                        oldPassword,
                        updatedUser.getPassword()
                )
        );

        assertNull(
                redisService.getData(changeKey)
        );


    }


    private UserEntity saveUser(String email, String nickname, String rawPassword) {

        UserEntity user = UserEntity.builder()
                .email(email)
                .password(
                        passwordEncoder.encode(rawPassword)
                )
                .nickname(nickname)
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

        return userRepository.saveAndFlush(user);

    }

    private void saveRedis(String key, String value, Long expirationMinutes) {
        redisService.setDataExpireMinutes(
                key, value, expirationMinutes
        );

        redisKey.add(key);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }


}
