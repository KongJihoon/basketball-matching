package com.example.basketballmatching.notifications.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class EmitterRepository {

    public final Map<String, SseEmitter> emitter = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();


    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitter.put(emitterId, sseEmitter);
        return sseEmitter;
    }


    public void saveEventCache(String emitterId, Object event) {
        eventCache.put(emitterId, event);
    }

    public Map<String, SseEmitter> findAllStartWithByUserId(String userId) {

        return emitter.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    public Map<String, Object> findAllEventCacheStartWithUserId(String userId) {


        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    public void deleteAllStratWithUserId(String userId) {
        emitter.forEach(
                (key, value) -> {
                    if (key.startsWith(userId)) {
                        emitter.remove(key);
                    }
                }
        );
    }

    public void deleteByEmitterId(String emitterId) {
        emitter.remove(emitterId);
    }

    public void deleteAllEventCacheStartWithUserId(String userId) {

        eventCache.forEach(
                (key, value) -> {
                    if (key.startsWith(userId)) {
                        eventCache.remove(key);
                    }
                }
        );
    }
}
