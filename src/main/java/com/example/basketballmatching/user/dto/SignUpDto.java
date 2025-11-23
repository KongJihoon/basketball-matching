package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SignUpDto {


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Request {

        @Schema(description = "이메일", example = "test@test.com")
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식으로 입력해주세요.")
        private String email;

        @Schema(description = "비밀번호", example = "Test@1234", defaultValue = "Test@1234")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
                message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
        private String password;

        @Schema(description = "비밀번호 확인", example = "Test@1234", defaultValue = "Test@1234")
        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()])[a-zA-Z\\d~!@#$%^&*()]{8,}$",
                message = "비밀번호는 영어 대소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.")
        private String checkPassword;

        @Schema(description = "닉네임", example = "커리")
        @NotBlank(message = "닉네임을 입력해주세요.")
        private String nickname;

        @Schema(description = "이름", example = "서장훈")
        @NotBlank(message = "이름을 입력해주세요.")
        private String name;

        @Schema(description = "생년월일", example = "19970101")
        @JsonFormat(
            shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd"
        )
        private LocalDate birth;

        @Schema(description = "휴대폰 번호", example = "010-1111-0000")
        @NotBlank(message = "휴대폰 번호를 입력해주세요.")
        @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        private String phone;

        @Schema(description = "주소", example = "서울특별시 강남구")
        @NotBlank(message = "주소를 입력해주세요.")
        private String address;

        @Schema(description = "포지션", example = "NONE", defaultValue = "NONE")
        @NotNull(message = "포지션을 입력해주세요.")
        private Position position;

        @Schema(description = "성별", example = "MALE", defaultValue = "MALE")
        @NotNull(message = "성별을 입력해주세요.")
        private GenderType genderType;

        @Schema(description = "로그인 타입", example = "LOCAL", defaultValue = "LOCAL")
        private LoginProvider loginProvider;

        public static UserEntity toEntity(SignUpDto.Request request) {

            return UserEntity.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .nickname(request.getNickname())
                    .name(request.getName())
                    .birth(request.getBirth())
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .position(request.getPosition())
                    .userType(UserType.USER)
                    .genderType(request.genderType)
                    .loginProvider(LoginProvider.LOCAL)
                    .build();
        }



    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {

        @Schema(description = "이메일", example = "test@test.com")
        private String email;

        @Schema(description = "닉네임", example = "커리")
        private String nickname;

        @Schema(description = "이름", example = "서장훈")
        private String name;

        @Schema(description = "생년월일", example = "19970101")
        private LocalDate birth;

        @Schema(description = "휴대폰 번호", example = "010-1111-0000")
        private String phone;

        @Schema(description = "주소", example = "서울특별시 강남구")
        private String address;

        @Schema(description = "포지션", example = "NONE", defaultValue = "NONE")
        private Position position;

        @Schema(description = "성별", example = "MALE", defaultValue = "MALE")
        private GenderType genderType;

        @Schema(description = "권한", example = "USER", defaultValue = "USER")
        private UserType userType;

        @Schema(description = "생성 일시", example = "2025-11-22T15:00:00:00", defaultValue = "2025-11-22T15:00:00:00")
        private LocalDateTime createdAt;

        public static Response fromDto (UserDto userDto) {

            return Response.builder()
                    .email(userDto.getEmail())
                    .name(userDto.getName())
                    .nickname(userDto.getNickname())
                    .birth(userDto.getBirth())
                    .phone(userDto.getPhone())
                    .position(userDto.getPosition())
                    .userType(userDto.getUserType())
                    .genderType(userDto.getGenderType())
                    .createdAt(userDto.getCreatedAt())
                    .build();
        }


    }

}
