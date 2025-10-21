package com.example.basketballmatching.user.oauth2.service.impl;

import com.example.basketballmatching.auth.dto.LoginDto;
import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoOAuthTokenDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoUserInfoDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoUserInfoDto.KakaoAccount;
import com.example.basketballmatching.user.oauth2.dto.KakaoUserInfoDto.Properties;
import com.example.basketballmatching.user.oauth2.service.OAuthService;
import com.example.basketballmatching.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

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
        return authorizationUri + "?response_type=code" + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri;
    }



    @Override
    @Transactional
    public TokenDto kakaoLogin(String code) throws JsonProcessingException {

        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(code);
        Properties properties = kakaoUserInfo.getProperties();
        KakaoAccount kakaoAccount = kakaoUserInfo.getKakao_account();


        if (!userRepository.existsByEmail(kakaoAccount.getEmail())) {

            KakaoDto.Request request = KakaoDto.Request.builder()
                    .email(kakaoAccount.getEmail())
                    .name(properties.getNickname())
                    .nickname(properties.getNickname())
                    .build();

            userRepository.save(KakaoDto.Request.toEntity(request));

        }


        LoginDto.Request loginDto = kakaoUserLogin(kakaoAccount.getEmail());


        return authService.loginUser(loginDto.getEmail(), loginDto.getPassword());
    }


    private KakaoUserInfoDto getKakaoUserInfo(String code) throws JsonProcessingException {

        ResponseEntity<String> accessTokenResponse = requestAccessToken(code);
        KakaoOAuthTokenDto accessToken = getAccessToken(accessTokenResponse);
        ResponseEntity<String> userinfo = requestUserinfo(accessToken);


        return getUserInfo(userinfo);

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

    public LoginDto.Request kakaoUserLogin(String email) {
        return LoginDto.Request.builder()
                .email(email)
                .password("kakao")
                .build();
    }

}
