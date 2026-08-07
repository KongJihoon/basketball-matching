package com.example.basketballmatching.game.event;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateGameEvent(
        Long gameId,
        String title,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        List<Long> receiverIds
) {

    public UpdateGameEvent {
        receiverIds = List.copyOf(receiverIds);
    }

}
