package com.example.basketballmatching.auth.oauth2.client;

import com.example.basketballmatching.auth.oauth2.client.dto.KakaoTokenResponse;
import com.example.basketballmatching.auth.oauth2.client.dto.KakaoUserInfoResponse;
import com.example.basketballmatching.auth.oauth2.config.KakaoOAuthProperties;
import com.example.basketballmatching.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Slf4j
@Component
public class KakaoOAuthClient {

    private final RestClient restClient;

    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(
            @Qualifier("kakaoRestClient")
            RestClient restClient,
            KakaoOAuthProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String createAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUri(
                        properties.authorizationUri()
                )
                .queryParam(
                        "client_id",
                        properties.clientId()
                )
                .queryParam(
                        "redirect_uri",
                        properties.redirectUri()
                )
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam(
                        "state", state
                )
                .build()
                .toUriString();
    }

    public KakaoUserInfoResponse getUserInfo(String authorizationCode) {

        KakaoTokenResponse tokenResponse = requestAccessToken(authorizationCode);

        if (tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
        }

        KakaoUserInfoResponse userInfo = requestUserInfo(tokenResponse.accessToken());

        if (userInfo.id() == null) {
            throw new CustomException(OAUTH_PROVIDER_ID_NOT_FOUND);
        }

        return userInfo;

    }

    private KakaoTokenResponse requestAccessToken(String authorizationCode) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");

        form.add("client_id", properties.clientId());

        form.add("client_secret", properties.clientSecret());

        form.add("redirect_uri", properties.redirectUri().toString());

        form.add("code", authorizationCode);

        try {

            KakaoTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null) {
                throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
            }

            return response;


        } catch (RestClientResponseException exception) {
            log.warn("[카카오 토큰 요청 실패] status={}", exception.getStatusCode());

            throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
        } catch (ResourceAccessException exception) {
            log.warn("[카카오 토큰 요청 통신 실패]");

            throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
        }

    }

    private KakaoUserInfoResponse requestUserInfo(String accessToken) {

        try {

            KakaoUserInfoResponse response = restClient.get()
                    .uri(properties.userInfoUri())
                    .headers(header -> header.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null) {
                throw new CustomException(OAUTH_USERINFO_REQUEST_FAILED);
            }

            return response;

        } catch (RestClientResponseException exception) {
            log.warn("[카카오 사용자 정보 요청 실패] status={}", exception.getStatusCode());

            throw new CustomException(OAUTH_USERINFO_REQUEST_FAILED);
        } catch (ResourceAccessException exception) {
            log.warn("[카카오 사용자 정보 요청 통신 실패]");

            throw new CustomException(OAUTH_USERINFO_REQUEST_FAILED);
        }
    }

}
