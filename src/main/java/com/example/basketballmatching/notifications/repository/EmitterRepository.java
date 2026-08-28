package com.example.basketballmatching.notifications.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EmitterRepository {

    private static final Duration EVENT_CACHE_TTL = Duration.ofMinutes(5);

    private final Clock clock;


    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, CacheEvent> eventCache = new ConcurrentHashMap<>();


    public SseEmitter saveEmitter(String emitterId, SseEmitter emitter) {
        emitters.put(emitterId, emitter);
        return emitter;
    }


    public void saveEvent(String eventId, Object event) {

        removeExpireEvents();

        eventCache.put(eventId, new CacheEvent(event, Instant.now(clock)));
    }

    public Map<String, SseEmitter> findAllEmittersByUserId(Long userId) {

        String emitterPrefix = userId + "_emitter_";


        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(emitterPrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    public Map<String, Object> findAllEventsByUserId(Long userId) {

        removeExpireEvents();

        String eventPrefix = userId + "_event_";


        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(eventPrefix))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        entry.getValue().data(), (first, second) -> first, LinkedHashMap::new));

    }


    public void deleteEmitter(String emitterId) {
        emitters.remove(emitterId);
    }



    private void removeExpireEvents() {
        Instant expirationTime = Instant.now(clock).minus(EVENT_CACHE_TTL);

        eventCache.entrySet().removeIf(
                entry -> entry.getValue().cacheAt.isBefore(expirationTime)
        );
    }

    private record CacheEvent(
            Object data, Instant cacheAt
    ) {}
}
