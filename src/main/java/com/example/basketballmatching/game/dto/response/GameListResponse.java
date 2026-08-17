package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.type.GameStatus;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GameListResponse(
        @Schema(description = "경기 ID", example = "1")
        Long gameId,

        @Schema(description = "경기 제목")
        String title,

        @Schema(description = "장소명")
        String placeName,

        @Schema(description = "주소")
        String address,

        @Schema(description = "경기 시작 시각")
        LocalDateTime startDateTime,

        @Schema(description = "경기 정원", example = "6")
        int headCount,

        @Schema(description = "현재 참가 인원", example = "1")
        int participantCount,

        @Schema(description = "참가 성별 조건")
        MatchGenderType matchGenderType,

        @Schema(description = "경기 형식")
        MatchFormat matchFormat,

        @Schema(description = "경기 상태")
        GameStatus gameStatus

) {

    public static GameListResponse fromEntity(GameEntity game) {

        return new GameListResponse(
                game.getGameId(),
                game.getTitle(),
                game.getPlaceName(),
                game.getAddress(),
                game.getStartDateTime(),
                game.getHeadCount(),
                game.getParticipantCount(),
                game.getMatchGenderType(),
                game.getMatchFormat(),
                game.getGameStatus()
        );
    }
}
