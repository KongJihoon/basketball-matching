package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.type.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GameDetailResponse(
        @Schema(description = "경기 ID", example = "1")
        Long gameId,

        @Schema(description = "경기 제목")
        String title,

        @Schema(description = "경기 상세 내용")
        String content,

        @Schema(description = "경기 정원", example = "6")
        int headCount,

        @Schema(description = "현재 참가 인원", example = "1")
        int participantCount,

        @Schema(description = "실내·실외 구분", example = "INDOOR")
        FieldStatus fieldStatus,

        @Schema(description = "경기 형식", example = "THREE_ON_THREE")
        MatchFormat matchFormat,

        @Schema(description = "경기 상태", example = "RECRUITING")
        GameStatus gameStatus,

        @Schema(
                description = "경기 시작 시각",
                example = "2026-11-22T15:00:00"
        )
        LocalDateTime startDateTime,

        @Schema(
                description = "경기 종료 시각",
                example = "2026-11-22T17:00:00"
        )
        LocalDateTime endDateTime,

        @Schema(description = "경기 장소명")
        String placeName,

        @Schema(description = "경기 주소")
        String address,

        @Schema(description = "위도")
        Double latitude,

        @Schema(description = "경도")
        Double longitude,

        @Schema(description = "지역", example = "SEOUL")
        CityName cityName,

        @Schema(description = "참가 성별 조건", example = "MIXED")
        MatchGenderType matchGenderType,

        @Schema(description = "경기 생성자 ID", example = "1")
        Long creatorId,

        @Schema(description = "경기 생성자 닉네임")
        String creatorNickname,

        @Schema(description = "참가자 수준")
        GameUserLevel gameUserLevel

) {

    public static GameDetailResponse fromEntity(GameEntity game) {
        return new GameDetailResponse(
                game.getGameId(),
                game.getTitle(),
                game.getContent(),
                game.getHeadCount(),
                game.getParticipantCount(),
                game.getFieldStatus(),
                game.getMatchFormat(),
                game.getGameStatus(),
                game.getStartDateTime(),
                game.getEndDateTime(),
                game.getPlaceName(),
                game.getAddress(),
                game.getLatitude(),
                game.getLongitude(),
                game.getCityName(),
                game.getMatchGenderType(),
                game.getUserEntity().getUserId(),
                game.getUserEntity().getNickname(),
                game.getGameUserLevel()
        );
    }

}
