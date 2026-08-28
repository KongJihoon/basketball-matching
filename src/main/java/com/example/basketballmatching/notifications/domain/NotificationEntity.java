package com.example.basketballmatching.notifications.domain;

import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserEntity receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean isRead;

    private LocalDateTime readDateTime;

    @Builder(access = AccessLevel.PRIVATE)
    private NotificationEntity(UserEntity receiver, NotificationType notificationType, String content) {
        this.receiver = receiver;
        this.notificationType = notificationType;
        this.content = content;
        this.isRead = false;
    }

    public static NotificationEntity create(UserEntity receiver, NotificationType notificationType, String content) {
        return NotificationEntity.builder()
                .receiver(receiver)
                .notificationType(notificationType)
                .content(content)
                .build();
    }

    public void markAsRead(LocalDateTime readDateTime) {
        if (isRead) {
            return;
        }

        this.isRead = true;
        this.readDateTime = readDateTime;
    }

}
