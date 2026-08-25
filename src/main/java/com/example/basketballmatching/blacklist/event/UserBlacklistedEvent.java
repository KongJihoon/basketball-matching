package com.example.basketballmatching.blacklist.event;

import com.example.basketballmatching.game.dto.GameCancelNotificationDto;

import java.time.LocalDateTime;
import java.util.List;

public record UserBlacklistedEvent(
        Long userId,

        String email,

        LocalDateTime bannedAt,

        LocalDateTime expiresAt,

        List<GameCancelNotificationDto> notices
) {
    public UserBlacklistedEvent {
        notices = List.copyOf(notices);
    }
}
