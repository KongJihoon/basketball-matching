package com.example.basketballmatching.game.event;

public record GameParticipantKickedOutEvent(
        Long gameId,
        String gameTitle,
        Long participantUserId
) {
}
