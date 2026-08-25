package com.example.basketballmatching.blacklist.event;

import com.example.basketballmatching.auth.service.AuthTokenStore;
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
public class UserBlacklistedEventListener {

    private final AuthTokenStore authTokenStore;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserBlacklistedEvent event) {

        deleteRefreshToken(event);
        sendRestrictionNotification(event);
        sendGameCancelNotification(event);

    }

    private void deleteRefreshToken(UserBlacklistedEvent event) {
        try {
            authTokenStore.deleteRefreshToken(event.email());
        } catch (Exception e) {
            log.error("[블랙리스트 사용자 RefreshToken 삭제 실패] userId={}", event.userId(), e);
        }
    }

    private void sendRestrictionNotification(UserBlacklistedEvent event) {

        try {
            UserEntity targetUser = userRepository.findById(event.userId())
                    .orElse(null);

            if (targetUser == null) {
                log.warn("[블랙리스트 알림 대상 사용자 없음] userId={}", event.userId());
                return;
            }

            String content = "신고 검토 결과 " + event.expiresAt() + "까지 서비스 이용이 제한되었습니다.";

            notificationService.send(
                    NotificationType.BLACKLISTED, targetUser, content
            );
        } catch (Exception e) {
            log.error("[블랙리스트 제재 알림 발송 실패] userId={}", event.userId(), e);
        }
    }

    private void sendGameCancelNotification(UserBlacklistedEvent event) {

        for (GameCancelNotificationDto notice : event.notices()) {

            try {
                UserEntity receiver = userRepository.findByUserIdAndDeletedDateTimeIsNull(notice.receiverId())
                        .orElse(null);

                if (receiver == null) {
                    log.warn("[경기 취소 알림 수신자 없음] userId={}", notice.receiverId());
                    continue;
                }

                notificationService.send(NotificationType.DELETE_GAME, receiver, notice.getContent());
            } catch (Exception e) {
                log.error("[블랙리스트 경기 취소 알림 실패] userId={}", notice.receiverId(), e);
            }

        }
    }
}
