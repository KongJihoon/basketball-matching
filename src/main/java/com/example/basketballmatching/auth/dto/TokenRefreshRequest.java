package com.example.basketballmatching.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @Schema(
                description = "Refresh Token",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        @NotBlank(message = "Refresh Token을 입력해주세요.")
        String refreshToken
) {
}
