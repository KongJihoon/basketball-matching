package com.example.basketballmatching.notifications.redis;

import com.example.basketballmatching.notifications.dto.GameCreateSuccessSseDto;
import com.example.basketballmatching.notifications.repository.EmitterRepository;
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



    @Override
    public void onMessage(Message message, byte[] pattern) {

        try {

            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            GameCreateSuccessSseDto gameCreateSuccessSseDto = objectMapper.readValue(json, GameCreateSuccessSseDto.class);

            Map<String, SseEmitter> emitters = emitterRepository.findAllStartWithByUserId(
                    String.valueOf(gameCreateSuccessSseDto.getReceiverUserId())
            );

            if (emitters.isEmpty()) {
                return;
            }

            emitters.forEach((emitterId, emitter) -> {
                emitterRepository.saveEventCache(emitterId, gameCreateSuccessSseDto);

                try {
                    emitter.send(SseEmitter.event()
                            .id(emitterId)
                            .name("sse")
                            .data(gameCreateSuccessSseDto));
                } catch (IOException e) {
                    emitterRepository.deleteByEmitterId(emitterId);
                }
            });

        } catch (Exception e) {
            log.error("[NotificationRedisSubscriber] message process fail", e);
        }

    }
}
