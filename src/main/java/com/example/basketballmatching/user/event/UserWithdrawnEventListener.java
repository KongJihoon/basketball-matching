package com.example.basketballmatching.user.event;

import com.example.basketballmatching.auth.service.UserSessionRevocationService;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
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
public class UserWithdrawnEventListener {

    private final UserSessionRevocationService sessionRevocationService;

    private final UserRepository userRepository;

    private final NotificationService notificationService;


    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserWithdrawnEvent event) {

        revokeSession(event);

        sendNotification(event);

    }

    private void revokeSession(UserWithdrawnEvent event) {
        try {

            sessionRevocationService.revokeAll(
                    event.email(), event.accessToken()
            );

        } catch (Exception e) {
            log.error(
                    "탈퇴 사용자 세션 폐기 실패 : userId={}", event.userId(),
                    e
            );
        }
    }

    private void sendNotification(UserWithdrawnEvent event) {
        for (GameCancelNotificationDto notice : event.notices()) {
            UserEntity receiver = userRepository.findByUserIdAndDeletedDateTimeIsNull(notice.receiverId())
                    .orElse(null);

            if (receiver == null) {
                log.warn("경기 취소 알림 수신자 없음 : receiverId={}", notice.receiverId());
                continue;
            }

            notificationService.send(
                    NotificationType.DELETE_GAME,
                    receiver,
                    notice.getContent()
            );
        }
    }
}
