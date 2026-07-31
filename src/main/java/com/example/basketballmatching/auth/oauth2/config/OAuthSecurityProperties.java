package com.example.basketballmatching.auth.oauth2.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.oauth2.security")
public record OAuthSecurityProperties(
        @NotNull
        Duration stateExpiration,

        boolean secureCookie
) {
}
