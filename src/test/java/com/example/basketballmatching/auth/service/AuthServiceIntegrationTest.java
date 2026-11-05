package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import com.example.basketballmatching.blacklist.repository.BlackListRepository;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.type.CityName;
import com.example.basketballmatching.game.type.FieldStatus;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.type.ReportType;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
@Transactional
@DisplayName("AuthService 통합 테스트")
class AuthServiceIntegrationTest {

    private static final String EMAIL =
            "auth-integration@test.com";

    private static final String RAW_PASSWORD =
            "Password1234!";

    private static final String NICKNAME =
            "인증통합회원";

    private static final String REFRESH_TOKEN_PREFIX =
            "refreshToken:";

    private static final String REVOKED_ACCESS_TOKEN_PREFIX =
            "logout:access:";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private RedisService redisService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private BlackListRepository blackListRepository;

    @Autowired
    private Clock clock;

    private final Set<String> createdRedisKeys = new HashSet<>();
    @Autowired
    private AuthTokenStore authTokenStore;

    @AfterEach
    void clearRedis() {
        createdRedisKeys.forEach(
                redisService::deleteData
        );

        createdRedisKeys.clear();
    }


    @Test
    @DisplayName("로그인 성공 JWT 토큰 발급 및 RefreshToken 저장")
    void login_success() {
        // given
        UserEntity user = saveLocalUser();

        String refreshTokenKey = refreshTokenKey();

        createdRedisKeys.add(refreshTokenKey);



        // when

        AuthTokenResponse response = authService.login(EMAIL, RAW_PASSWORD);

        // then

        assertAll(
                () -> assertNotNull(response.refreshToken()),
                () -> assertNotNull(response.accessToken()),
                () -> assertEquals(user.getUserId(), response.user().userId()),
                () -> assertEquals(EMAIL, response.user().email())

        );

        assertDoesNotThrow(() -> tokenProvider.validateAccessToken(response.accessToken()));
        assertDoesNotThrow(() -> tokenProvider.validateRefreshToken(response.refreshToken()));

        assertEquals(EMAIL, tokenProvider.getEmailFromToken(response.accessToken()));

        assertEquals(response.refreshToken(), redisService.getData(refreshTokenKey));

        Long expiration = redisService.getExpiration(refreshTokenKey);

        assertNotNull(expiration);
        assertTrue(expiration > 0);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() {
        // given

        saveLocalUser();

        AuthTokenResponse loginResponse = login();

        Claims previousClaims = tokenProvider.validateRefreshToken(loginResponse.refreshToken());

        AuthTokenResponse response = authService.reissue(loginResponse.refreshToken());

        Claims newClaims = tokenProvider.validateRefreshToken(response.refreshToken());

        assertAll(
                () -> assertNotEquals(loginResponse.refreshToken(), response.refreshToken()),
                () -> assertNotNull(newClaims.getId()),
                () -> assertNotEquals(previousClaims.getId(), newClaims.getId()),
                () -> assertEquals(previousClaims.getExpiration(), newClaims.getExpiration()),
                () -> assertEquals(response.refreshToken(), redisService.getData(refreshTokenKey())),
                () -> assertDoesNotThrow(
                        () -> tokenProvider.validateAccessToken(response.accessToken())
                )
        );

        long expectedRemainingSeconds = (newClaims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

        Long redisTtlSeconds = redisService.getExpiration(refreshTokenKey());

        assertNotNull(redisTtlSeconds);

        assertTrue(redisTtlSeconds > 0);

        assertTrue(Math.abs(redisTtlSeconds - expectedRemainingSeconds) <= 2);


        AuthTokenResponse nextResponse =
                authService.reissue(response.refreshToken());

        assertNotEquals(
                response.refreshToken(),
                nextResponse.refreshToken()
        );

        assertEquals(
                nextResponse.refreshToken(),
                redisService.getData(refreshTokenKey())
        );
    }

    @Test
    @DisplayName("교체된 이전 Refresh는 재사용할 수 없다")
    void reissue_fail_reusedRefreshToken() {
        saveLocalUser();

        AuthTokenResponse loginResponse = login();

        AuthTokenResponse rotatedResponse = authService.reissue(
                loginResponse.refreshToken()
        );

        CustomException exception = assertThrows(
                CustomException.class,
                () -> authService.reissue(
                        loginResponse.refreshToken()
                )
        );

        assertEquals(
                ErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );

        // 실패한 이전 토큰 요청이 현재 토큰을 변경하면 안 된다.
        assertEquals(
                rotatedResponse.refreshToken(),
                redisService.getData(refreshTokenKey())
        );
    }

    @Test
    @DisplayName("Redis 키가 삭제되면 재발급을 거절하고 키를 재생성하지 않는다")
    void reissue_fail_missingRefreshToken() {
        saveLocalUser();

        AuthTokenResponse loginResponse = login();

        redisService.deleteData(refreshTokenKey());

        CustomException exception = assertThrows(
                CustomException.class,
                () -> authService.reissue(
                        loginResponse.refreshToken()
                )
        );

        assertEquals(
                ErrorCode.INVALID_TOKEN,
                exception.getErrorCode()
        );

        assertNull(redisService.getData(refreshTokenKey()));
    }

    @Test
    @DisplayName("같은 Refresh Token의 동시 교체는 정확히 하나만 성공한다.")
    void rotateRefreshToken_concurrent() throws Exception{

        String email = "rotation-" + UUID.randomUUID() + "@test.com";

        String key = REFRESH_TOKEN_PREFIX + email;

        createdRedisKeys.add(key);

        String currentToken = tokenProvider.createRefreshToken(email);

        Date expiresAt = tokenProvider.validateRefreshToken(currentToken).getExpiration();

        String candidateA = tokenProvider.createRefreshToken(email, expiresAt);
        String candidateB = tokenProvider.createRefreshToken(email, expiresAt);

        assertNotEquals(candidateA, candidateB);

        authTokenStore.saveRefreshToken(email, currentToken, expiresAt.getTime() - System.currentTimeMillis());


        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> resultA = executor.submit(() -> {
                ready.countDown();

                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("시작 신호 대기 초과");
                }

                return authTokenStore.rotateRefreshToken(email, currentToken, candidateA, expiresAt.getTime());
            });

            Future<Boolean> resultB = executor.submit(() -> {
                ready.countDown();

                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("시작 신호 대기 초과");
                }

                return authTokenStore.rotateRefreshToken(
                        email,
                        currentToken,
                        candidateB,
                        expiresAt.getTime()
                );
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));

            start.countDown();

            boolean successA = resultA.get(10, TimeUnit.SECONDS);
            boolean successB = resultB.get(10, TimeUnit.SECONDS);

            int successCount = (successA ? 1 : 0) + (successB ? 1 : 0);

            assertEquals(1, successCount);

            String expectedToken = successA ? candidateA : candidateB;

            assertEquals(expectedToken, redisService.getData(key));

            assertFalse(authTokenStore.rotateRefreshToken(
                    email, currentToken, candidateA, expiresAt.getTime()
            ));
        } finally {
            start.countDown();
            executor.shutdown();

            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("로그아웃 성공 RefreshToken 제거 및 AccessToken 블랙리스트 처리")
    void logout_success() {
        // given

        UserEntity user = saveLocalUser();

        AuthTokenResponse loginResponse = login();

        String refreshTokenKey = refreshTokenKey();

        String revokedAccessTokenKey = REVOKED_ACCESS_TOKEN_PREFIX + loginResponse.accessToken();

        createdRedisKeys.add(revokedAccessTokenKey);


        // when

        authService.logoutUser(EMAIL, loginResponse.accessToken());

        // then

        assertNull(redisService.getData(refreshTokenKey));

        assertEquals("LOGOUT", redisService.getData(revokedAccessTokenKey));

        Long expiration = redisService.getExpiration(revokedAccessTokenKey);

        assertNotNull(expiration);
        assertTrue(expiration > 0);

    }

    @Test
    @DisplayName("블랙리스트 유저 로그인 접근 시 예외 발생")
    void login_fail_blacklistedUser() {
        // given

        UserEntity targetUser = saveLocalUser();

        String refreshTokenKey = refreshTokenKey();

        createdRedisKeys.add(refreshTokenKey);

        saveActiveBlackList(targetUser);

        // when

        CustomException exception = assertThrows(CustomException.class, () -> authService.login(EMAIL, RAW_PASSWORD));

        // then

        assertEquals(ErrorCode.BLACKLIST_USER, exception.getErrorCode());

        assertNull(redisService.getData(refreshTokenKey));

    }

    private AuthTokenResponse login() {
        String refreshTokenKey = refreshTokenKey();

        createdRedisKeys.add(refreshTokenKey);

        return authService.login(EMAIL, RAW_PASSWORD);
    }

    private UserEntity saveLocalUser() {

        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);

        UserEntity user = UserEntity.create(
                EMAIL,
                encodedPassword,
                NICKNAME,
                "인증통합회원",
                LocalDate.of(1995, 01, 01),
                "010-1234-5678",
                "서울특별시",
                Position.GUARD,
                GenderType.MALE

        );

        return userRepository.saveAndFlush(user);
    }

