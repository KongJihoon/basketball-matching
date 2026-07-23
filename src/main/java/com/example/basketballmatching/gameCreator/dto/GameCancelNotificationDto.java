package com.example.basketballmatching.gameCreator.dto;

public record GameCancelNotificationDto(Long receiverId, String gameTitle) {

    public String getContent() {
        return gameTitle + "의 게임이 취소되었습니다.";
    }
}
