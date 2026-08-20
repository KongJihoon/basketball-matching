package com.example.basketballmatching.auth.oauth2.controller;

import com.example.basketballmatching.auth.oauth2.client.KakaoOAuthClient;
import com.example.basketballmatching.auth.oauth2.client.dto.KakaoUserInfoResponse;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("OAuth State 보안 흐름 통합 테스트")
class OAuthStateFlowIntegrationTest {

    private static final String AUTHORIZATION_PATH =
            "/api/v1/auth/oauth2/kakao/authorization";

    private static final String CALLBACK_PATH =
            "/api/v1/auth/oauth2/kakao/callback";

    private static final String AUTHORIZATION_CODE =
            "kakao-authorization-code";

    private static final String EMAIL =
            "oauth-state-flow@test.com";

    private static final String NICKNAME =
            "OAuthState";

    private static final Long KAKAO_USER_ID =
            987654321L;

    private static final String STATE_PREFIX =
            "oauth:state:";

    private static final String TICKET_PREFIX =
            "oauth:ticket:";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;

    private final Set<String> createdRedisKeys = new HashSet<>();

    @AfterEach
    void clearRedis() {
        createdRedisKeys.forEach(
                redisService::deleteData
        );

        createdRedisKeys.clear();
    }

    @Test
    @DisplayName("인가 요청 시 발급한 State를 Callback에서 검증 및 소비")
    void stateFlow_success() throws Exception {
        // given

        String state = requestAuthorization();

        when(kakaoOAuthClient.getUserInfo(AUTHORIZATION_CODE))
                .thenReturn(createKakaoUserInfo());

        // when

        MvcResult result = mockMvc.perform(get(CALLBACK_PATH)
                .param("code", AUTHORIZATION_CODE)
                .param("state", state)
                .cookie(new Cookie("oauth_state", state)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flowType").value("SIGNUP"))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andReturn();

        // then

        assertNull(redisService.getData(stateKey(state)));

        String ticket = readTicket(result);

        assertFalse(ticket.isBlank());

        createdRedisKeys.add(ticketKey(ticket));

        verify(kakaoOAuthClient).getUserInfo(AUTHORIZATION_CODE);

    }

    @Test
    @DisplayName("Callback State와 쿠키가 다르면 예외 발생")
    void stateFlow_fail_stateMismatch() throws Exception {
        // given

        String state = requestAuthorization();



        // when

        // then
        mockMvc.perform(
                        get(CALLBACK_PATH)
                                .param(
                                        "code",
                                        AUTHORIZATION_CODE
                                )
                                .param(
                                        "state",
                                        "tampered-state"
                                )
                                .cookie(
                                        new Cookie(
                                                "oauth_state",
                                                state
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("OAUTH_STATE_INVALID")
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("Max-Age=0")
                        )
                );
        assertEquals(
                "valid",
                redisService.getData(
                        stateKey(state)
                )
        );

        verify(
                kakaoOAuthClient,
                never()
        ).getUserInfo(anyString());
    }

    @Test
    @DisplayName("이미 소비한 State 재사용 불가")
    void stateFlow_fail_reusedState() throws Exception {
        // given

        String state = requestAuthorization();

        when(kakaoOAuthClient.getUserInfo(AUTHORIZATION_CODE))
                .thenReturn(createKakaoUserInfo());

        MvcResult firstResult = mockMvc.perform(
                        callbackRequest(state)
                )
                .andExpect(status().isOk())
                .andReturn();

        String ticket = readTicket(firstResult);

        createdRedisKeys.add(
                ticketKey(ticket)
        );

        // when

        // then

        mockMvc.perform(
                        callbackRequest(state)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.errorCode")
                                .value("OAUTH_STATE_INVALID")
                );

        verify(
                kakaoOAuthClient,
                times(1)
        ).getUserInfo(AUTHORIZATION_CODE);

    }

    private String requestAuthorization() throws Exception {

        when(kakaoOAuthClient.createAuthorizationUrl(anyString()))
                .thenAnswer(invocation -> {
                    String state = invocation.getArgument(0);

                    return UriComponentsBuilder
                            .fromUriString(
                                    "https://kauth.kakao.com/oauth/authorize"
                            )
                            .queryParam("state", state)
                            .build()
                            .toUriString();
                });

        MvcResult result = mockMvc.perform(get(AUTHORIZATION_PATH))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE, containsString("oauth_state=")
                ))
                .andReturn();

        String redirectedUrl = result.getResponse().getRedirectedUrl();

        assertNotNull(redirectedUrl);

        String state = UriComponentsBuilder
                .fromUriString(redirectedUrl)
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

    private String readTicket(
            MvcResult result
    ) throws Exception {

        JsonNode responseBody = objectMapper.readTree(
                result.getResponse()
                        .getContentAsString()
        );

        return responseBody
                .path("data")
                .path("ticket")
                .asText();
    }

    private String stateKey(String state) {
        return STATE_PREFIX + state;
    }

    private String ticketKey(String ticket) {
        return TICKET_PREFIX + ticket;
    }

    private KakaoUserInfoResponse
    createKakaoUserInfo() {

        KakaoUserInfoResponse.Profile profile =
                new KakaoUserInfoResponse.Profile(
                        false,
                        NICKNAME
                );

        KakaoUserInfoResponse.KakaoAccount account =
                new KakaoUserInfoResponse.KakaoAccount(
                        false,
                        profile,
                        true,
                        false,
                        true,
                        true,
                        EMAIL
                );

        return new KakaoUserInfoResponse(
                KAKAO_USER_ID,
                account
        );
    }

}
