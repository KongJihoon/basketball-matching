package com.example.basketballmatching.user.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChangePasswordDto {


    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
            message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
    private String currentPassword;


    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
            message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
    private String newPassword;

    private String newCheckPassword;

}
