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

    /**
     * OAuth CSRF 방지를 위한 State를 발급하고
     * State가 포함된 카카오 Authorization URL을 생성한다.
     */
    public OAuthAuthorizationResult createKakaoAuthorization() {
        String state = oAuthStateStore.issue();

        String authorizationUrl = kakaoOAuthClient.createAuthorizationUrl(state);

        return new OAuthAuthorizationResult(authorizationUrl, state);
    }


    /**
     * 카카오 OAuth Callback을 처리한다.
     *
     * 인가 코드와 State를 검증하고 카카오 사용자 정보를 조회한 뒤,
     * 기존 회원 로그인 또는 신규 회원가입에 사용할 일회용 Ticket을 발급한다.
     */
    public OAuthCallbackResponse kakaoCallback(
            String authorizationCode, String returnedState, String cookieState) {

        // 카카오가 전달한 Authorization Code가 존재하는지 검증한다.
        validateAuthorizationCode(authorizationCode);

        /*
         * Callback의 State와 Cookie의 State를 비교하고,
         * Redis에 저장된 State를 한 번만 사용할 수 있도록 제거한다.
         */
        oAuthStateStore.consume(returnedState, cookieState);

        /*
         * Authorization Code를 카카오 AccessToken으로 교환한 뒤에
         * 해당 토큰을 이용해 사용자 정보를 조회한다.
         */
        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthClient.getUserInfo(authorizationCode);

        // 이후 로직에서 사용할 카카오 계정 정보가 존재하는지 검증한다.
        KakaoUserInfoResponse.KakaoAccount account = requireKakaoAccount(kakaoUserInfo);

        // 카카오 계정이 기존 회원인지 신규 회원인지 판별
        OAuthAccountDecision decision = oAuthAccountService.resolveKakaoAccount(kakaoUserInfo.id(), account.email());

        /*
         * 이미 가입한 사용자라면 LOGIN 용도의 Ticket 발급
         *
         * Callback 단계에서 JWT 토큰을 바로 발급하지 않고
         * 클라이언트가 발급받은 Ticket을 이용하여 별도 API로 서비스 JWT 토큰을 발급받고록 한다.
         */
        if (decision.flowType() == LOGIN) {
            OAuthTicketPayload payload = OAuthTicketPayload.login(decision.email());

            String ticket = oAuthTicketStore.issue(payload);

            return OAuthCallbackResponse.login(ticket);
        }


        // 신규 회원 카카오 프로필 닉네임 존재여부를 검증.
        String nickname = requireNickname(account);

        // 추가정보 회원가입에 필요한 Provider, ProviderId, 사용자 이메일을 포함한 회원가입 Payload 생성
        OAuthTicketPayload payload = OAuthTicketPayload.signup(KAKAO, String.valueOf(kakaoUserInfo.id()), decision.email());

        // 회원가입 정보를 Redis에 저장하고 일회용 Ticket을 발급
        String ticket = oAuthTicketStore.issue(payload);



        return OAuthCallbackResponse.signup(ticket, decision.email(), nickname);
    }

    /**
     * LOGIN 일회용 티켓을 사용하고 인증된 사용자에게 서비스 JWT 발급
     */
    public AuthTokenResponse exchangeLoginTicket(OAuthTicketRequest request) {
        OAuthTicketPayload payload = oAuthTicketStore.consume(request.ticket(), LOGIN);

        /*
         * OAuth 인증 결과에서 확인한 이메일로 사용자를 조회하고
         * 현재 로그인 가능한 상태인지 검증한 뒤 서비스 JWT 토큰 발급
         */
        return authService.loginWithKakao(payload.email());
    }

    /**
     * SIGNUP Ticket을 사용하여 신규 OAuth 회원가입을 완료하고
     * 가입된 사용자에게 서비스 JWT 토큰 발급
     */
    public AuthTokenResponse signUp(OAuthSignUpRequest request) {

        OAuthTicketPayload payload = oAuthTicketStore.consume(request.ticket(), SIGNUP);

        /*
         * Ticket의 저장된 OAuth 정보와 사용자 추가 정보를 이용해
         * UserEntity와 OAuthAccountEntity를 생성
         */
        String email = oAuthAccountService.completeSignUp(payload, request);


        return authService.loginWithKakao(email);
    }


    /**
     * 신규 OAuth 회원가입에 사용할 카카오 프로필 닉네임이 존재하는지 검증한다.
     */
    private static String requireNickname(KakaoUserInfoResponse.KakaoAccount account) {

        // 프로필 닉네임이 존재하지 않는다면 예외 처리
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

    /*
     * 카카오 Callback에 Authorization Code가 포함되어 있는지 검증한다.
     *
     * 인가 코드가 없으면 Access Token 교환을 진행할 수 없으므로
     * OAuth 요청을 즉시 중단한다.
     */
    private static void validateAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new CustomException(OAUTH_CODE_NOT_FOUND);
        }
    }







}
