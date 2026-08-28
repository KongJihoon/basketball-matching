package com.example.basketballmatching.notifications.repository;


import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.domain.QNotificationEntity;
import com.example.basketballmatching.notifications.dto.response.NotificationResponse;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {


    private final JPAQueryFactory jpaQueryFactory;

    public List<NotificationResponse> findUnreadNotifications(Long userId, Pageable pageable) {

        QNotificationEntity notification = QNotificationEntity.notificationEntity;


        List<NotificationEntity> notifications = jpaQueryFactory
                .selectFrom(notification)
                .where(
                        notification.receiver.userId.eq(userId),
                        notification.isRead.isFalse(),
                        notification.readDateTime.isNull()
                )
                .orderBy(notification.createdAt.desc(), notification.notificationId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

}
