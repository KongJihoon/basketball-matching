package com.example.basketballmatching.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @Schema(
                description = "이메일",
                example = "test@test.com"
        )
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식으로 입력해주세요.")
        String email,

        @Schema(
                description = "Refresh Token",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        @NotBlank(message = "Refresh Token을 입력해주세요.")
        String refreshToken
) {
}
