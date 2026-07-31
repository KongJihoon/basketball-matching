package com.example.basketballmatching.auth.oauth2.support;

import com.example.basketballmatching.auth.oauth2.config.OAuthSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
public class OAuthStateCookieManager {

    private static final String COOKIE_NAME = "oauth_state";

    private static final String CALLBACK_PATH = "/api/v1/auth/oauth2/kakao/callback";

    private final OAuthSecurityProperties properties;

    public void add(HttpServletResponse response, String state) {
        ResponseCookie cookie = ResponseCookie
                .from(COOKIE_NAME, state)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path(CALLBACK_PATH)
                .maxAge(properties.stateExpiration())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);

        return cookie == null ? null : cookie.getValue();
    }

    public void delete(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie
                .from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path(CALLBACK_PATH)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    }

}
