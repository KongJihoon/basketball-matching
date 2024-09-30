package com.example.basketballproject.user.oAuth2.service;

import com.example.basketballproject.auth.dto.SignInDto;
import com.example.basketballproject.auth.service.AuthService;
import com.example.basketballproject.user.dto.KakaoDto;
import com.example.basketballproject.user.dto.UserDto;
import com.example.basketballproject.user.oAuth2.dto.KakaoOAuthTokenDto;
import com.example.basketballproject.user.oAuth2.dto.KakaoUserInfoDto;
import com.example.basketballproject.user.oAuth2.dto.KakaoUserInfoDto.Properties;
import com.example.basketballproject.user.repository.UserRepository;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2Service {


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


    public String responseUrl() {
        log.info("카카오 로그인 요청");

        return authorizationUri + "?client_id=" + clientId + "&redirect_uri=http://localhost:8080/api/oauth2/kakao"
                + "&response_type=code";
    }


    public UserDto kakaoLogin(String code) throws JsonProcessingException {

        log.info("카카오 유저 정보 가져오기");
        KakaoUserInfoDto kakaoUser = getKakaoUserInfoDto(code);
        Properties properties = kakaoUser.getProperties();
        KakaoUserInfoDto.KakaoAccount kakaoAccount = kakaoUser.getKakao_account();
        String loginId = "kakao_" + kakaoUser.getId().toString();

        log.info("카카오 유저 정보 등록");

        validateKakaoInfo(kakaoAccount, loginId, properties);
        log.info("카카오 유저 정보 등록 성공");
        SignInDto.Request logInDto = kakoUserLogin(loginId);
        log.info("카카오 로그인 성공");
        return authService.loginUser(logInDto);


    }

    private void validateKakaoInfo(KakaoUserInfoDto.KakaoAccount kakaoAccount, String loginId, Properties properties) {
        if (kakaoAccount.getEmail() == null && kakaoAccount.getGender() == null && kakaoAccount.getPosition() == null
                && !userRepository.existsByLoginIdAndDeletedDateTimeNull(loginId)) {
            KakaoDto.Request user = kakaoUserDto(
                    loginId,
                    loginId + "@kakao.com",
                    properties.getNickname()
                    , "MALE",
                    "GUARD");

            userRepository.save(KakaoDto.Request.toEntity(user));
        } else if (kakaoAccount.getEmail() == null && kakaoAccount.getGender() != null && kakaoAccount.getPosition() == null
                && !userRepository.existsByLoginIdAndDeletedDateTimeNull(loginId)) {
            KakaoDto.Request user = kakaoUserDto(
                    loginId,
                    loginId + "@kakao.com",
                    properties.getNickname()
                    , kakaoAccount.getGender()
                    ,"GUARD");

            userRepository.save(KakaoDto.Request.toEntity(user));
        } else if (kakaoAccount.getEmail() != null && kakaoAccount.getGender() == null && kakaoAccount.getPosition() == null
                && !userRepository.existsByLoginIdAndDeletedDateTimeNull(loginId)) {
            KakaoDto.Request user = kakaoUserDto(
                    loginId,
                    "kakao_" + kakaoAccount.getEmail(),
                    properties.getNickname()
                    , "MALE", "GUARD");

            userRepository.save(KakaoDto.Request.toEntity(user));
        } else if (kakaoAccount.getEmail() == null && kakaoAccount.getGender() == null && kakaoAccount.getPosition() != null
                && !userRepository.existsByLoginIdAndDeletedDateTimeNull(loginId)) {
            KakaoDto.Request user = kakaoUserDto(
                    loginId,
                    "kakao_" + kakaoAccount.getEmail(),
                    properties.getNickname()
                    , "MALE", kakaoAccount.getPosition());
            userRepository.save(KakaoDto.Request.toEntity(user));

        } else if (!userRepository.existsByLoginIdAndDeletedDateTimeNull(loginId)) {
            KakaoDto.Request user = kakaoUserDto(
                    loginId,
                    "kakao_" + kakaoAccount.getEmail(),
                    properties.getNickname()
                    , kakaoAccount.getGender()
                    , kakaoAccount.getPosition());

            userRepository.save(KakaoDto.Request.toEntity(user));
        }
    }


    private KakaoUserInfoDto getKakaoUserInfoDto(String code) throws JsonProcessingException {

        log.info("카카오 토큰 요청");
        ResponseEntity<String> accessTokenResponse = requestAccessToken(code);

        log.info("카카오 토큰 요청 성공");
        KakaoOAuthTokenDto kakaoOAuthTokenDto = getAccessToken(accessTokenResponse);

        log.info("카카오 유저 정보 요청");
        ResponseEntity<String> userInfoResponse = requestUserInfo(kakaoOAuthTokenDto);

        log.info("카카오 유저 정보 요청 성공");

        return getUserInfo(userInfoResponse);


    }

    private KakaoDto.Request kakaoUserDto(String loginId, String email, String nickname, String gender, String position) {

        return KakaoDto.Request.builder()
                .loginId(loginId)
                .name(nickname)
                .nickname(nickname)
                .email(email)
                .gender(gender.toUpperCase())
                .position(position)
                .createdDateTime(LocalDateTime.now())
                .build();

    }

    private ResponseEntity<String> requestAccessToken(String code) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", "http://localhost:8080/api/oauth2/kakao");
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> kakoRequest = new HttpEntity<>(params, headers);

        return restTemplate.postForEntity(tokenRequestUri, kakoRequest, String.class);

    }

    private KakaoOAuthTokenDto getAccessToken(ResponseEntity<String>response) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(response.getBody(), KakaoOAuthTokenDto.class);

    }

    private ResponseEntity<String> requestUserInfo(
            KakaoOAuthTokenDto oAuthTokenDto
    ) {

        HttpHeaders headers = new HttpHeaders();

        RestTemplate restTemplate = new RestTemplate();

        headers.add("Authorization", "Bearer " + oAuthTokenDto.getAccess_token());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        return restTemplate.exchange("https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET, request, String.class);


    }

    private KakaoUserInfoDto getUserInfo(ResponseEntity<String> response) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(response.getBody(), KakaoUserInfoDto.class);

    }

    private SignInDto.Request kakoUserLogin(String id) {
        return SignInDto.Request.builder()
                .loginId(id)
                .password("kakao")
                .build();
    }

}
