package com.example.basketballmatching.global.cache.version;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSearchCacheVersionService {

    private final StringRedisTemplate redisTemplate;

    private static final String VERSION_KEY = "cache:gameSearch:version";


    public long getVersion() {

        String version = redisTemplate.opsForValue().get(VERSION_KEY);

        if (version == null) {
            redisTemplate.opsForValue().set(VERSION_KEY, "1");
            return 1L;
        }

        try {

            long parsed = Long.parseLong(version);

            if (parsed < 1L) {
                redisTemplate.opsForValue().set(VERSION_KEY, "1");
                return 1L;
            }


        } catch (NumberFormatException e) {
            redisTemplate.opsForValue().set(VERSION_KEY, "1");
            return 1L;
        }

        return Long.parseLong(version);
    }

    public long bump() {
        Long version = redisTemplate.opsForValue().increment(VERSION_KEY);

        if (version == null || version < 1L) {
            redisTemplate.opsForValue().set(VERSION_KEY, "1");
            return 1L;
        }

        return version;

    }

}
