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
                // Javascript에서 State Cookie에 접근하지 못하게 한다.
                .httpOnly(true)
                // 운영환경에서는 HTTPS 연결에서만 전송한다.
                .secure(properties.secureCookie())
                /*
                 * 외부 카카오 페이지에서 우리 Callback으로 돌아오는
                 * 최상위 Get 요청에는 Cookie 전송을 허용한다.
                 */
                .sameSite("Lax")
                // OAuth Callback 경로만 Cookie를 전송
                .path(CALLBACK_PATH)
                /*
                 * Redis State와 Cookie의 유효시간을 동일하게 맞춰
                 * 완료되지 않은 인증 요청이 오래 유지되지 않게 한다.
                 */
                .maxAge(properties.stateExpiration())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * OAuth 인증 요청 단계에서 브라우저에 저장한 State Cookie를 조회한다.
     *
     * Cookie가 없으면 null을 반환하고
     * 이후 OAuthStateStore에서 유효하지 않은 요청으로 처리한다.
     */
    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);

        return cookie == null ? null : cookie.getValue();
    }

    /**
     * 사용이 끝난 OAuth State Cookie를 재사용 방지를 위해 브라우저에서 삭제한다.
     *
     * Cookie를 생성할 때와 동일한 이름과 Path를 지정하고
     * Max-Age를 0으로 설정해 즉시 만료시킨다.
     */
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
