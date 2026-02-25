package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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


        @Schema(name = "경기 제목", example = "서울 xx체육관 3vs3 경기 인원 모집")
        @NotBlank(message = "제목을 입력해주세요.")
        private String title;

        @Schema(name = "경기 상세 내용", example = "3대3경기 인원 모집입니다.")
        @NotBlank(message = "내용을 입력해주세요.")
        private String content;

        @Schema(name = "경기 인원 수", example = "6")
        @NotNull(message = "인원수를 입력해주세요.")
        @Min(value = 6, message = "최소 6명이상입니다.")
        private Integer headCount;

        @Schema(name = "경기 실내외", example = "INDOOR")
        @NotNull(message = "실내외를 입력해주세요.")
        private FieldStatus fieldStatus;

        @Schema(name = "경기 형식", example = "THREE_ON_THREE")
        @NotNull(message = "경기형식을 입력해주세요.")
        private MatchFormat matchFormat;

        @Schema(name = "경기 시작 날짜", example = "2025-11-22T15:00:00:00")
        @NotNull(message = "시작 날짜를 입력해주세요.")
        @Future(message = "시작 시간은 현재 시각 이후여야 합니다.")
        private LocalDateTime startDateTime;

        @Schema(name = "경기 종료 날짜", example = "2025-11-22T17:00:00:00")
        @NotNull(message = "종료 날짜를 입력해주세요.")
        @Future(message = "종료 시간은 현재 시각 이후여야 합니다.")
        private LocalDateTime endDateTime;

        @Schema(name = "경기 장소", example = "서울 xx 체육관")
        @NotBlank(message = "장소 이름을 입력해주세요.")
        private String placeName;

        @Schema(name = "경기 주소", example = "서울특별시 강남구..")
        @NotBlank(message = "주소를 입력해주세요.")
        private String address;


        private Double latitude;

        private Double longitude;

        @Schema(name = "경기 성별 유형", example = "MALE_ONLY")
        @NotNull(message = "성별을 입력해주세요.")
        private MatchGenderType matchGenderType;


    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response {

        @Schema(name = "경기 PK", example = "1")
        private Long gameId;

        @Schema(name = "경기 제목", example = "서울 xx체육관 3vs3 경기 인원 모집", defaultValue = "서울 xx체육관 3vs3 경기 인원 모집")
        private String title;

        @Schema(name = "경기 상세 내용", example = "3대3경기 인원 모집입니다.", defaultValue = "3대3경기 인원 모집입니다.")
        private String content;

        @Schema(name = "경기 인원 수", example = "6")
        private int headCount;

        @Schema(name = "경기 참가 인원 수", example = "1", defaultValue = "1")
        private int participantCount;

        @Schema(name = "경기 실내외", example = "INDOOR")
        private FieldStatus fieldStatus;

        @Schema(name = "경기 형식", example = "THREE_ON_THREE")
        private MatchFormat matchFormat;

        @Schema(name = "경기 상태", example = "RECRUITING")
        private GameStatus gameStatus;

        @Schema(name = "경기 시작 날짜", example = "2025-11-22T15:00:00:00")
        private LocalDateTime startDateTime;

        @Schema(name = "경기 장소", example = "서울 xx 체육관")
        private String placeName;

        @Schema(name = "경기 주소", example = "서울특별시 강남구..")
        private String address;

        private Double latitude;

        private Double longitude;

        @Schema(name = "도시 이름", example = "INCHEON")
        private CityName cityName;

        @Schema(name = "경기 성별 유형", example = "MALE_ONLY")
        private MatchGenderType matchGenderType;

        @Schema(name = "경기 생성자 아이디", example = "1")
        private Long creatorId;

        @Schema(name = "경기 생성자 닉네임", example = "커리")
        private String creatorNickname;

        public static Response fromDto(GameDto gameDto) {

            return Response.builder()
                    .gameId(gameDto.getGameId())
                    .title(gameDto.getTitle())
                    .content(gameDto.getContent())
                    .headCount(gameDto.getHeadCount())
                    .participantCount(gameDto.getParticipantCount())
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
