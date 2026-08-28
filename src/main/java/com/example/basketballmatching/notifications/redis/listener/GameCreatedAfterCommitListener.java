package com.example.basketballmatching.notifications.redis.listener;


import com.example.basketballmatching.game.event.GameCreateEvent;
import com.example.basketballmatching.notifications.dto.response.GameCreatedNotificationMessage;
import com.example.basketballmatching.notifications.type.RedisTopic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameCreatedAfterCommitListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(GameCreateEvent event) {

        // 커밋 성공한 경우에만 실행
        GameCreatedNotificationMessage message = GameCreatedNotificationMessage.create(event.creatorId(), event.gameId(), event.title(), clock.millis());


        try {
            String json = objectMapper.writeValueAsString(message);

            stringRedisTemplate.convertAndSend(RedisTopic.GAME_CREATED.getValue(),json);

        } catch (Exception e) {

            log.error("[경기 생성 알림 발행 실패] gameId={}", event.gameId(), e);
        }


    }

}
