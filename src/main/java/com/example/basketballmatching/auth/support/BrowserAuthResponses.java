package com.example.basketballmatching.auth.support;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.dto.BrowserAuthResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class BrowserAuthResponses {

    public static final String REFRESH_COOKIE_NAME = "basketball_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final boolean secureCookies;

    public BrowserAuthResponses(
            @Value("${AUTH_COOKIE_SECURE:true}") boolean secureCookies
    ) {
        this.secureCookies = secureCookies;
    }

    public ResponseEntity<CommonResponse<BrowserAuthResponse>> session(
            HttpStatus status,
            String message,
            AuthTokenResponse tokens
    ) {
        BrowserAuthResponse body = new BrowserAuthResponse(tokens.accessToken(), tokens.user());

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(tokens.refreshToken()).build().toString()
                )
                .body(CommonResponse.of(message, body));
    }

    public String expiredRefreshCookie() {
        return refreshCookie("").maxAge(0).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .path(COOKIE_PATH)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax");
    }

}
