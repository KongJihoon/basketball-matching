package com.example.basketballmatching.user.oauth2.service.impl;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.oauth2.dto.KakaoDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoLoginDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoOAuthTokenDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoUserInfoDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoUserInfoDto.KakaoAccount;
import com.example.basketballmatching.user.oauth2.dto.KakaoUserInfoDto.Properties;
import com.example.basketballmatching.user.oauth2.service.OAuthService;
import com.example.basketballmatching.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserRepository userRepository;

    private final AuthService authService;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.kakao.authorization-uri}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String tokenRequestUri;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;


    @Override
    public String responseUrl() {
        return UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .build()
                .toString();
    }



    @Override
    @Transactional
    public AuthTokenResponse kakaoLogin(String code) throws JsonProcessingException {

        if (code == null || code.isBlank()) {
            throw new CustomException(ErrorCode.OAUTH_CODE_NOT_FOUND);
        }


        log.info("[카카오 유저 정보 발급]");
        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(code);
        Properties properties = kakaoUserInfo.getProperties();
        KakaoAccount kakaoAccount = kakaoUserInfo.getKakao_account();


        log.info("[카카오 로그인 시작]");

        if (!userRepository.existsByEmail(kakaoAccount.getEmail())) {
            log.info("[카카오 유저 회원가입 시작] nickname : {}", properties.getNickname());
            KakaoDto.Request request = KakaoDto.Request.builder()
                    .email(kakaoAccount.getEmail())
                    .name(properties.getNickname())
                    .nickname(properties.getNickname())
                    .build();

            log.info("[카카오 유저 회원가입 완료] nickname : {}", properties.getNickname());
            userRepository.save(KakaoDto.Request.toEntity(request));

        }



        log.info("[카카오 로그인 완료] nickname : {}", properties.getNickname());
        return authService.loginWithKakao(kakaoAccount.getEmail());
    }


    private KakaoUserInfoDto getKakaoUserInfo(String code){

        try {



            ResponseEntity<String> accessTokenResponse = requestAccessToken(code);

            if (accessTokenResponse.getBody() == null) {
                log.warn("[카카오 토큰 발급 실패] state : {}", accessTokenResponse.getStatusCode());
                throw new CustomException(ErrorCode.OAUTH_TOKEN_REQUEST_FAILED);
            }


            KakaoOAuthTokenDto accessToken = getAccessToken(accessTokenResponse);
            ResponseEntity<String> userinfo = requestUserinfo(accessToken);

            if (userinfo.getBody() == null) {
                log.warn("[카카오 사용자 정보 요청 실패] state : {}", userinfo.getStatusCode());
                throw new CustomException(ErrorCode.OAUTH_USERINFO_REQUEST_FAILED);
            }

            log.info("[카카오 토큰 발급 성공]");

            return getUserInfo(userinfo);

        } catch (HttpStatusCodeException e) {
            log.warn("[카카오 토큰 요청 오류] status : {}", e.getStatusCode());
            throw new CustomException(ErrorCode.OAUTH_USERINFO_REQUEST_FAILED);
        } catch (JsonProcessingException e) {
            log.warn("[카카오 토큰 응답 파싱 실패]");
            throw new CustomException(ErrorCode.OAUTH_USERINFO_RESPONSE_PARSE_ERROR);
        } catch (Exception e) {
            log.info("[내부 서버 오류] : {}", e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

    }

    private ResponseEntity<String> requestAccessToken(String code) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Type", "application/x-www-form-urlencoded;"
        + "charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);


        HttpEntity<MultiValueMap<String, String>> kakaoRequest = new HttpEntity<>(params, headers);

        return restTemplate.postForEntity(tokenRequestUri, kakaoRequest, String.class);
    }


    private KakaoOAuthTokenDto getAccessToken(ResponseEntity<String> response) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(response.getBody(), KakaoOAuthTokenDto.class);


    }

    private ResponseEntity<String> requestUserinfo(KakaoOAuthTokenDto kakaoOAuthTokenDto) {

        HttpHeaders headers = new HttpHeaders();

        RestTemplate restTemplate = new RestTemplate();

        headers.add("Authorization", "Bearer " +
                kakaoOAuthTokenDto.getAccess_token());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);


        return restTemplate.exchange("https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET, request, String.class);
    }

    public KakaoUserInfoDto getUserInfo(ResponseEntity<String> response) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(response.getBody(), KakaoUserInfoDto.class);
    }

    public KakaoLoginDto kakaoUserLogin(String email) {
        return KakaoLoginDto.builder()
                .email(email)
                .build();
    }

}
