package com.example.basketballmatching.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @Schema(
                description = "현재 비밀번호",
                example = "Current@1234"
        )
        @NotBlank(
                message = "현재 비밀번호를 입력해주세요."
        )
        String currentPassword,

        @Schema(
                description = "새 비밀번호",
                example = "NewPassword@1234"
        )
        @NotBlank(
                message = "새 비밀번호를 입력해주세요."
        )
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)"
                        + "(?=.*[~!@#$%^&*()])"
                        + "[a-zA-Z\\d~!@#$%^&*()]{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
        )
        String newPassword,

        @Schema(
                description = "새 비밀번호 확인",
                example = "NewPassword@1234"
        )
        @NotBlank(
                message = "새 비밀번호 확인을 입력해주세요."
        )
        String newCheckPassword
) {
}
