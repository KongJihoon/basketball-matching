package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.Position;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public record GameParticipantListResponse(

        @Schema(description = "경기 참가 관계 ID", example = "1")
        Long participationId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 닉네임", example = "농구왕")
        String nickname,


        @Schema(description = "선호 포지션", example = "GUARD")
        Position position,

        @Schema(description = "경기 생성자 여부", example = "false")
        boolean creator

) {

    public static GameParticipantListResponse fromEntity(ParticipantGameEntity participation, Long creatorId) {

        UserEntity participant = participation.getUserEntity();

        return new GameParticipantListResponse(
                participation.getParticipantGameId(),
                participant.getUserId(),
                participant.getNickname(),
                participant.getPosition(),
                Objects.equals(participant.getUserId(), creatorId)
        );

    }

}
