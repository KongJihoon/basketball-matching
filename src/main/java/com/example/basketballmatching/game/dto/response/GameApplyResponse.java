package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GameApplyResponse(
        @Schema(description = "경기 참가 관계 ID", example = "1")
        Long participationId,

        @Schema(description = "경기 ID", example = "1")
        Long gameId,

        @Schema(description = "참가 상태", example = "APPLY")
        ParticipantGameStatus status,

        @Schema(description = "신청 시각", example = "2026-08-09T15:00:00")
        LocalDateTime appliedAt
) {

    public static GameApplyResponse fromEntity(ParticipantGameEntity participation) {

        return new GameApplyResponse(
                participation.getParticipantGameId(),
                participation.getGameEntity().getGameId(),
                participation.getParticipantGameStatus(),
                participation.getApplyDateTime()
        );
    }

}
