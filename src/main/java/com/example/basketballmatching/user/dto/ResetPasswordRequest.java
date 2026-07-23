package com.example.basketballmatching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @Schema(
                description = "비밀번호를 재설정할 사용자 이메일",
                example = "test@test.com")
        @NotBlank(
                message = "이메일은 필수 입력값입니다."
        )
        @Email(
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @NotBlank(
                message = "비밀번호는 필수 입력값입니다."
        )
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)"
                        + "(?=.*[~!@#$%^&*()])"
                        + "[a-zA-Z\\d~!@#$%^&*()]{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
        )
        String password,

        @NotBlank(
                message = "비밀번호 확인은 필수 입력값입니다."
        )
        String checkPassword
) {
}
