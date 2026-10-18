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

    /**
     * 사용자를 카카오 인증 화면으로 이동시키기 위한
     * Authorization Code 방식의 인증 URL을 생성한다.
     */
    public String createAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUri(
                        properties.authorizationUri()
                )
                // 카카오에서 발급받은 애플리케이션 식별자
                .queryParam(
                        "client_id",
                        properties.clientId()
                )
                // 카카오 인증 완료 후 돌아올 지금 서버의 Callback 주소
                .queryParam(
                        "redirect_uri",
                        properties.redirectUri()
                )
                /*
                 * 인증 성공 후 토큰을 바로 받지 않고
                 * 일회용 Authorization Code를 받는 방식을 사용한다.
                 */
                .queryParam(
                        "response_type",
                        "code"
                )
                // OAuth 요청 위조 방지를 위해 서버가 발급한 State를 전달한다.
                .queryParam(
                        "state", state
                )
                .build()
                .toUriString();
    }

    /**
     * Authorization Code를 이용해 카카오 Access Token을 발급받고,
     * 해당 토큰으로 카카오 사용자 정보를 조회한다.
     */
    public KakaoUserInfoResponse getUserInfo(String authorizationCode) {

        // 일회용 Authorization Code를 카카오 Access Token으로 교환한다.
        KakaoTokenResponse tokenResponse = requestAccessToken(authorizationCode);

        /*
         * 카카오의 토큰 응답은 성공했지만 AccessToken이 존재하지 않으면
         * 정상적으로 사용자 정보를 조회할 수 없으므로 실패 처리
         */
        if (tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
        }

        /*
         * 카카오 Access Token은 현재 서버의 JWT가 아니라
         * 카카오 사용자 정보 API를 호출하기 위한 Provider Token이다.
         */
        KakaoUserInfoResponse userInfo = requestUserInfo(tokenResponse.accessToken());

        // 카카오 게정을 식별하는 Provider 사용자 ID가 있는지 확인한다.
        if (userInfo.id() == null) {
            throw new CustomException(OAUTH_PROVIDER_ID_NOT_FOUND);
        }

        return userInfo;

    }

    /**
     * 카카오 Token API에 Authorization Code를 전달하여
     * 카카오 Access Token을 발급받는다.
     */
    private KakaoTokenResponse requestAccessToken(String authorizationCode) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        // Authorization Code Grant 방식임을 나타낸다. 카카오 공식 문서에 "authorization_code"로 고정
        form.add("grant_type", "authorization_code");

        // 카카오에서 발급받는 애플리케이션 식별 정보
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        /*
         * 인가 요청 때 사용한 Redirect URI와 동일해야 한다.
         * 값이 다르면 카카오가 토큰 발급 요청을 거부한다.
         */
        form.add("redirect_uri", properties.redirectUri().toString());

        // Callback으로 전달받은 일회용 Authorization Code
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

            // 카카오가 4xx 또는 5xx 응답을 반환한 경우
            log.warn("[카카오 토큰 요청 실패] status={}", exception.getStatusCode());

            throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
        } catch (ResourceAccessException exception) {

            // 연결 실패나 응답 시간 초과 등
            log.warn("[카카오 토큰 요청 통신 실패]");

            throw new CustomException(OAUTH_TOKEN_REQUEST_FAILED);
        }

    }

    /**
     * 카카오 Access Token을 Bearer 인증 헤더에 담아
     * 카카오 사용자 정보 API를 호출한다.
     */
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
