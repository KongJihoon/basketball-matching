package com.example.basketballproject.gameCreator.dto;

import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import com.example.basketballproject.user.entity.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class CreateGameDto {


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "제목을 입력해주세요.")
        private String title;

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;

        @NotNull(message = "인원 수를 입력해주세요.")
        private Long headCount;

        @NotNull(message = "실내외를 입력해주세요.")
        private FieldState fieldState;

        @NotNull(message = "성별을 입력해주세요.")
        private Gender gender;

        @NotNull(message = "경기 형식을 입력해주세요.")
        private MatchFormat matchFormat;

        @NotBlank(message = "주소를 입력해주세요.")
        private String address;

        @NotNull(message = "시작 날짜를 입력해주세요.")
        private LocalDateTime startDateTime;


        public static GameEntity toEntity(Request request, UserEntity user) {

            return GameEntity.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .headCount(request.getHeadCount())
                    .fieldState(request.getFieldState())
                    .gender(request.getGender())
                    .matchFormat(request.getMatchFormat())
                    .address(request.getAddress())
                    .startDateTime(request.getStartDateTime())
                    .userEntity(user)
                    .build();

        }

    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {

        private String title;

        private String content;

        private Long headCount;

        private FieldState fieldState;

        private Gender gender;

        private CityName cityName;

        private MatchFormat matchFormat;

        private String address;

        private LocalDateTime startDateTime;

        private LocalDateTime createdDateTime;

        public static Response toDto(GameEntity gameEntity) {

            return Response.builder()
                    .title(gameEntity.getTitle())
                    .content(gameEntity.getContent())
                    .headCount(gameEntity.getHeadCount())
                    .fieldState(gameEntity.getFieldState())
                    .gender(gameEntity.getGender())
                    .cityName(gameEntity.getCityName())
                    .matchFormat(gameEntity.getMatchFormat())
                    .address(gameEntity.getAddress())
                    .startDateTime(gameEntity.getStartDateTime())
                    .createdDateTime(gameEntity.getCreatedDateTime())
                    .build();
        }
    }

}
