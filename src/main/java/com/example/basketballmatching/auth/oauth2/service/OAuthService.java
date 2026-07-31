package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.oauth2.client.KakaoOAuthClient;
import com.example.basketballmatching.auth.oauth2.client.dto.KakaoUserInfoResponse;
import com.example.basketballmatching.auth.oauth2.dto.*;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.example.basketballmatching.auth.oauth2.type.OAuthFlowType.LOGIN;
import static com.example.basketballmatching.auth.oauth2.type.OAuthFlowType.SIGNUP;
import static com.example.basketballmatching.auth.oauth2.type.OAuthProvider.KAKAO;
import static com.example.basketballmatching.global.exception.ErrorCode.OAUTH_CODE_NOT_FOUND;
import static com.example.basketballmatching.global.exception.ErrorCode.OAUTH_USERINFO_RESPONSE_PARSE_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;

    private final OAuthAccountService oAuthAccountService;

    private final OAuthTicketStore oAuthTicketStore;
    private final AuthService authService;
    private final OAuthStateStore oAuthStateStore;

    public OAuthAuthorizationResult createKakaoAuthorization() {
        String state = oAuthStateStore.issue();

        String authorizationUrl = kakaoOAuthClient.createAuthorizationUrl(state);

        return new OAuthAuthorizationResult(authorizationUrl, state);
    }



    public OAuthCallbackResponse kakaoCallback(
            String authorizationCode, String returnedState, String cookieState) {

        validateAuthorizationCode(authorizationCode);

        oAuthStateStore.consume(returnedState, cookieState);

        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthClient.getUserInfo(authorizationCode);

        KakaoUserInfoResponse.KakaoAccount account = requireKakaoAccount(kakaoUserInfo);

        OAuthAccountDecision decision = oAuthAccountService.resolveKakaoAccount(kakaoUserInfo.id(), account.email());

        if (decision.flowType() == LOGIN) {
            OAuthTicketPayload payload = OAuthTicketPayload.login(decision.email());

            String ticket = oAuthTicketStore.issue(payload);

            return OAuthCallbackResponse.login(ticket);
        }


        String nickname= requireNickname(account);

        OAuthTicketPayload payload = OAuthTicketPayload.signup(KAKAO, String.valueOf(kakaoUserInfo.id()), decision.email());

        String ticket = oAuthTicketStore.issue(payload);


        return OAuthCallbackResponse.signup(ticket, decision.email(), nickname);
    }

    public AuthTokenResponse exchangeLoginTicket(OAuthTicketRequest request) {
        OAuthTicketPayload payload = oAuthTicketStore.consume(request.ticket(), LOGIN);

        return authService.loginWithKakao(payload.email());
    }

    public AuthTokenResponse signUp(OAuthSignUpRequest request) {

        OAuthTicketPayload payload = oAuthTicketStore.consume(request.ticket(), SIGNUP);

        String email = oAuthAccountService.completeSignUp(payload, request);


        return authService.loginWithKakao(email);
    }




    private static String requireNickname(KakaoUserInfoResponse.KakaoAccount account) {

        if (account.profile() == null || !StringUtils.hasText(account.profile().nickname())) {
            throw new CustomException(OAUTH_USERINFO_RESPONSE_PARSE_ERROR);
        }

        return account.profile().nickname();
    }

    private KakaoUserInfoResponse.KakaoAccount requireKakaoAccount(KakaoUserInfoResponse kakaoUserInfo) {
        if (kakaoUserInfo.kakaoAccount() == null) {
            throw new CustomException(OAUTH_USERINFO_RESPONSE_PARSE_ERROR);
        }

        return kakaoUserInfo.kakaoAccount();
    }

    private static void validateAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new CustomException(OAUTH_CODE_NOT_FOUND);
        }
    }







}
