package com.example.basketballmatching.auth.oauth2.config;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.oauth2.kakao")
public record KakaoOAuthProperties(
        @NotBlank
        String clientId,

        @NotBlank
        String clientSecret,

        @NotNull
        URI authorizationUri,

        @NotNull
        URI tokenUri,

        @NotNull
        URI userInfoUri,

        @NotNull
        URI redirectUri,

        @NotNull
        Duration connectTimeout,

        @NotNull
        Duration readTimeout
) {
}
