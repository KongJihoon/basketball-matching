package com.example.basketballmatching.user.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChangePasswordDto {


    @Schema(description = "현재 비밀번호", example = "Test@1234", defaultValue = "Test@1234")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
            message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
    private String currentPassword;


    @Schema(description = "변경 비밀번호", example = "Test@1234", defaultValue = "Test@1234")
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
            message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
    private String newPassword;

    @Schema(description = "변경 비밀번호 확인", example = "Test@1234", defaultValue = "Test@1234")
    private String newCheckPassword;

}
