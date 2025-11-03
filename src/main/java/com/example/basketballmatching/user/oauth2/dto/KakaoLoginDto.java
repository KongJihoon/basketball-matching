package com.example.basketballmatching.user.oauth2.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class KakaoLoginDto {
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    private String email;
}
