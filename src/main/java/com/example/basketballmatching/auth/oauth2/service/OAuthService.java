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

import static com.example.basketballmatching.global.exception.ErrorCode.OAUTH_CODE_NOT_FOUND;
import static com.example.basketballmatching.global.exception.ErrorCode.OAUTH_USERINFO_RESPONSE_PARSE_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;

    private final UserRepository userRepository;

    private final AuthService authService;

    public String createKakaoAuthorizationUrl() {
        return kakaoOAuthClient
                .createAuthorizationUrl();
    }



    public AuthTokenResponse kakaoLogin(String authorizationCode) {

        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new CustomException(OAUTH_CODE_NOT_FOUND);
        }

        KakaoUserInfoResponse kakaoUserInfo = kakaoOAuthClient.getUserInfo(authorizationCode);

        KakaoUserInfoResponse.KakaoAccount account = kakaoUserInfo.kakaoAccount();

        if (account == null) {
            throw new CustomException(OAUTH_USERINFO_RESPONSE_PARSE_ERROR);
        }

        String email = account.email();
        String nickname = extractNickname(account);

        if (!userRepository.existsByEmail(email)) {
            KakaoDto.Request request =
                    KakaoDto.Request.builder()
                            .email(email)
                            .name(nickname)
                            .nickname(nickname)
                            .build();

            userRepository.save(
                    KakaoDto.Request.toEntity(
                            request
                    )
            );
        }


        return authService.loginWithKakao(email);
    }

    private String extractNickname(KakaoUserInfoResponse.KakaoAccount account) {

        if (account.profile() == null || account.profile().nickname() == null || account.profile().nickname().isBlank()) {

            throw new CustomException(OAUTH_USERINFO_RESPONSE_PARSE_ERROR);
        }

        return account.profile().nickname();
    }






}
