package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.blacklist.service.BlackListStore;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceUnitTest {

    private static final Long USER_ID = 1L;

    private static final String EMAIL =
            "test@test.com";

    private static final String NAME =
            "테스트회원";

    private static final String NICKNAME =
            "테스트";

    private static final String PASSWORD =
            "Password1234!";

    private static final String ENCODED_PASSWORD =
            "encoded-password";

    private static final String ACCESS_TOKEN =
            "access-token";

    private static final String REFRESH_TOKEN =
            "refresh-token";

    private static final String NEW_REFRESH_TOKEN =
            "new-refresh-token";

    private static final String OTHER_REFRESH_TOKEN =
            "other-refresh-token";

    private static final long REFRESH_TOKEN_EXPIRATION =
            1_209_600_000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthTokenStore authTokenStore;

    @Mock
    private UserSessionRevocationService
            userSessionRevocationService;

    @Mock
    private BlackListStore blackListStore;

    @InjectMocks
    private AuthService authService;



    @Nested
    @DisplayName("로컬 로그인")
    class LocalLogin {

        @Test
        @DisplayName("로그인 성공 시 토큰 발급 및 RefreshToken 저장")
        void login_success() {
            // given

            UserEntity user = createUser(LoginProvider.LOCAL);

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                    .thenReturn(true);

            stubTokenIssuance();

            // when

            AuthTokenResponse response = authService.login(EMAIL, PASSWORD);

            // then

            assertAll(
                    () -> assertEquals(ACCESS_TOKEN, response.accessToken()),
                    () -> assertEquals(REFRESH_TOKEN, response.refreshToken()),
                    () -> assertEquals(USER_ID, response.user().userId()),
                    () -> assertEquals(EMAIL, response.user().email())
            );

            verify(blackListStore).isBlacklisted(EMAIL);
            verify(authTokenStore).saveRefreshToken(EMAIL, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION);

        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외 발생")
        void login_fail_passwordNotMatch() {
            // given

            UserEntity user = createUser(LoginProvider.LOCAL);

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                    .thenReturn(false);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> authService.login(EMAIL, PASSWORD));

            // then

            assertEquals(PASSWORD_NOT_MATCH, exception.getErrorCode());

            verifyNoInteractions(blackListStore, tokenProvider, authTokenStore);

        }

        @Test
        @DisplayName("카카오 가입 유저 접근 시 예외 발생")
        void login_fail_providerNotMatch() {
            // given

            UserEntity user = createUser(LoginProvider.KAKAO);

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            // when

            CustomException exception = assertThrows(CustomException.class, () -> authService.login(EMAIL, PASSWORD));
            // then

            assertEquals(PROVIDER_NOT_MATCH, exception.getErrorCode());

            verifyNoInteractions(passwordEncoder, blackListStore, authTokenStore, tokenProvider);

        }

        @Test
        @DisplayName("블랙리스트 유저 접근 시 예외 발생")
        void login_fail_blacklistedUser() {
            // given

            UserEntity user = createUser(LoginProvider.LOCAL);

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                    .thenReturn(true);

            when(blackListStore.isBlacklisted(EMAIL))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> authService.login(EMAIL, PASSWORD));

            // then

            assertEquals(BLACKLIST_USER, exception.getErrorCode());
            verify(blackListStore).isBlacklisted(EMAIL);

            verifyNoInteractions(tokenProvider, authTokenStore);

        }

    }

    @Nested
    @DisplayName("카카오 로그인")
    class KakaoLogin {
        @Test
        @DisplayName("카카오 로그인 성공")
        void loginWithKakao_success() {
            // given

            UserEntity user = createUser(LoginProvider.KAKAO);

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            stubTokenIssuance();

            // when

            AuthTokenResponse response = authService.loginWithKakao(EMAIL);

            // then

            assertEquals(ACCESS_TOKEN, response.accessToken());

            assertEquals(REFRESH_TOKEN, response.refreshToken());

            verifyNoInteractions(passwordEncoder);

            verify(authTokenStore).saveRefreshToken(EMAIL, REFRESH_TOKEN, REFRESH_TOKEN_EXPIRATION);

        }
    }

    @Nested
    @DisplayName("토큰 재발급 및 로그아웃")
    class ReissueAndLogout {

        @Test
        @DisplayName("재발급 성공 시 새로운 토큰 쌍을 반환한다")
        void reissue_success() {
            // given

            Date expiresAt = stubReissue(true);

            // when

            AuthTokenResponse response = authService.reissue(REFRESH_TOKEN);

            // then

            assertAll(
                    () -> assertEquals(
                            ACCESS_TOKEN,
                            response.accessToken()
                    ),
                    () -> assertEquals(
                            NEW_REFRESH_TOKEN,
                            response.refreshToken()
                    )
            );
            verify(authTokenStore).rotateRefreshToken(
                    EMAIL,
                    REFRESH_TOKEN,
                    NEW_REFRESH_TOKEN,
                    expiresAt.getTime()
            );

            verify(tokenProvider).createRefreshToken(EMAIL, expiresAt);

            verify(tokenProvider, never()).createRefreshToken(EMAIL);

            verify(authTokenStore, never())
                    .saveRefreshToken(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("Redis 토큰 교체에 실패하면 재발급을 거절한다")
        void reissue_fail_tokenNotMatch() {
            Date expiresAt = stubReissue(false);

            CustomException exception = assertThrows(
                    CustomException.class,
                    () -> authService.reissue(REFRESH_TOKEN)
            );

            assertEquals(INVALID_TOKEN, exception.getErrorCode());

            verify(authTokenStore).rotateRefreshToken(
                    EMAIL,
                    REFRESH_TOKEN,
                    NEW_REFRESH_TOKEN,
                    expiresAt.getTime()
            );
        }


        @Test
        @DisplayName("로그아웃 성공")
        void logout_success() {
            // given


            // when

            authService.logoutUser(EMAIL, ACCESS_TOKEN);

            // then

            verify(userSessionRevocationService)
                    .revokeAll(EMAIL, ACCESS_TOKEN);

        }

    }

    private void stubTokenIssuance() {
        when(tokenProvider.createAccessToken(EMAIL, NAME, UserType.USER))
                .thenReturn(ACCESS_TOKEN);

        when(tokenProvider.createRefreshToken(EMAIL))
                .thenReturn(REFRESH_TOKEN);

        when(tokenProvider.getRefreshTokenExpirationMillis())
                .thenReturn(REFRESH_TOKEN_EXPIRATION);
    }


    private UserEntity createUser(
            LoginProvider loginProvider
    ) {
        return UserEntity.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .nickname(NICKNAME)
                .name(NAME)
                .birth(LocalDate.of(
                        1995,
                        1,
                        1
                ))
                .phone("010-1234-5678")
                .address("서울특별시")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(loginProvider)
                .build();
    }


    private Date stubReissue(boolean rotationSuccess) {

        Date expiresAt = Date.from(
                java.time.Instant.parse("2030-01-01T00:00:00Z")
        );

        Claims claims = Jwts.claims()
                .setSubject(EMAIL)
                .setExpiration(expiresAt);

        when(tokenProvider.validateRefreshToken(REFRESH_TOKEN))
                .thenReturn(claims);

        when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                .thenReturn(Optional.of(createUser(LoginProvider.LOCAL)));

        when(tokenProvider.createAccessToken(EMAIL, NAME, UserType.USER))
                .thenReturn(ACCESS_TOKEN);

        when(tokenProvider.createRefreshToken(EMAIL, expiresAt))
                .thenReturn(NEW_REFRESH_TOKEN);

        when(authTokenStore.rotateRefreshToken(
                EMAIL,
                REFRESH_TOKEN,
                NEW_REFRESH_TOKEN,
                expiresAt.getTime()
        )).thenReturn(rotationSuccess);

        return expiresAt;

    }
}
