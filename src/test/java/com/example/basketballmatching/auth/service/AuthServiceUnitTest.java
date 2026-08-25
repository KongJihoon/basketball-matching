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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        @DisplayName("저장된 RefreshToken 일치 시 AccessToken 재발급")
        void reissue_success() {
            // given

            UserEntity user = createUser(LoginProvider.LOCAL);

            when(tokenProvider.getEmailFromToken(REFRESH_TOKEN))
                    .thenReturn(EMAIL);

            when(authTokenStore.getRefreshToken(EMAIL))
                    .thenReturn(REFRESH_TOKEN);

            when(userRepository.findByEmailAndDeletedDateTimeIsNull(EMAIL))
                    .thenReturn(Optional.of(user));

            when(tokenProvider.createAccessToken(EMAIL, NAME, UserType.USER))
                    .thenReturn(ACCESS_TOKEN);
            // when

            AuthTokenResponse response = authService.reissue(REFRESH_TOKEN);

            // then

            verify(tokenProvider).validateRefreshToken(REFRESH_TOKEN);

            assertEquals(ACCESS_TOKEN, response.accessToken());
            assertEquals(REFRESH_TOKEN, response.refreshToken());

            verify(tokenProvider, never()).createRefreshToken(EMAIL);

            verify(authTokenStore, never()).saveRefreshToken(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("저장된 RefreshToken이 일치하지 않으면 예외 발생")
        void reissue_fail_tokenNotMatch() {
            // given

            when(tokenProvider.getEmailFromToken(REFRESH_TOKEN))
                    .thenReturn(EMAIL);

            when(authTokenStore.getRefreshToken(EMAIL))
                    .thenReturn(OTHER_REFRESH_TOKEN);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> authService.reissue(REFRESH_TOKEN));

            // then

            assertEquals(INVALID_TOKEN, exception.getErrorCode());

            verify(tokenProvider).validateRefreshToken(REFRESH_TOKEN);

            verifyNoInteractions(userRepository);

            verify(tokenProvider, never()).createAccessToken(anyString(), anyString(), any(UserType.class));

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
}
