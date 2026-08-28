package com.example.basketballmatching.user.service;

import com.example.basketballmatching.auth.service.AuthTokenStore;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.dto.UserWithdrawalGameResultDto;
import com.example.basketballmatching.game.service.UserWithdrawalGameService;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@IntegrationTest
@DisplayName("UserWithdrawalService 통합 테스트")
class UserWithdrawalServiceIntegrationTest {

    private static final String WITHDRAWAL_EMAIL =
            "withdrawal@test.com";

    private static final String RECEIVER_EMAIL =
            "receiver@test.com";

    private static final String REFRESH_TOKEN_PREFIX =
            "refreshToken:";

    private static final String LOGOUT_ACCESS_PREFIX =
            "logout:access:";

    private final List<Long> createdUserIds =
            new ArrayList<>();

    private final Set<String> createdRedisKeys =
            new HashSet<>();

    @Autowired
    private UserWithdrawalService userWithdrawalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AuthTokenStore authTokenStore;

    /*
     * 경기 정리 로직은 gameCreator 도메인의 책임이다.
     *
     * 이 테스트에서는 경기 정리 결과를 받아
     * UserWithdrawalService가 탈퇴와 이벤트 처리를
     * 올바르게 연결하는지만 검증한다.
     */
    @MockBean
    private UserWithdrawalGameService userWithdrawalGameService;

    @AfterEach
    void cleanUp() {
        createdRedisKeys.forEach(
                redisService::deleteData
        );

        notificationRepository.deleteAllInBatch();

        if (!createdUserIds.isEmpty()) {
            userRepository.deleteAllByIdInBatch(
                    createdUserIds
            );
        }

        createdRedisKeys.clear();
        createdUserIds.clear();
    }

    @Test
    @DisplayName("회원 탈퇴 커밋 이후 토큰 제거하고 경기 취소 알림 저장")
    void withdraw_success_afterCommit() {
        // given

        UserEntity withdrawalUser = saveUser(
                WITHDRAWAL_EMAIL,
                "탈퇴회원",
                "탈퇴회원"
        );

        UserEntity receiver = saveUser(
                RECEIVER_EMAIL,
                "알림수신회원",
                "알림수신회원"
        );

        Long withdrawalUserUserId = withdrawalUser.getUserId();

        Long receiverId = receiver.getUserId();

        GameCancelNotificationDto notice = new GameCancelNotificationDto(
                receiverId, "토요일 농구 경기"
        );

        when(userWithdrawalGameService.cleanup(eq(withdrawalUserUserId), any(LocalDateTime.class)))
                .thenReturn(new UserWithdrawalGameResultDto(List.of(notice)));

        String accessToken = tokenProvider.createAccessToken(WITHDRAWAL_EMAIL, withdrawalUser.getName(), UserType.USER);

        String refreshToken = tokenProvider.createRefreshToken(WITHDRAWAL_EMAIL);

        authTokenStore.saveRefreshToken(
                WITHDRAWAL_EMAIL,
                refreshToken,
                tokenProvider.getRefreshTokenExpirationMillis()
        );
        String refreshTokenKey =
                REFRESH_TOKEN_PREFIX + WITHDRAWAL_EMAIL;

        String logoutAccessKey =
                LOGOUT_ACCESS_PREFIX + accessToken;

        createdRedisKeys.add(refreshTokenKey);
        createdRedisKeys.add(logoutAccessKey);

        assertEquals(
                refreshToken,
                redisService.getData(refreshTokenKey)
        );
        // when

        userWithdrawalService.withdraw(withdrawalUserUserId, accessToken);

        /*
         * UserWithdrawalService의 트랜잭션이 종료되면서
         * AFTER_COMMIT 이벤트 리스너도 동기적으로 실행된다.
         */

        // then

        UserEntity withdrawnUser = userRepository.findById(withdrawalUserUserId)
                .orElseThrow();

        assertNotNull(withdrawnUser.getDeletedDateTime());

        assertTrue(userRepository.findByUserIdAndDeletedDateTimeIsNull(withdrawalUserUserId).isEmpty());

        /*
         * Refresh Token 삭제 확인
         */
        assertNull(
                redisService.getData(refreshTokenKey)
        );

        /*
         * Access Token 블랙리스트 등록 확인
         */
        assertEquals(
                "LOGOUT",
                redisService.getData(logoutAccessKey)
        );

        Long expiration = redisService.getExpiration(logoutAccessKey);

        assertNotNull(expiration);
        assertTrue(expiration > 0);

        NotificationResult notification = transactionTemplate.execute(
                status -> {
                    List<NotificationEntity> notifications = notificationRepository.findAll();

                    assertEquals(1, notifications.size());

                    NotificationEntity entity = notifications.get(0);

                    return new NotificationResult(
                            entity.getReceiver().getUserId(),
                            entity.getNotificationType(),
                            entity.getContent()
                    );
                }
        );

        assertNotNull(notification);

        assertEquals(
                receiverId,
                notification.receiverId()
        );

        assertEquals(
                NotificationType.DELETE_GAME,
                notification.notificationType()
        );

        assertEquals(
                "토요일 농구 경기의 게임이 취소되었습니다.",
                notification.content()
        );

    }

    private UserEntity saveUser(
            String email,
            String nickname,
            String name
    ) {
        UserEntity user =
                UserEntity.create(
                        email,
                        "encoded-password",
                        nickname,
                        name,
                        LocalDate.of(
                                1997,
                                1,
                                1
                        ),
                        "010-1111-2222",
                        "서울특별시 강남구",
                        Position.GUARD,
                        GenderType.MALE
                );

        user.setEmailAuth();

        UserEntity savedUser =
                userRepository.saveAndFlush(user);

        createdUserIds.add(
                savedUser.getUserId()
        );

        return savedUser;
    }

    private record NotificationResult(
            Long receiverId,
            NotificationType notificationType,
            String content
    ) {
    }
}
