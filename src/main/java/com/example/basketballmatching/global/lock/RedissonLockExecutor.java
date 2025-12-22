package com.example.basketballmatching.global.lock;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
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

    public <T> T executeWithLock(String redisKey, long waitTimeMs, long leastTimeMs, Callable<T> action) {


        RLock lock = redissonClient.getLock(redisKey);

        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTimeMs, leastTimeMs, TimeUnit.MILLISECONDS);

            if (!acquired) {
                throw new CustomException(LOCK_BY_GAME);
            }
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
                } catch (Exception e) {
                    log.error("[RedissonLockExecutor] unlock 실패 = {}", redisKey);
                }
            }
        }


    }

}
