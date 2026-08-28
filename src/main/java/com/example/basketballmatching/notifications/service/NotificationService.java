package com.example.basketballmatching.notifications.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.dto.response.NotificationResponse;
import com.example.basketballmatching.notifications.dto.response.ReadNotificationResponse;
import com.example.basketballmatching.notifications.repository.EmitterRepository;
import com.example.basketballmatching.notifications.repository.NotificationQueryRepository;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.example.basketballmatching.global.exception.ErrorCode.NOTIFICATION_NOT_FOUND;
import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {
    private static final Long DEFAULT_TIMEOUT = 30L * 1000 * 60;

    private final EmitterRepository emitterRepository;

    private final NotificationRepository notificationRepository;

    private final NotificationQueryRepository notificationQueryRepository;

    private final UserRepository userRepository;

    private final Clock clock;

    public SseEmitter subscribe(Long userId, String lastEventId) {

        getActiveUser(userId);

        String emitterId = userId + "_" + System.currentTimeMillis();

        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(
                () -> emitterRepository.deleteByEmitterId(emitterId)
        );

        emitter.onTimeout(() -> emitterRepository.deleteByEmitterId(emitterId));
        emitter.onError((e) -> emitterRepository.deleteByEmitterId(emitterId));


        sendToClient(emitter, emitterId,
                "EventStream Created. [emitterId = " + emitterId + "]");

        if (!lastEventId.isEmpty()) {
            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithUserId(
                    String.valueOf(userId)
            );

            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(
                            entry -> sendToClient(emitter, entry.getKey(), entry.getValue())
                    );
        }


        return emitter;
    }

    @Transactional
    public void send(NotificationType notificationType, UserEntity userEntity, String content) {

        NotificationEntity notification = NotificationEntity.create(
                userEntity, notificationType, content
        );

        NotificationEntity savedNotification = notificationRepository.save(notification);

        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllStartWithByUserId(
                String.valueOf(userEntity.getUserId())
        );

        sseEmitters.forEach(
                (key, emitter) -> {
                    emitterRepository.saveEventCache(key, savedNotification);
                    sendToClient(emitter, key, NotificationResponse.fromEntity(savedNotification));
                }
        );


    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnReadNotifications(Long userId, Pageable pageable) {

        getActiveUser(userId);


        return notificationQueryRepository
                .findUnreadNotifications(userId, pageable);
    }

    @Transactional
    public ReadNotificationResponse readNotification(Long userId, Long notificationId) {
        getActiveUser(userId);


        NotificationEntity notification = notificationRepository.findByNotificationIdAndReceiver_UserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(NOTIFICATION_NOT_FOUND));

        notification.markAsRead(LocalDateTime.now(clock));


        return ReadNotificationResponse.fromEntity(notification);
    }


    private void sendToClient(SseEmitter sseEmitter, String emitterId, Object data) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(data));
        } catch (IOException e) {
            emitterRepository.deleteByEmitterId(emitterId);

            log.warn("[SSE 알림 전송 실패] emitterId={}", emitterId, e);

        }
    }

    private UserEntity getActiveUser(Long userId) {
        return userRepository
                .findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(
                        () -> new CustomException(USER_NOT_FOUND)
                );
    }




}
