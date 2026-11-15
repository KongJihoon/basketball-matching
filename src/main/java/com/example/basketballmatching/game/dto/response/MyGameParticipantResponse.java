package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.type.ParticipantGameStatus;

public record MyGameParticipantResponse(
        Long participantId,
        ParticipantGameStatus status
) {
}
