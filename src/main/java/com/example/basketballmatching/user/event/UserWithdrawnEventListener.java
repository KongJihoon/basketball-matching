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


    /**
     * 회원 탈퇴 트랜잭션이 정상 커밋 후
     * 사용자 세션 폐기 후 경기 취소 알림 발송
     *
     * AFTER_COMMIT :
     * 회원 탈퇴 트랜잭션이 롤백될 경우 후처리를 실행하지 않는다.
     *
     * REQUIRES_NEW
     * 이미 종료된 탈퇴 트랜잭션과 분리된 새로운 트랜잭션에서 알림 데이터 저장
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserWithdrawnEvent event) {

        revokeSession(event);

        sendNotification(event);

    }

    /**
     * 탈퇴 사용자의 Access_Token과 Refresh_Token 폐기
     * 세션 폐기에 실패하더라도 알림 처리는 계속 진행한다.
     */
    private void revokeSession(UserWithdrawnEvent event) {
        try {

            sessionRevocationService.revokeAll(
                    event.email(), event.accessToken()
            );

        } catch (Exception e) {
            /*
             * 회원 탈퇴 트랜잭션은 이미 커밋되었으므로
             * 예외 처리를 하지 않고 실패 로그를 남긴 뒤에 알림 발송 진행
             */
            log.error(
                    "탈퇴 사용자 세션 폐기 실패 : userId={}", event.userId(),
                    e
            );
        }
    }

    /**
     * 탈퇴자가 생성한 경기의 취소 알림을 각 활성 참가자에게 발송
     */
    private void sendNotification(UserWithdrawnEvent event) {
        for (GameCancelNotificationDto notice : event.notices()) {
            UserEntity receiver = userRepository.findByUserIdAndDeletedDateTimeIsNull(notice.receiverId())
                    .orElse(null);

            if (receiver == null) {
                log.warn("경기 취소 알림 수신자 없음 : receiverId={}", notice.receiverId());
                continue;
            }

            // 경기 취소 알림을 DB에 저장하고 연결된 SSE 구독자에게 전송
            notificationService.send(
                    NotificationType.DELETE_GAME,
                    receiver,
                    notice.getContent()
            );
        }
    }
}
