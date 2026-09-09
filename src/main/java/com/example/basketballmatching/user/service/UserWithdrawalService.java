package com.example.basketballmatching.user.service;

import com.example.basketballmatching.game.dto.UserWithdrawalGameResultDto;
import com.example.basketballmatching.game.service.UserWithdrawalGameService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.event.UserWithdrawnEvent;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.NOT_FOUND_TOKEN;
import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final UserWithdrawalGameService userWithdrawalGameService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 로그인한 사용자의 회원탈퇴 요청을 처리한다.
     *
     */
    @Transactional
    public void withdraw(Long userId, String accessToken) {

        /*
         * 요청된 Access 토큰의 형식을 검사한다.
         */
        validateAccessToken(accessToken);

        UserEntity user = findActiveUser(userId);

        // 테스트에서 탈퇴 시각을 고정할 수 있도록 주입받은 Clock을 사용한다.
        LocalDateTime withdrawnAt = LocalDateTime.now(clock);

        /*
         * 탈퇴 요청한 사용자가 참가한 예정 경기 및 현재 참가 경기를 정리한다.
         * 검증에 실패하면 사용자 탈퇴 처리까지 함께 트랜잭션 롤백처리.
         */
        UserWithdrawalGameResultDto gameResult = userWithdrawalGameService.cleanup(userId, withdrawnAt);

        // 실제 데이터를 삭제하지 않고 탈퇴 시각을 기록해 Soft Delete 처리
        user.withdraw(withdrawnAt);

        /*
         * 이벤트는 현재 트랜잭션 커밋 후 처리된다.
         * 토큰 폐기와 경기 취소 알림은 이벤트 리스너가 담당한다.
         */
        eventPublisher.publishEvent(
                new UserWithdrawnEvent(
                        user.getUserId(),
                        user.getEmail(),
                        accessToken,
                        gameResult.notices()
                )
        );

        log.info("[회원 탈퇴 DB 처리 완료] userId={}", user.getUserId());

    }

    private UserEntity findActiveUser(Long userId) {
        return userRepository
                .findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void validateAccessToken(
            String accessToken
    ) {
        if (accessToken == null ||
                accessToken.isBlank()) {
            throw new CustomException(
                    NOT_FOUND_TOKEN
            );
        }
    }
}
