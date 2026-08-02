package com.example.basketballmatching.game.event;

public record GameCreateEvent(
        Long gameId, Long creatorId, String title
) {
}