    private String refreshTokenKey() {
        return REFRESH_TOKEN_PREFIX + EMAIL;
    }

    private void saveActiveBlackList(UserEntity targetUser) {
        LocalDateTime now = LocalDateTime.now(clock);

        UserEntity admin = userRepository.saveAndFlush(
                UserEntity.create(
                        "blacklist-admin@test.com",
                        passwordEncoder.encode(RAW_PASSWORD),
                        "블랙리스트관리자",
                        "관리자",
                        LocalDate.of(1990, 1, 1),
                        "010-9999-9999",
                        "서울특별시",
                        Position.GUARD,
                        GenderType.MALE
                )
        );

        GameEntity game = gameRepository.saveAndFlush(
                GameEntity.create(
                        "블랙리스트 통합 테스트 경기",
                        "블랙리스트 로그인 차단 검증용 경기",
                        6,
                        FieldStatus.INDOOR,
                        MatchFormat.THREE_ON_THREE,
                        MatchGenderType.MIXED,
                        now.plusDays(2),
                        now.plusDays(2).plusHours(2),
                        "테스트 농구장",
                        "서울특별시 송파구",
                        CityName.SEOUL,
                        37.5,
                        127.0,
                        admin,
                        now
                )
        );

        ReportEntity report = ReportEntity.create(
                admin,
                targetUser,
                game,
                ReportType.POOR_SPORTSMANSHIP,
                "블랙리스트 로그인 차단 검증용 신고",
                now
        );

        report.approve(
                admin,
                "신고 승인",
                now
        );

        reportRepository.saveAndFlush(report);

        blackListRepository.saveAndFlush(
                BlackListEntity.create(
                        report,
                        admin,
                        now,
                        now.plusDays(7)
                )
        );
    }


}
