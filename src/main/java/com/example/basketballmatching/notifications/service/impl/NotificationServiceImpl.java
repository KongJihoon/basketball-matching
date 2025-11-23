package com.example.basketballmatching.notifications.service.impl;

import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.notifications.dto.NotificationDto;
import com.example.basketballmatching.notifications.entity.NotificationEntity;
import com.example.basketballmatching.notifications.repository.EmitterRepository;
import com.example.basketballmatching.notifications.repository.NotificationQueryRepository;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.notifications.service.NotificationService;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private static final Long DEFAULT_TIMEOUT = 30L * 1000 * 60;

    private final EmitterRepository emitterRepository;

    private final NotificationRepository notificationRepository;

    private final NotificationQueryRepository notificationQueryRepository;

    private final UserRepository userRepository;

    @Override
    public SseEmitter subscribe(Long userId, String lastEventId) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

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

    @Override
    @Transactional
    public void send(NotificationType notificationType, UserEntity userEntity, String content) {

        NotificationEntity notification = NotificationEntity.builder()
                .receiver(userEntity)
                .notificationType(notificationType)
                .content(content)
                .build();

        notificationRepository.save(notification);

        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllStartWithByUserId(
                String.valueOf(notification.getReceiver().getUserId())
        );

        sseEmitters.forEach(
                (key, emitter) -> {
                    emitterRepository.saveEventCache(key, notification);
                    sendToClient(emitter, key, NotificationDto.fromEntity(notification));
                }
        );


    }

    @Override
    @Transactional
    public CommonResponse<List<NotificationDto>> getUnReadNotifications(Long userId, Pageable pageable) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        List<NotificationDto> notificationDtoList = notificationQueryRepository.getUnReadNotification(userId, pageable);

        return CommonResponse.of("현재 읽지 않은 알림 조회에 성공하였습니다.", notificationDtoList);
    }

    private void sendToClient(SseEmitter sseEmitter, String emitterId, Object data) {
        try {
            sseEmitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(data));
        } catch (IOException e) {
            emitterRepository.deleteByEmitterId(emitterId);
        }
    }




}
