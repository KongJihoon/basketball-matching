package com.example.basketballmatching.notifications.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.dto.response.NotificationResponse;
import com.example.basketballmatching.notifications.dto.response.ReadNotificationResponse;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.example.basketballmatching.notifications.type.NotificationType.DELETE_GAME;
import static com.example.basketballmatching.notifications.type.NotificationType.UPDATE_GAME;
import static org.junit.jupiter.api.Assertions.*;
@IntegrationTest
@Transactional
@DisplayName("NotificationService 통합 테스트")
class NotificationServiceIntegrationTest {
    private static final String UPDATE_CONTENT =
            "'주말 농구 경기' 경기 정보가 수정되었습니다.";

    private static final String DELETE_CONTENT =
            "'주말 농구 경기' 경기가 삭제되었습니다.";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private Long receiverId;
    private Long otherUserId;


    @BeforeEach
    void setUp() {
        UserEntity receiver = saveUser(
                "notification-receiver@test.com",
                "알림수신자",
                "010-1111-1111"
        );

        UserEntity otherUser = saveUser(
                "notification-other@test.com",
                "다른사용자",
                "010-2222-2222"
        );

        receiverId = receiver.getUserId();
        otherUserId = otherUser.getUserId();

        flushAndClear();
    }

    @Test
    @DisplayName("알림을 전송하면 알림이 저장되고 읽지 않은 목록에서 조회된다.")
    void sendAndGetUnreadNotifications_success() {
        // given

        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow();

        // when

        notificationService.send(UPDATE_GAME, receiver, UPDATE_CONTENT);

        flushAndClear();

        List<NotificationResponse> response = notificationService.getUnReadNotifications(receiverId, PageRequest.of(0, 10));

        // then

        assertEquals(1, notificationRepository.count());
        assertEquals(1, response.size());

        NotificationResponse notification = response.get(0);

        assertNotNull(notification.notificationId());

        assertEquals(UPDATE_GAME, notification.notificationType());

        assertEquals(UPDATE_CONTENT, notification.content());

        assertNotNull(notification.createdAt());

        NotificationEntity savedNotification = notificationRepository.findById(notification.notificationId())
                .orElseThrow();

        assertEquals(receiverId, savedNotification.getReceiver().getUserId());

        assertFalse(savedNotification.isRead());
        assertNull(savedNotification.getReadDateTime());


    }

    @Test
    @DisplayName("알림을 읽으면 읽음 시각이 저장되고 읽지 않은 목록에서 제외된다.")
    void readNotification_success() {
        // given

        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow();

        NotificationEntity notification = NotificationEntity.create(receiver, DELETE_GAME, DELETE_CONTENT);

        notificationRepository.save(notification);

        Long notificationId = notification.getNotificationId();

        flushAndClear();

        LocalDateTime beforeRead = LocalDateTime.now(clock)
                .truncatedTo(ChronoUnit.SECONDS);

        // when

        ReadNotificationResponse response = notificationService.readNotification(receiverId, notificationId);

        flushAndClear();
        // then

        NotificationEntity readNotification = notificationRepository.findById(notificationId)
                .orElseThrow();

        assertEquals(notificationId, response.notificationId());
        assertTrue(response.isRead());

        assertNotNull(response.readDateTime());

        assertTrue(readNotification.isRead());

        assertEquals(
                response.readDateTime().truncatedTo(ChronoUnit.SECONDS),
                readNotification.getReadDateTime().truncatedTo(ChronoUnit.SECONDS)
        );

        assertFalse(
                readNotification.getReadDateTime()
                        .truncatedTo(ChronoUnit.SECONDS)
                        .isBefore(beforeRead)
        );

        List<NotificationResponse> unReadNotifications = notificationService.getUnReadNotifications(receiverId, PageRequest.of(0, 10));

        assertTrue(unReadNotifications.isEmpty());
    }

    @Test
    @DisplayName("다른 사용자의 알림은 읽음 처리할 수 없다.")
    void readNotification_fail_otherUserNotification() {
        // given

        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow();

        NotificationEntity notification =
                NotificationEntity.create(
                        receiver,
                        UPDATE_GAME,
                        UPDATE_CONTENT
                );

        notificationRepository.save(notification);

        Long notificationId = notification.getNotificationId();

        flushAndClear();

        // when

        CustomException exception = assertThrows(CustomException.class, () -> notificationService.readNotification(otherUserId, notificationId));

        // then

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());

        NotificationEntity unchangedNotification = notificationRepository.findById(notificationId)
                .orElseThrow();

        assertFalse(unchangedNotification.isRead());

        assertNull(unchangedNotification.getReadDateTime());

    }

    @Test
    @DisplayName("읽지 않은 알림은 최신 생성 순서로 조회된다.")
    void getUnreadNotification_success_latestOrder() {
        // given

        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow();

        notificationService.send(UPDATE_GAME, receiver, UPDATE_CONTENT);

        notificationService.send(DELETE_GAME, receiver, DELETE_CONTENT);

        flushAndClear();

        // when

        List<NotificationResponse> response = notificationService.getUnReadNotifications(receiverId, PageRequest.of(0, 10));

        // then
        assertEquals(2, response.size());

        assertEquals(DELETE_GAME, response.get(0).notificationType());

        assertEquals(UPDATE_GAME, response.get(1).notificationType());

        assertTrue(response.get(0).notificationId() > response.get(1).notificationId());
    }




    private UserEntity saveUser(
            String email,
            String nickname,
            String phone
    ) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name(nickname)
                .birth(LocalDate.of(1997, 1, 1))
                .phone(phone)
                .address("서울특별시 송파구")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();

        return userRepository.save(user);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
