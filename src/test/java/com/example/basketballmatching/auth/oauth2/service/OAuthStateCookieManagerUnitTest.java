package com.example.basketballmatching.auth.oauth2.service;


import com.example.basketballmatching.auth.oauth2.config.OAuthSecurityProperties;
import com.example.basketballmatching.auth.oauth2.support.OAuthStateCookieManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OAuthStateCookieManager 단위 테스트")
class OAuthStateCookieManagerUnitTest {

    private static final String STATE = "oauth-state";

    private OAuthStateCookieManager cookieManager;

    @BeforeEach
    void setUp() {
        OAuthSecurityProperties properties =
                new OAuthSecurityProperties(
                        Duration.ofMinutes(3),
                        false
                );

        cookieManager =
                new OAuthStateCookieManager(properties);
    }

    @Test
    @DisplayName("State 보안 속성이 적용된 쿠키로 저장")
    void add_success() {
        // given

        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.add(response, STATE);
        // when

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        // then
        assertAll(
                () -> assertNotNull(setCookie),
                () -> assertTrue(
                        setCookie.contains(
                                "oauth_state=" + STATE
                        )
                ),
                () -> assertTrue(
                        setCookie.contains("HttpOnly")
                ),
                () -> assertTrue(
                        setCookie.contains("SameSite=Lax")
                ),
                () -> assertTrue(
                        setCookie.contains("Max-Age=180")
                ),
                () -> assertTrue(
                        setCookie.contains(
                                "Path=/api/v1/auth/oauth2/kakao/callback"
                        )
                ),
                () -> assertFalse(
                        setCookie.contains("Secure")
                )
        );

    }

    @Test
    @DisplayName("요청 쿠키에서 State 조회")
    void read_success() {
        // given

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setCookies(new Cookie("oauth_state", STATE));

        // when

        String result = cookieManager.read(request);

        // then

        assertEquals(STATE, result);

    }

    @Test
    @DisplayName("State 쿠키가 없으면 null 반환")
    void read_cookieMissing() {
        // given
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        // when
        String result = cookieManager.read(request);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("Callback 처리 후 State 쿠키 삭제")
    void delete_success() {
        // given

        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.delete(response);

        // when

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        // then
        assertAll(
                () -> assertNotNull(setCookie),
                () -> assertTrue(
                        setCookie.contains("oauth_state=")
                ),
                () -> assertTrue(
                        setCookie.contains("Max-Age=0")
                ),
                () -> assertTrue(
                        setCookie.contains("HttpOnly")
                ),
                () -> assertTrue(
                        setCookie.contains("SameSite=Lax")
                )
        );
    }



}
