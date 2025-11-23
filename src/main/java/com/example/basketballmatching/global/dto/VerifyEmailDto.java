package com.example.basketballmatching.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyEmailDto {

    @Schema(description = "이메일", example = "test@test.com")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    private String email;

    @Schema(description = "인증번호", example = "123456")
    @NotBlank(message = "인증번호는 필수 입력값입니다.")
    private String code;

}
