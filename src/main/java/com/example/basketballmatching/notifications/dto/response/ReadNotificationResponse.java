package com.example.basketballmatching.notifications.dto.response;

import com.example.basketballmatching.notifications.domain.NotificationEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ReadNotificationResponse(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId,

        @Schema(description = "읽음 여부", example = "true")
        boolean isRead,

        @Schema(description = "알림을 읽은 시각", example = "2026-08-27T15:35:00")
        LocalDateTime readDateTime
) {

    public static ReadNotificationResponse fromEntity(NotificationEntity notification) {
        return new ReadNotificationResponse(
                notification.getNotificationId(),
                notification.isRead(),
                notification.getReadDateTime()
        );
    }

}
