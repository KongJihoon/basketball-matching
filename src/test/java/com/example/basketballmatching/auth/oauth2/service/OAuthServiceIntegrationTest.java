package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.dto.OAuthAccountDecision;
import com.example.basketballmatching.auth.oauth2.dto.OAuthSignUpRequest;
import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketPayload;
import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketRequest;
import com.example.basketballmatching.auth.oauth2.repository.OAuthAccountRepository;
import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.support.IntegrationTestSupport;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@DisplayName("OAuthService 통합 테스트")
public class OAuthServiceIntegrationTest extends IntegrationTestSupport {

    private static final Long KAKAO_USER_ID = 123456789L;

    private static final String PROVIDER_USER_ID = String.valueOf(KAKAO_USER_ID);

    private static final String EMAIL =
            "oauth-integration@test.com";

    private static final String NICKNAME =
            "OAuth통합";

    private static final String REFRESH_TOKEN_PREFIX =
            "refreshToken:";

    private static final String OAUTH_TICKET_PREFIX =
            "oauth:ticket:";

    @Autowired
    private OAuthService oauthService;

    @Autowired
    private OAuthAccountService oauthAccountService;

    @Autowired
    private OAuthTicketStore oauthTicketStore;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenProvider tokenProvider;

    private final Set<String> createdRedisKey = new HashSet<>();

    @AfterEach
    void clearRedis() {
        createdRedisKey.forEach(
                redisService::deleteData
        );

        createdRedisKey.clear();
    }

    @Test
    @DisplayName("SIGNUP TICKET 발급 유저 추가정보 입력 후 회원가입 및 JWT토큰 발급")
    void signup_success() {
        // given

        OAuthTicketPayload payload = OAuthTicketPayload.signup(
                OAuthProvider.KAKAO,
                PROVIDER_USER_ID,
                EMAIL
        );

        String ticket = oauthTicketStore.issue(payload);

        createdRedisKey.add(oauthTicketKey(ticket));

        createdRedisKey.add(refreshTokenKey());

        OAuthSignUpRequest request = createSignUpRequest(ticket);

        // when

        AuthTokenResponse response = oauthService.signUp(request);

        // then

        UserEntity savedUser = userRepository.findByEmail(EMAIL)
                .orElseThrow();

        OAuthAccountEntity savedOAuthAccount = oauthAccountRepository.findWithUserByProviderAndProviderUserId(
                OAuthProvider.KAKAO, PROVIDER_USER_ID
        ).orElseThrow();

        assertAll(
                () -> assertNotNull(
                        savedUser.getUserId()
                ),
                () -> assertEquals(
                        EMAIL,
                        savedUser.getEmail()
                ),
                () -> assertEquals(
                        NICKNAME,
                        savedUser.getNickname()
                ),
                () -> assertEquals(
                        LoginProvider.KAKAO,
                        savedUser.getLoginProvider()
                ),
                () -> assertNull(
                        savedUser.getPassword()
                ),
                () -> assertTrue(
                        savedUser.isEmailAuth()
                ),
                () -> assertEquals(
                        OAuthProvider.KAKAO,
                        savedOAuthAccount.getProvider()
                ),
                () -> assertEquals(
                        PROVIDER_USER_ID,
                        savedOAuthAccount.getProviderUserId()
                ),
                () -> assertEquals(
                        savedUser.getUserId(),
                        savedOAuthAccount
                                .getUserEntity()
                                .getUserId()
                ),
                () -> assertNotNull(
                        response.accessToken()
                ),
                () -> assertNotNull(
                        response.refreshToken()
                ),
                () -> assertEquals(
                        savedUser.getUserId(),
                        response.user().userId()
                )
        );

        assertDoesNotThrow(
                () -> tokenProvider.validateToken(response.accessToken())
        );

        assertDoesNotThrow(() -> tokenProvider.validateToken(response.refreshToken()));

        assertEquals(
                EMAIL,
                tokenProvider.getEmailFromToken(
                        response.accessToken()
                )
        );

        assertEquals(
                response.refreshToken(),
                redisService.getData(
                        refreshTokenKey()
                )
        );

        assertNull(
                redisService.getData(
                        oauthTicketKey(ticket)
                )
        );
    }

    @Test
    @DisplayName("기존 OAuth 회원 LOGIN TICKET으로 JWT 토큰 발급 및 재사용 금지")
    void exchangeLoginTicket_success_and_reuseFail() {
        // given

        UserEntity user = userRepository.saveAndFlush(createOAuthUser());

        OAuthAccountEntity oAuthAccount = OAuthAccountEntity.create(user, OAuthProvider.KAKAO, PROVIDER_USER_ID);

        oauthAccountRepository.saveAndFlush(oAuthAccount);

        OAuthAccountDecision decision = oauthAccountService.resolveKakaoAccount(KAKAO_USER_ID, EMAIL);

        assertEquals(OAuthFlowType.LOGIN, decision.flowType());

        OAuthTicketPayload payload = OAuthTicketPayload.login(decision.email());

        String ticket = oauthTicketStore.issue(payload);

        createdRedisKey.add(oauthTicketKey(ticket));

        createdRedisKey.add(refreshTokenKey());

        OAuthTicketRequest request = new OAuthTicketRequest(ticket);

        // when

        AuthTokenResponse response = oauthService.exchangeLoginTicket(request);

        // then
        assertAll(
                () -> assertNotNull(
                        response.accessToken()
                ),
                () -> assertNotNull(
                        response.refreshToken()
                ),
                () -> assertEquals(
                        user.getUserId(),
                        response.user().userId()
                ),
                () -> assertEquals(
                        EMAIL,
                        response.user().email()
                ),
                () -> assertNull(
                        redisService.getData(
                                oauthTicketKey(ticket)
                        )
                )
        );

        CustomException exception = assertThrows(CustomException.class, () -> oauthService.exchangeLoginTicket(request));

        assertEquals(ErrorCode.OAUTH_TICKET_INVALID, exception.getErrorCode());

    }


    private OAuthSignUpRequest createSignUpRequest(
            String ticket
    ) {
        return new OAuthSignUpRequest(
                ticket,
                NICKNAME,
                "OAuth통합회원",
                LocalDate.of(1997, 1, 1),
                "010-1234-5678",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE
        );
    }

    private UserEntity createOAuthUser() {
        return UserEntity.createOAuth(
                EMAIL,
                NICKNAME,
                "OAuth통합회원",
                LocalDate.of(1997, 1, 1),
                "010-1234-5678",
                "서울특별시 강남구",
                Position.GUARD,
                GenderType.MALE,
                LoginProvider.KAKAO
        );
    }

    private String refreshTokenKey() {
        return REFRESH_TOKEN_PREFIX + EMAIL;
    }

    private String oauthTicketKey(String ticket) {
        return OAUTH_TICKET_PREFIX + ticket;
    }

}
