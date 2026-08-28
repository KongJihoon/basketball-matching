package com.example.basketballmatching.notifications.redis;

import com.example.basketballmatching.notifications.dto.response.GameCreatedNotificationMessage;
import com.example.basketballmatching.notifications.repository.EmitterRepository;
import com.example.basketballmatching.notifications.support.SseIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final EmitterRepository emitterRepository;
    private final SseIdGenerator sseIdGenerator;



    @Override
    public void onMessage(Message redisMessage, byte[] pattern) {

        try {

            String json = new String(redisMessage.getBody(), StandardCharsets.UTF_8);
            GameCreatedNotificationMessage message = objectMapper.readValue(json, GameCreatedNotificationMessage.class);


            Long receiverId = message.receiverUserId();

            String eventId = sseIdGenerator.createEventId(receiverId);

            emitterRepository.saveEvent(eventId, message);

            Map<String, SseEmitter> emitters = emitterRepository.findAllEmittersByUserId(receiverId);


            emitters.forEach(
                    (emitterId, emitter) ->
                            sendToClient(emitter, emitterId, eventId, message)
            );


        } catch (Exception e) {
            log.error("[Redis 알림 메시지 처리 실패]", e);
        }

    }

    private void sendToClient(SseEmitter emitter, String emitterId, String eventId, Object data) {

        try {

            emitter.send(
                    SseEmitter.event()
                            .id(eventId)
                            .name("notification")
                            .data(data)
            );

        } catch (IOException e) {
            emitterRepository.deleteEmitter(emitterId);

            log.warn("[Redis SSE 알림 전송 실패] emitterId={}, eventId={}", emitterId, eventId, e);

        }

    }
}
