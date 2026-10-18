package com.example.basketballmatching.auth.oauth2.dto;


/**
 * 카카오 인증 요청에 필요한 Redirect URL과
 * 브라우저 Cookie에 저장할 State를 함께 전달한다.
 */
public record OAuthAuthorizationResult(
        String authorizationUrl, String state
) {
}
