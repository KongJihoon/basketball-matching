package com.example.basketballmatching.global.lock;

import com.example.basketballmatching.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.example.basketballmatching.global.exception.ErrorCode.LOCK_BY_GAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockExecutor {

    private final RedissonClient redissonClient;

    public <T> T executeWithLock(String lockKey, long waitTimeMs, long leastTimeMs, Callable<T> action) {


        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTimeMs, leastTimeMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("[LOCK 획득 실패] key = {}", lockKey);
                throw new CustomException(LOCK_BY_GAME);
            }

            log.info("[LOCK 획득 성공] key = {}", lockKey);

            return action.call();

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CustomException(LOCK_BY_GAME);
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.info("[Lock 해제 성공] key = {}", lockKey);
                } catch (Exception e) {
                    log.error("[RedissonLockExecutor] unlock 실패 = {}", lockKey);
                }
            }
        }


    }

}
