package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.oauth2.client.KakaoOAuthClient;
import com.example.basketballmatching.auth.oauth2.client.dto.KakaoUserInfoResponse;
import com.example.basketballmatching.auth.oauth2.dto.KakaoDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;

    private final OAuthAccountService oAuthAccountService;

    private final UserRepository userRepository;

    private final AuthService authService;

    public String createKakaoAuthorizationUrl() {
        return kakaoOAuthClient
                .createAuthorizationUrl();
    }



    public AuthTokenResponse kakaoLogin(String authorizationCode) {

        validateAuthorizationCode(authorizationCode);

        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthClient.getUserInfo(authorizationCode);

        KakaoUserInfoResponse.KakaoAccount account = requireKakaoAccount(kakaoUserInfo);


        String email = requireEmail(account);
        String nickname= requireNickname(account);

        String loginEmail = oAuthAccountService.findOrCreateKakaoUserEmail(kakaoUserInfo.id(), email, nickname);


        return authService.loginWithKakao(loginEmail);
    }

    private String requireEmail(KakaoUserInfoResponse.KakaoAccount account) {

        if (!StringUtils.hasText(account.email())) {
            throw new CustomException(OAUTH_EMAIL_NOT_FOUND);
        }

        return account.email();
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

    private String extractNickname(KakaoUserInfoResponse.KakaoAccount account) {

        if (account.profile() == null || account.profile().nickname() == null || account.profile().nickname().isBlank()) {

            throw new CustomException(OAUTH_USERINFO_RESPONSE_PARSE_ERROR);
        }

        return account.profile().nickname();
    }






}
