package com.example.basketballproject.gameCreator.dto;

import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import lombok.*;

import java.time.LocalDateTime;

public class UpdateGameDto {


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Request {


        private String title;

        private String content;

        private Long headCount;

        private String address;

        private Gender gender;

        private FieldState fieldState;

        private MatchFormat matchFormat;

        private LocalDateTime startDateTime;


    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {

        private Long gameId;

        private String title;

        private String content;

        private Long headCount;

        private String address;

        private CityName cityName;

        private Gender gender;

        private FieldState fieldState;

        private MatchFormat matchFormat;

        private LocalDateTime createdDateTime;

        private LocalDateTime startDateTime;

        public static Response toDto(GameEntity gameEntity) {

            return Response.builder()
                    .title(gameEntity.getTitle())
                    .content(gameEntity.getContent())
                    .headCount(gameEntity.getHeadCount())
                    .address(gameEntity.getAddress())
                    .cityName(gameEntity.getCityName())
                    .gender(gameEntity.getGender())
                    .fieldState(gameEntity.getFieldState())
                    .matchFormat(gameEntity.getMatchFormat())
                    .createdDateTime(gameEntity.getCreatedDateTime())
                    .startDateTime(gameEntity.getStartDateTime())
                    .build();
        }

    }

}
