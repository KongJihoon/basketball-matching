package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.user.entity.UserEntity;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CreateGameDto {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Request {


        @NotBlank(message = "제목을 입력해주세요.")
        private String title;

        @NotBlank(message = "내용을 입력해주세요.")
        private String content;

        @NotNull(message = "인원수를 입력해주세요.")
        @Min(value = 6, message = "최소 6명이상입니다.")
        private Integer headCount;

        @NotNull(message = "실내외를 입력해주세요.")
        private FieldStatus fieldStatus;

        @NotNull(message = "경기형식을 입력해주세요.")
        private MatchFormat matchFormat;

        @NotNull(message = "시작 날짜를 입력해주세요.")
        @Future(message = "시작 시간은 현재 시각 이후여야 합니다.")
        private LocalDateTime startDateTime;

        @NotNull(message = "종료 날짜를 입력해주세요.")
        @Future(message = "종료 시간은 현재 시각 이후여야 합니다.")
        private LocalDateTime endDateTime;

        @NotBlank(message = "장소 이름을 입력해주세요.")
        private String placeName;

        @NotBlank(message = "주소를 입력해주세요.")
        private String address;

        private Double latitude;

        private Double longitude;

        @NotNull(message = "성별을 입력해주세요.")
        private MatchGenderType matchGenderType;

        public static GameEntity toEntity(CreateGameDto.Request request, UserEntity userEntity) {

            return GameEntity.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .headCount(request.getHeadCount())
                    .fieldStatus(request.getFieldStatus())
                    .matchGenderType(request.getMatchGenderType())
                    .startDateTime(request.getStartDateTime())
                    .endDateTime(request.getEndDateTime())
                    .placeName(request.getPlaceName())
                    .address(request.getAddress())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .cityName(CityName.getCityName(request.getAddress()))
                    .matchFormat(request.getMatchFormat())
                    .gameStatus(GameStatus.RECRUITING)
                    .userEntity(userEntity)
                    .build();


        }

    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {
        private Long gameId;

        private String title;

        private String content;

        private int headCount;

        private FieldStatus fieldStatus;

        private MatchFormat matchFormat;

        private GameStatus gameStatus;

        private LocalDateTime startDateTime;

        private String placeName;

        private String address;

        private Double latitude;

        private Double longitude;

        private CityName cityName;

        private MatchGenderType matchGenderType;

        private Long creatorId;

        private String creatorNickname;

        public static Response fromDto(GameDto gameDto) {

            return Response.builder()
                    .gameId(gameDto.getGameId())
                    .title(gameDto.getTitle())
                    .content(gameDto.getContent())
                    .headCount(gameDto.getHeadCount())
                    .fieldStatus(gameDto.getFieldStatus())
                    .matchFormat(gameDto.getMatchFormat())
                    .gameStatus(gameDto.getGameStatus())
                    .startDateTime(gameDto.getStartDateTime())
                    .placeName(gameDto.getPlaceName())
                    .address(gameDto.getAddress())
                    .latitude(gameDto.getLatitude())
                    .longitude(gameDto.getLongitude())
                    .cityName(gameDto.getCityName())
                    .matchGenderType(gameDto.getMatchGenderType())
                    .creatorId(gameDto.getCreatorId())
                    .creatorNickname(gameDto.getCreatorNickname())
                    .build();

        }
    }

}
