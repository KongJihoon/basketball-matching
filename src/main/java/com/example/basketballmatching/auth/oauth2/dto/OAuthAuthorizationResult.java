package com.example.basketballmatching.auth.oauth2.dto;

public record OAuthAuthorizationResult(
        String authorizationUrl, String state
) {
}
