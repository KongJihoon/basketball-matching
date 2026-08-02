package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.type.CityName;
import com.example.basketballmatching.game.type.FieldStatus;
import com.example.basketballmatching.game.type.GameStatus;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CreateGameResponse(

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

        @Schema(description = "실내·실외 구분")
        FieldStatus fieldStatus,

        @Schema(description = "경기 형식")
        MatchFormat matchFormat,

        @Schema(description = "참가 성별 조건")
        MatchGenderType matchGenderType,

        @Schema(description = "경기 상태")
        GameStatus gameStatus,

        @Schema(description = "경기 시작 시각")
        LocalDateTime startDateTime,

        @Schema(description = "경기 종료 시각")
        LocalDateTime endDateTime,

        @Schema(description = "경기 장소명")
        String placeName,

        @Schema(description = "경기 주소")
        String address,

        @Schema(description = "위도")
        Double latitude,

        @Schema(description = "경도")
        Double longitude,

        @Schema(description = "지역")
        CityName cityName,

        @Schema(description = "생성자 ID")
        Long creatorId,

        @Schema(description = "생성자 닉네임")
        String creatorNickname
) {

    public static CreateGameResponse fromEntity(
            GameEntity game
    ) {
        return new CreateGameResponse(
                game.getGameId(),
                game.getTitle(),
                game.getContent(),
                game.getHeadCount(),
                game.getParticipantCount(),
                game.getFieldStatus(),
                game.getMatchFormat(),
                game.getMatchGenderType(),
                game.getGameStatus(),
                game.getStartDateTime(),
                game.getEndDateTime(),
                game.getPlaceName(),
                game.getAddress(),
                game.getLatitude(),
                game.getLongitude(),
                game.getCityName(),
                game.getUserEntity().getUserId(),
                game.getUserEntity().getNickname()
        );
    }
}