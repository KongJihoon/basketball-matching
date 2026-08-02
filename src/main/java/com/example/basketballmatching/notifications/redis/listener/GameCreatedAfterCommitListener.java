package com.example.basketballmatching.notifications.redis.listener;


import com.example.basketballmatching.game.event.GameCreateEvent;
import com.example.basketballmatching.notifications.dto.GameCreateSuccessSseDto;
import com.example.basketballmatching.notifications.type.RedisTopic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameCreatedAfterCommitListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(GameCreateEvent event) {

        // 커밋 성공한 경우에만 실행
        GameCreateSuccessSseDto notification = GameCreateSuccessSseDto.of(event.creatorId(), event.gameId(), event.title());

        try {
            String json = objectMapper.writeValueAsString(notification);

            stringRedisTemplate.convertAndSend(RedisTopic.Game_CREATED.getValue(),json);

        } catch (Exception e) {
            log.error("[GameCreatedAfterCommitListener] Redis publish fail", e);
        }


    }

}
