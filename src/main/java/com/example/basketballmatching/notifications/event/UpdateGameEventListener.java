package com.example.basketballmatching.notifications.event;

import com.example.basketballmatching.game.event.UpdateGameEvent;
import com.example.basketballmatching.notifications.service.NotificationService;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateGameEventListener {

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UpdateGameEvent event) {


        for (Long receiverId : event.receiverIds()) {
            UserEntity receiver = userRepository.findByUserIdAndDeletedDateTimeIsNull(receiverId)
                    .orElse(null);

            if (receiver == null) {
                log.warn("경기 수정 알림 수신자를 찾을 수 없음: gameId={}, receiverId={}", event.gameId(), receiverId);
                continue;
            }

            notificationService.send(NotificationType.UPDATE_GAME, receiver, createContent(event));
        }

    }

    private String createContent(UpdateGameEvent event) {

        return "'" + event.title() + "' 경기 정보가 수정되었습니다.";


    }

}
