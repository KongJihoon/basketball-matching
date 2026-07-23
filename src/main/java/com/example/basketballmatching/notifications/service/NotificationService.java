package com.example.basketballmatching.notifications.service;

import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.notifications.dto.NotificationDto;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.domain.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface NotificationService {


    SseEmitter subscribe(Long userId, String lastEventId);

    void send(NotificationType notificationType, UserEntity userEntity, String content);


    CommonResponse<List<NotificationDto>> getUnReadNotifications(Long userId, Pageable pageable);



}
