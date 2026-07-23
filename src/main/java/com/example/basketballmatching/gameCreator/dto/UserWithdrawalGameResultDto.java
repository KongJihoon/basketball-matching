package com.example.basketballmatching.gameCreator.dto;

import java.util.List;

public record UserWithdrawalGameResultDto(List<GameCancelNotificationDto> notices) {

    public UserWithdrawalGameResultDto {
        notices = List.copyOf(notices);
    }

    public static UserWithdrawalGameResultDto empty() {
        return new UserWithdrawalGameResultDto(
                List.of()
        );
    }

}
