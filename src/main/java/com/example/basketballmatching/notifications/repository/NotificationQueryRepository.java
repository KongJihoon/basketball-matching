package com.example.basketballmatching.notifications.repository;


import com.example.basketballmatching.notifications.dto.NotificationDto;
import com.example.basketballmatching.notifications.entity.NotificationEntity;
import com.example.basketballmatching.notifications.entity.QNotificationEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepository {

    private final NotificationRepository notificationRepository;

    private final JPAQueryFactory jpaQueryFactory;

    public List<NotificationDto> getUnReadNotification(Long userId, Pageable pageable) {

        QNotificationEntity notificationEntity = QNotificationEntity.notificationEntity;


        BooleanBuilder builder = new BooleanBuilder();

        builder.and(notificationEntity.receiver.userId.eq(userId));
        builder.and(notificationEntity.isRead.eq(false));
        builder.and(notificationEntity.readDateTime.isNull());

        List<NotificationEntity> notificationEntities = jpaQueryFactory
                .select(notificationEntity)
                .from(notificationEntity)
                .where(builder)
                .orderBy(notificationEntity.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        for (NotificationEntity notification : notificationEntities) {

            notification.setReadAndReadDateTime(LocalDateTime.now());
            notificationRepository.save(notification);
        }


        return notificationEntities.stream()
                .map(NotificationDto::fromEntity)
                .toList();
    }

}
