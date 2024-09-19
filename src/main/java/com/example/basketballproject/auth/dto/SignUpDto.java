package com.example.basketballproject.auth.dto;


import com.example.basketballproject.user.dto.UserDto;
import com.example.basketballproject.user.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


public class SignUpDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "아이디를 입력해주세요.")
        private String loginId;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;

        @Email
        @NotBlank(message = "이메일을 입력해주세요.")
        private String email;

        @NotBlank(message = "이름을 입력해주세요.")
        private String name;

        @NotBlank(message = "닉네임을 입력해주세요.")
        private String nickname;

        @NotNull(message = "생년월일을 입력해주세요.")
        private LocalDate birth;

        @NotBlank(message = "성별을 입력해주세요.")
        private String genderType;

        @NotBlank(message = "포지션을 입력해주세요.")
        private String position;

        private LocalDateTime createdDateTime;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {

        private String loginId;

        private String password;

        private String email;

        private String name;

        private String nickname;


        private LocalDate birth;

        private String userType;

        private String genderType;

        private String position;

        private LocalDateTime createdDateTime;

        public static Response toEntity(UserDto userDto) {

            return Response.builder()
                    .loginId(userDto.getLoginId())
                    .password(userDto.getPassword())
                    .email(userDto.getEmail())
                    .name(userDto.getName())
                    .nickname(userDto.getNickname())
                    .birth(userDto.getBirth())
                    .userType(userDto.getUserType())
                    .genderType(userDto.getGenderType())
                    .position(userDto.getPosition())
                    .createdDateTime(userDto.getCreatedDateTime())
                    .build();
        }

    }





}
