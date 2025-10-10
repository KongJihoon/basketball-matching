package com.example.basketballmatching.auth.dto;

import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoginDto {


    @Getter
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식으로 입력해주세요.")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
                message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
        private String password;

    }


}
