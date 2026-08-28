package com.example.basketballmatching.notifications.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.dto.response.NotificationResponse;
import com.example.basketballmatching.notifications.dto.response.ReadNotificationResponse;
import com.example.basketballmatching.notifications.repository.EmitterRepository;
import com.example.basketballmatching.notifications.repository.NotificationQueryRepository;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.notifications.support.SseIdGenerator;
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

    private final SseIdGenerator sseIdGenerator;

    private final Clock clock;

    public SseEmitter subscribe(Long userId, String lastEventId) {

        getActiveUser(userId);

        String emitterId = sseIdGenerator.createEmitterId(userId);

        SseEmitter emitter = emitterRepository.saveEmitter(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        registerEmitterCallbacks(emitter, emitterId);

        sendConnectionEvent(emitter, emitterId);


        if (!lastEventId.isBlank()) {
            resendMissedEvents(userId, lastEventId, emitterId, emitter);
        }


        return emitter;
    }

    @Transactional
    public void send(NotificationType notificationType, UserEntity userEntity, String content) {

        NotificationEntity notification = NotificationEntity.create(
                userEntity, notificationType, content
        );

        NotificationEntity savedNotification = notificationRepository.save(notification);

        NotificationResponse response = NotificationResponse.fromEntity(savedNotification);


        Long receiverId = userEntity.getUserId();

        String eventId = sseIdGenerator.createEventId(receiverId);

        emitterRepository.saveEvent(eventId, response);


        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmittersByUserId(receiverId);

        sseEmitters.forEach(
                (emitterId, emiter) -> sendToClient(emiter, emitterId, eventId, response)
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

    private void registerEmitterCallbacks(SseEmitter emitter, String emitterId) {

        emitter.onCompletion(() -> emitterRepository.deleteEmitter(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteEmitter(emitterId));
        emitter.onError(exception -> emitterRepository.deleteEmitter(emitterId));

    }

    private void resendMissedEvents(Long userId, String lastEventId, String emitterId, SseEmitter emitter) {

        Map<String, Object> events = emitterRepository.findAllEventsByUserId(userId);

        events.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                .forEach(entry ->
                        sendToClient(emitter, emitterId, entry.getKey(), entry.getValue()));

    }

    private void sendConnectionEvent(SseEmitter emitter, String emitterId) {

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connect")
                            .data("SSE 연결이 완료되었습니다.")
            );
        } catch (IOException e) {
            emitterRepository.deleteEmitter(emitterId);

            log.warn("[SSE 연결 이벤트 전송 실패] emitterId={}", emitterId, e);
        }
    }


    private void sendToClient(SseEmitter emitter, String emitterId, String eventId,Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("notification")
                    .data(data));
        } catch (IOException e) {
            emitterRepository.deleteEmitter(emitterId);

            log.warn("[SSE 알림 전송 실패] emitterId={} eventId={}", emitterId,eventId, e);

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
