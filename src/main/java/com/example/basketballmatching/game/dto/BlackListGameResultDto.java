package com.example.basketballmatching.game.dto;

import java.util.List;

public record BlackListGameResultDto(
        List<GameCancelNotificationDto> notices
) {
    public BlackListGameResultDto {
        notices = List.copyOf(notices);
    }

}
