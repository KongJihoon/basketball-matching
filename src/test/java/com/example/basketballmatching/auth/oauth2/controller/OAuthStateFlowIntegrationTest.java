package com.example.basketballmatching.auth.oauth2.controller;

import com.example.basketballmatching.auth.oauth2.client.KakaoOAuthClient;
import com.example.basketballmatching.auth.oauth2.client.dto.KakaoUserInfoResponse;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "KAKAO_FRONTEND_CALLBACK_URI=http://localhost:5173/auth/kakao/complete")
@DisplayName("OAuth State 보안 흐름 통합 테스트")
class OAuthStateFlowIntegrationTest {

    private static final String AUTHORIZATION_PATH =
            "/api/v1/auth/oauth2/kakao/authorization";
    private static final String CALLBACK_PATH =
            "/api/v1/auth/oauth2/kakao/callback";
    private static final String FRONTEND_CALLBACK =
            "http://localhost:5173/auth/kakao/complete";
    private static final String AUTHORIZATION_CODE = "kakao-authorization-code";
    private static final String EMAIL = "oauth-state-flow@test.com";
    private static final String NICKNAME = "OAuthState";
    private static final Long KAKAO_USER_ID = 987654321L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisService redisService;

    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;

    private final Set<String> createdRedisKeys = new HashSet<>();

    @AfterEach
    void clearRedis() {
        createdRedisKeys.forEach(redisService::deleteData);
    }

    @Test
    @DisplayName("State를 소비하고 가입 티켓과 함께 React로 이동한다")
    void stateFlow_success() throws Exception {
        String state = requestAuthorization();
        when(kakaoOAuthClient.getUserInfo(AUTHORIZATION_CODE))
                .thenReturn(createKakaoUserInfo());

        MvcResult result = mockMvc.perform(callbackRequest(state))
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andReturn();

        trackSignupTicket(result);
        assertNull(redisService.getData(stateKey(state)));
        verify(kakaoOAuthClient).getUserInfo(AUTHORIZATION_CODE);
    }

    @Test
    @DisplayName("State와 쿠키가 다르면 티켓 없이 오류를 전달한다")
    void stateFlow_fail_stateMismatch() throws Exception {
        String state = requestAuthorization();

        mockMvc.perform(get(CALLBACK_PATH)
                        .param("code", AUTHORIZATION_CODE)
                        .param("state", "tampered-state")
                        .cookie(new Cookie("oauth_state", state)))
                .andExpect(status().isSeeOther())
                .andExpect(header().string(
                        HttpHeaders.LOCATION, FRONTEND_CALLBACK + "#error=OAUTH_STATE_INVALID"
                ))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertEquals("valid", redisService.getData(stateKey(state)));
        verify(kakaoOAuthClient, never()).getUserInfo(anyString());
    }

    @Test
    @DisplayName("이미 소비한 State로는 티켓을 다시 발급받을 수 없다")
    void stateFlow_fail_reusedState() throws Exception {
        String state = requestAuthorization();
        when(kakaoOAuthClient.getUserInfo(AUTHORIZATION_CODE))
                .thenReturn(createKakaoUserInfo());

        MvcResult first = mockMvc.perform(callbackRequest(state))
                .andExpect(status().isSeeOther())
                .andReturn();

        trackSignupTicket(first);

        mockMvc.perform(callbackRequest(state))
                .andExpect(status().isSeeOther())
                .andExpect(header().string(
                        HttpHeaders.LOCATION, FRONTEND_CALLBACK + "#error=OAUTH_STATE_INVALID"
                ));

        verify(kakaoOAuthClient, times(1)).getUserInfo(AUTHORIZATION_CODE);
    }

    private String requestAuthorization() throws Exception {
        when(kakaoOAuthClient.createAuthorizationUrl(anyString()))
                .thenAnswer(invocation -> {
                    String state = invocation.getArgument(0);
                    return UriComponentsBuilder
                            .fromUriString("https://kauth.kakao.com/oauth/authorize")
                            .queryParam("state", state)
                            .build()
                            .toUriString();
                });

        MvcResult result = mockMvc.perform(get(AUTHORIZATION_PATH))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("oauth_state=")))
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);

        String state = UriComponentsBuilder.fromUriString(location)
                .build()
                .getQueryParams()
                .getFirst("state");

        assertNotNull(state);
        createdRedisKeys.add(stateKey(state));
        assertEquals("valid", redisService.getData(stateKey(state)));
        return state;
    }

    private RequestBuilder callbackRequest(String state) {
        return get(CALLBACK_PATH)
                .param("code", AUTHORIZATION_CODE)
                .param("state", state)
                .cookie(new Cookie("oauth_state", state));
    }

    private void trackSignupTicket(MvcResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location);
        assertTrue(location.startsWith(FRONTEND_CALLBACK + "#"));

        String fragment = URI.create(location).getFragment();
        assertNotNull(fragment);

        String prefix = "flowType=SIGNUP&ticket=";
        assertTrue(fragment.startsWith(prefix));

        String ticket = fragment.substring(prefix.length());
        assertFalse(ticket.isBlank());
        assertFalse(ticket.contains("&"));

        String key = "oauth:ticket:" + ticket;
        createdRedisKeys.add(key);
        assertNotNull(redisService.getData(key));
    }

    private String stateKey(String state) {
        return "oauth:state:" + state;
    }

    private KakaoUserInfoResponse createKakaoUserInfo() {
        KakaoUserInfoResponse.Profile profile =
                new KakaoUserInfoResponse.Profile(false, NICKNAME);

        KakaoUserInfoResponse.KakaoAccount account =
                new KakaoUserInfoResponse.KakaoAccount(
                        false, profile, true, false, true, true, EMAIL
                );

        return new KakaoUserInfoResponse(KAKAO_USER_ID, account);
    }
}