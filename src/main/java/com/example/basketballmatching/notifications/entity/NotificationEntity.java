package com.example.basketballmatching.notifications.entity;

import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
    @Builder.Default
    private boolean isRead = false;


    private LocalDateTime readDateTime;


    public void setReadAndReadDateTime(LocalDateTime readDateTime) {
        this.isRead = true;
        this.readDateTime = readDateTime;
    }

}
