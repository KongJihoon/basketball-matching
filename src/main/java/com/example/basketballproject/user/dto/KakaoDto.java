package com.example.basketballproject.user.dto;

import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.type.GenderType;
import com.example.basketballproject.user.type.Position;
import com.example.basketballproject.user.type.UserType;
import lombok.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class KakaoDto {


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        private String loginId;

        private String email;

        private String name;

        private String nickname;

        private String gender;

        private String position;

        private LocalDateTime createdDateTime;

        public static UserEntity toEntity(Request request) {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            return UserEntity.builder()
                    .loginId(request.loginId)
                    .password(encoder.encode("kakao"))
                    .email(request.email)
                    .name(request.name)
                    .birth(LocalDate.now())
                    .genderType(GenderType.valueOf(request.gender))
                    .position(Position.valueOf(request.position))
                    .nickname(request.nickname)
                    .userType(UserType.USER)
                    .emailAuth(true)
                    .createdDateTime(request.createdDateTime)
                    .build();
        }


    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {

        private Long id;

        private String loginId;

        private String email;


        private String name;


        private LocalDate birth;


        private String gender;

        private String nickname;

        private LocalDateTime createdAt;


        private String position;

        private String userType;

        private LocalDateTime createdDateTime;

        private String refreshToken;

        public static Response fromDto(UserDto userDto, String refreshToken) {

            return Response.builder()
                    .id(userDto.getUserId())
                    .loginId(userDto.getLoginId())
                    .email(userDto.getEmail())
                    .name(userDto.getName())
                    .birth(userDto.getBirth())
                    .gender(userDto.getGenderType())
                    .nickname(userDto.getNickname())
                    .createdAt(userDto.getCreatedDateTime())
                    .position(userDto.getPosition())
                    .userType(userDto.getUserType())
                    .createdDateTime(userDto.getCreatedDateTime())
                    .refreshToken(refreshToken)
                    .build();

        }

    }

}
