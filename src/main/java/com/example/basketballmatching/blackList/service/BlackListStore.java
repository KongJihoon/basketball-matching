package com.example.basketballmatching.blackList.service;

import com.example.basketballmatching.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlackListStore {

    private static final String BLACKLIST_PREFIX =
            "blackList:";

    private final RedisService redisService;

    public boolean isBlacklisted(String email) {

        return redisService.getData(blacklistKey(email)) != null;
    }

    private String blacklistKey(String email) {
        return BLACKLIST_PREFIX + email;
    }
}
