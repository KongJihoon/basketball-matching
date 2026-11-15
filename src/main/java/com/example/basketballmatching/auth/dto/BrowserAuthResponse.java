package com.example.basketballmatching.auth.dto;

public record BrowserAuthResponse(
        String accessToken,
        AuthenticatedUserResponse user
) {
}
