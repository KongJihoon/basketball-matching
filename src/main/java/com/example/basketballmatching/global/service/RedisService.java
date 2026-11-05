package com.example.basketballmatching.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final DefaultRedisScript<Long> COMPARE_AND_SET_SCRIPT =
            new DefaultRedisScript<>("""
                local current = redis.call('GET', KEYS[1])

                if not current or current ~= ARGV[1] then
                    return 0
                end

                local expiresAt = tonumber(ARGV[3])
                local time = redis.call('TIME')
                local now = tonumber(time[1]) * 1000
                        + math.floor(tonumber(time[2]) / 1000)

                if expiresAt <= now then
                    return 0
                end

                redis.call(
                    'SET', KEYS[1], ARGV[2],
                    'PXAT', ARGV[3]
                )

                return 1
                """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public void setDataExpireMinutes(String key, String value, Long expiredTime) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        valueOperations.set(key, value, Duration.ofMinutes(expiredTime));
    }

    public void setDataExpireMillis(String key, String value, Long expiredTime) {

        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        valueOperations.set(key, value, Duration.ofMillis(expiredTime));

    }


    public String getData(String key) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        return valueOperations.get(key);
    }

    public Long getExpiration(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }




    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    public String getAndDeleteData(String key) {

        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        return valueOperations.getAndDelete(key);

    }

    public boolean compareAndSetUtil(String key, String expectedValue, String newValue, long expiresAtMillis) {

        Long result = redisTemplate.execute(
                COMPARE_AND_SET_SCRIPT,
                List.of(key),
                expectedValue,
                newValue,
                Long.toString(expiresAtMillis)
        );


        return Long.valueOf(1L).equals(result);
    }

}
