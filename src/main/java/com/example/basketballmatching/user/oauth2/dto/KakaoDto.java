package com.example.basketballmatching.user.oauth2.dto;

import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;

public class KakaoDto {


    @Getter
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "이메일 형식으로 입력해주세요.")
        private String email;

        @NotBlank(message = "닉네임을 입력해주세요.")
        private String nickname;

        @NotBlank(message = "이름을 입력해주세요.")
        private String name;


        public static UserEntity toEntity(KakaoDto.Request request) {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            return UserEntity.builder()
                    .email(request.getEmail())
                    .password(encoder.encode("kakao"))
                    .nickname(request.getNickname())
                    .name(request.getName())
                    .birth(LocalDate.now())
                    .phone("010-0000-0000")
                    .address("DEFAULT_ADDRESS")
                    .loginProvider(LoginProvider.KAKAO)
                    .position(Position.NONE)
                    .userType(UserType.USER)
                    .genderType(GenderType.NONE)
                    .emailAuth(true)
                    .build();
        }

    }


    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long userId;

        private String email;

        private String name;

        private String nickname;

        private LocalDate birth;

        private String phone;

        private String address;

        private Position position;

        private UserType userType;

        private String refreshToken;

        public static Response fromDto(UserDto userDto, String refreshToken) {

            return Response.builder()
                    .userId(userDto.getUserId())
                    .email(userDto.getEmail())
                    .name(userDto.getName())
                    .nickname(userDto.getNickname())
                    .birth(userDto.getBirth())
                    .phone(userDto.getPhone())
                    .address(userDto.getAddress())
                    .position(userDto.getPosition())
                    .userType(userDto.getUserType())
                    .refreshToken(refreshToken)
                    .build();

        }


    }


}
