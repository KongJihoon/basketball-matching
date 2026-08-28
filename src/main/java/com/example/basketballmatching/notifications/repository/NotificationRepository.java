package com.example.basketballmatching.notifications.repository;

import com.example.basketballmatching.notifications.domain.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {


    Optional<NotificationEntity> findByNotificationIdAndReceiver_UserId(Long notificationId, Long userId);

}
