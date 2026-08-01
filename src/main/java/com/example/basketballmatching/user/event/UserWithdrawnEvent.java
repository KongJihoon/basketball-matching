package com.example.basketballmatching.user.event;

import com.example.basketballmatching.game.dto.GameCancelNotificationDto;

import java.util.List;

public record UserWithdrawnEvent(
        Long userId,
        String email,
        String accessToken,
        List<GameCancelNotificationDto> notices
) {

    public UserWithdrawnEvent {
        notices = List.copyOf(notices);
    }
}
