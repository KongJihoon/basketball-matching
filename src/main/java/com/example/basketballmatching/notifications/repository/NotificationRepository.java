package com.example.basketballmatching.notifications.repository;

import com.example.basketballmatching.notifications.dto.NotificationDto;
import com.example.basketballmatching.notifications.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {




}
