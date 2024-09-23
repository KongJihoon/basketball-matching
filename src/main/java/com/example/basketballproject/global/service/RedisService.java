package com.example.basketballproject.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;


    public String getData(String key) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        return (String) valueOperations.get(key);
    }

    public void setDataExpire(String key, String value, Long expiredTime) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value, Duration.ofMillis(expiredTime));
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }


    public void findByLoginId(String loginId) {
        redisTemplate.opsForValue().get(loginId);
    }

}
