package com.example.basketballmatching.notifications.event;

import com.example.basketballmatching.game.event.GameParticipantKickedOutEvent;
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
public class GameParticipantKickoutEventListener {

    private final UserRepository userRepository;
    private final NotificationService notificationService;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(GameParticipantKickedOutEvent event) {

        UserEntity participant = userRepository.findByUserIdAndDeletedDateTimeIsNull(event.participantUserId())
                .orElse(null);

        if (participant == null) {
            log.warn("강퇴 알림 대상 사용자를 찾을 수 없습니다. userId={}", event.participantUserId());
            return;
        }

        notificationService.send(
                NotificationType.KICKED_OUT, participant, "'" + event.gameTitle() + "' 경기에서 강퇴되었습니다."
        );

    }

}
