package com.example.basketballmatching.notifications.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class SseIdGenerator {

    private final Clock clock;

    private final AtomicLong sequence = new AtomicLong();

    public String createEmitterId(Long userId) {
        return userId + "_emitter_" + UUID.randomUUID();
    }

    public String createEventId(Long userId) {
        return String.format("%d_event_%013d_%020d", userId, clock.millis(), sequence.incrementAndGet());
    }

}
