package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.oauth2.client.KakaoOAuthClient;
import com.example.basketballmatching.auth.oauth2.client.dto.KakaoUserInfoResponse;
import com.example.basketballmatching.auth.oauth2.dto.*;
import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static com.example.basketballmatching.auth.oauth2.type.OAuthFlowType.*;
import static com.example.basketballmatching.auth.oauth2.type.OAuthProvider.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthService 단위 테스트")
class OAuthServiceUnitTest {

    private static final String AUTHORIZATION_CODE = "kakao-authorization-code";

    private static final Long KAKAO_USER_ID = 123456789L;

    private static final String PROVIDER_USER_ID = String.valueOf(KAKAO_USER_ID);

    private static final String EMAIL = "test@example.com";

    private static final String NICKNAME = "커리";

    private static final String TICKET = "oauth-ticket";

    private static final String ACCESS_TOKEN = "access-token";

    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private OAuthAccountService oAuthAccountService;

    @Mock
    private OAuthTicketStore oAuthTicketStore;

    @Mock
    private AuthService authService;

    @InjectMocks
    private OAuthService oAuthService;

    @Nested
    @DisplayName("카카오 Callback")
    class KakaoCallback {

        @Test
        @DisplayName("기존 OAuth 회원 LOGIN TICKET 발급")
        void KakaoCallback_success_login() {
            // given

            KakaoUserInfoResponse userInfo = createKakaoUserInfo(null);

            when(kakaoOAuthClient.getUserInfo(AUTHORIZATION_CODE))
                    .thenReturn(userInfo);

            when(oAuthAccountService.resolveKakaoAccount(KAKAO_USER_ID, EMAIL))
                    .thenReturn(OAuthAccountDecision.login(EMAIL));

            when(oAuthTicketStore.issue(any(OAuthTicketPayload.class)))
                    .thenReturn(TICKET);


            ArgumentCaptor<OAuthTicketPayload> payloadCaptor = ArgumentCaptor.forClass(OAuthTicketPayload.class);
            // when

            OAuthCallbackResponse response = oAuthService.kakaoCallback(AUTHORIZATION_CODE);

            // then

            verify(oAuthTicketStore).issue(payloadCaptor.capture());

            OAuthTicketPayload payload = payloadCaptor.getValue();

            assertAll(
                    () -> assertEquals(LOGIN, response.flowType()),
                    () -> assertEquals(TICKET, response.ticket()),
                    () -> assertNull(response.email()),
                    () -> assertNull(response.nickname()),
                    () -> assertEquals(LOGIN, payload.flowType()),
                    () -> assertEquals(EMAIL, payload.email())
            );

        }

        @Test
        @DisplayName("신규 OAuth 회원 SIGNUP TICKET 발급")
        void kakaoCallback_success_signup() {
            // given

            KakaoUserInfoResponse userInfo = createKakaoUserInfo(NICKNAME);

            when(kakaoOAuthClient.getUserInfo(AUTHORIZATION_CODE))
                    .thenReturn(userInfo);

            when(oAuthAccountService.resolveKakaoAccount(KAKAO_USER_ID, EMAIL))
                    .thenReturn(OAuthAccountDecision.signup(EMAIL));

            when(oAuthTicketStore.issue(any(OAuthTicketPayload.class)))
                    .thenReturn(TICKET);

            ArgumentCaptor<OAuthTicketPayload> payloadCaptor = ArgumentCaptor.forClass(OAuthTicketPayload.class);

            // when

            OAuthCallbackResponse response = oAuthService.kakaoCallback(AUTHORIZATION_CODE);

            // then

            verify(oAuthTicketStore).issue(payloadCaptor.capture());

            OAuthTicketPayload payload = payloadCaptor.getValue();

            assertAll(
                    () -> assertEquals(SIGNUP, response.flowType()),
                    () -> assertEquals(TICKET, response.ticket()),
                    () -> assertEquals(
                            EMAIL,
                            response.email()
                    ),
                    () -> assertEquals(
                            NICKNAME,
                            response.nickname()
                    ),
                    () -> assertEquals(
                            SIGNUP,
                            payload.flowType()
                    ),
                    () -> assertEquals(
                            KAKAO,
                            payload.provider()
                    ),
                    () -> assertEquals(
                            PROVIDER_USER_ID,
                            payload.providerUserId()
                    ),
                    () -> assertEquals(
                            EMAIL,
                            payload.email()
                    )
            );

        }

