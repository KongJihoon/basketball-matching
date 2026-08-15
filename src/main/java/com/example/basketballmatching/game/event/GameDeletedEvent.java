package com.example.basketballmatching.game.event;

import java.util.List;

public record GameDeletedEvent(
        Long gameId,
        String gameTitle,
        List<Long> receiverIds
) {

    public GameDeletedEvent {
        receiverIds = List.copyOf(receiverIds);
    }
}
