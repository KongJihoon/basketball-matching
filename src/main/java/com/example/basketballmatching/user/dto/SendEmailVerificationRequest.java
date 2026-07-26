package com.example.basketballmatching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailVerificationRequest(
        @Schema(
                description = "인증번호를 전송할 이메일",
                example = "test@test.com"
        )
        @NotBlank(
                message = "이메일은 필수 입력값입니다."
        )
        @Email(
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email
) {
}
