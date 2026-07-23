package com.example.basketballmatching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmEmailVerificationRequest(
        @Schema(
                description = "인증 대상 이메일",
                example = "test@test.com"
        )
        @NotBlank(
                message = "이메일은 필수 입력값입니다."
        )
        @Email(
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @NotBlank(
                message = "인증번호는 필수 입력값입니다."
        )
        @Pattern(
                regexp = "^\\d{6}$",
                message = "인증번호는 6자리 숫자여야 합니다."
        )
        String code
) {
}
