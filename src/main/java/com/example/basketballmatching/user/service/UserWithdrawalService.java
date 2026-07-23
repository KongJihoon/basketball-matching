package com.example.basketballmatching.user.service;

import com.example.basketballmatching.gameCreator.dto.UserWithdrawalGameResultDto;
import com.example.basketballmatching.gameCreator.service.UserWithdrawalGameService;
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

    @Transactional
    public void withdraw(Long userId, String accessToken) {

        validateAccessToken(accessToken);

        UserEntity user = findActiveUser(userId);

        LocalDateTime withdrawnAt = LocalDateTime.now(clock);

        UserWithdrawalGameResultDto gameResult = userWithdrawalGameService.cleanup(userId, withdrawnAt);

        user.withdraw(withdrawnAt);

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
