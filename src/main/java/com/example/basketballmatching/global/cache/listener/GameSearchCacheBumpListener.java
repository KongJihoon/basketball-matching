package com.example.basketballmatching.global.cache.listener;

import com.example.basketballmatching.global.cache.event.GameSearchCacheBumpEvent;
import com.example.basketballmatching.global.cache.version.GameSearchCacheVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameSearchCacheBumpListener {

    private final GameSearchCacheVersionService versionService;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(GameSearchCacheBumpEvent gameSearchCacheBumpEvent) {
        long newVersion = versionService.bump();

        log.info("[검색 캐시 버전 Bump] newVersion = {}", newVersion);
    }
}