        @Test
        @DisplayName("인가 코드가 비어있을 시 예외 발생")
        void kakaoCallback_fail_emptyCode() {
            // given

            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthService.kakaoCallback(" "));

            // then

            assertEquals(OAUTH_CODE_NOT_FOUND, exception.getErrorCode());

            verifyNoInteractions(
                    kakaoOAuthClient, oAuthAccountService, oAuthTicketStore, authService
            );

        }

    }

    @Nested
    @DisplayName("OAuth Ticket 교환")
    class ExchangeTicket {

        @Test
        @DisplayName("LOGIN TICKET을 JWT 토큰으로 교환")
        void exchangeLoginTicket_success() {
            // given

            OAuthTicketRequest request = new OAuthTicketRequest(TICKET);

            OAuthTicketPayload payload = OAuthTicketPayload.login(EMAIL);

            AuthTokenResponse tokenResponse = createTokenResponse();

            when(oAuthTicketStore.consume(TICKET, LOGIN))
                    .thenReturn(payload);

            when(authService.loginWithKakao(EMAIL))
                    .thenReturn(tokenResponse);

            // when

            AuthTokenResponse response = oAuthService.exchangeLoginTicket(request);

            // then

            assertSame(tokenResponse, response);

            verify(oAuthTicketStore).consume(TICKET, LOGIN);

            verify(authService).loginWithKakao(EMAIL);

        }

    }

    @Test
    @DisplayName("SIGNUP TICKET으로 추가정보 입력 후 회원가입 및 JWT토큰 발급")
    void signUp_success() {
        // given

        OAuthSignUpRequest request = createSignUpRequest();

        OAuthTicketPayload payload = OAuthTicketPayload.signup(KAKAO, PROVIDER_USER_ID, EMAIL);

        AuthTokenResponse tokenResponse = createTokenResponse();

        when(oAuthTicketStore.consume(TICKET, SIGNUP))
                .thenReturn(payload);

        when(oAuthAccountService.completeSignUp(payload, request))
                .thenReturn(EMAIL);

        when(authService.loginWithKakao(EMAIL))
                .thenReturn(tokenResponse);
        // when

        AuthTokenResponse response = oAuthService.signUp(request);

        // then

        assertSame(tokenResponse, response);

        verify(oAuthTicketStore).consume(TICKET, SIGNUP);

        verify(oAuthAccountService).completeSignUp(payload, request);

        verify(authService).loginWithKakao(EMAIL);

    }

    private KakaoUserInfoResponse createKakaoUserInfo(String nickname) {

        KakaoUserInfoResponse.Profile profile = nickname == null ? null : new KakaoUserInfoResponse.Profile(false, nickname);

        KakaoUserInfoResponse.KakaoAccount account = new KakaoUserInfoResponse.KakaoAccount(
                false,
                profile,
                true,
                false,
                true,
                true,
                EMAIL
        );

        return new KakaoUserInfoResponse(KAKAO_USER_ID, account);
    }

    private OAuthSignUpRequest createSignUpRequest() {
        return new OAuthSignUpRequest(
                TICKET,
                NICKNAME,
                "테스트회원",
                LocalDate.of(1997, 1, 1),
                "010-1234-5678",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE
        );
    }

    private AuthTokenResponse createTokenResponse() {
        return new AuthTokenResponse(
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                null
        );
    }

}