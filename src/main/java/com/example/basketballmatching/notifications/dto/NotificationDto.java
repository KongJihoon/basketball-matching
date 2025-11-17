package com.example.basketballmatching.notifications.dto;

import com.example.basketballmatching.notifications.entity.NotificationEntity;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class NotificationDto {

    private Long notificationId;

    private NotificationType notificationType;

    private String content;

    private LocalDateTime createAt;

    public static NotificationDto fromEntity(NotificationEntity notificationEntity) {

        return NotificationDto.builder()
                .notificationId(notificationEntity.getNotificationId())
                .notificationType(notificationEntity.getNotificationType())
                .content(notificationEntity.getContent())
                .createAt(notificationEntity.getCreatedAt())
                .build();
    }




}
