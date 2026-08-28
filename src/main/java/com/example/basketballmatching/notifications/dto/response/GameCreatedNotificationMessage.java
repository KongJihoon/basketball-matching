package com.example.basketballmatching.notifications.dto.response;

public record GameCreatedNotificationMessage(
        Long receiverUserId,
        Long gameId,
        String title,
        String message,
        Long createdAtEpochMs
) {

    private static final String CREATED_MESSAGE = "경기 생성이 완료되었습니다.";

    public static GameCreatedNotificationMessage create(Long receiverUserId, Long gameId, String title, Long createdAt) {
        return new GameCreatedNotificationMessage(
                receiverUserId, gameId,title, CREATED_MESSAGE, createdAt
        );
    }
}
