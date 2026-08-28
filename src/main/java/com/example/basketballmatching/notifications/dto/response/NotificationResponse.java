package com.example.basketballmatching.notifications.dto.response;

import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.type.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId,

        @Schema(description = "알림 유형", example = "UPDATE_GAME")
        NotificationType notificationType,

        @Schema(description = "알림 내용", example = "'주말 농구 경기' 경기 정보가 수정되었습니다.")
        String content,

        @Schema(description = "알림 생성 시각", example = "2026-08-27T15:30:00")
        LocalDateTime createdAt
) {

    public static NotificationResponse fromEntity(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getContent(),
                notification.getCreatedAt()
        );
    }

}
