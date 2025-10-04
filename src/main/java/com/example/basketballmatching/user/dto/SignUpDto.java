package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SignUpDto {


    @Getter
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식으로 입력해주세요.")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,13}$",
                message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
        private String password;

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,13}$",
                message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
        private String checkPassword;

        @NotBlank(message = "이름을 입력해주세요.")
        private String name;

        @JsonFormat(
            shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd",
                timezone = "Asia/Seoul"
        )
        private LocalDate birth;

        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        private String phone;

        @NotBlank(message = "포지션을 입력해주세요.")
        private String position;

        public static UserEntity toEntity(SignUpDto.Request request) {

            return UserEntity.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .name(request.getName())
                    .birth(request.getBirth())
                    .phone(request.getPhone())
                    .position(Position.valueOf(request.getPosition()))
                    .userType(UserType.USER)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }



    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {

        private String email;

        private String name;

        private LocalDate birth;

        private String phone;

        private String position;

        private String userType;

        private LocalDateTime createdAt;

        public static Response fromDto (UserDto userDto) {

            return Response.builder()
                    .email(userDto.getEmail())
                    .name(userDto.getName())
                    .birth(userDto.getBirth())
                    .phone(userDto.getPhone())
                    .position(userDto.getPosition())
                    .userType(userDto.getUserType())
                    .createdAt(userDto.getCreatedAt())
                    .build();
        }


    }

}
