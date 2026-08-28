package com.example.basketballmatching.notifications.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.notifications.domain.NotificationEntity;
import com.example.basketballmatching.notifications.dto.response.NotificationResponse;
import com.example.basketballmatching.notifications.dto.response.ReadNotificationResponse;
import com.example.basketballmatching.notifications.repository.EmitterRepository;
import com.example.basketballmatching.notifications.repository.NotificationQueryRepository;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.notifications.support.SseIdGenerator;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.NOTIFICATION_NOT_FOUND;
import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;
import static com.example.basketballmatching.notifications.type.NotificationType.UPDATE_GAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 단위 테스트")
class NotificationServiceUnitTest {


    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 10L;

    private static final String EVENT_ID =
            "1_event_1787904000000_00000000000000000001";

    private static final String CONTENT =
            "'주말 농구 경기' 경기 정보가 수정되었습니다.";

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 28, 21, 0);

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Seoul");

    @Mock
    private EmitterRepository emitterRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationQueryRepository notificationQueryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseIdGenerator sseIdGenerator;

    private NotificationService notificationService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE_ID).toInstant(), ZONE_ID);

        notificationService = new NotificationService(
                emitterRepository, notificationRepository, notificationQueryRepository, userRepository, sseIdGenerator, clock
        );

        user = createUser();
    }

    @Nested
    @DisplayName("읽지 않은 알림 조회")
    class GetUnreadNotifications {
        @Test
        @DisplayName("사용자의 읽지 않은 알림을 조회한다.")
        void getUnreadNotifications_success() {
            // given

            PageRequest pageRequest = PageRequest.of(0, 20);

            NotificationResponse notification = new NotificationResponse(
                    NOTIFICATION_ID, UPDATE_GAME, CONTENT, NOW.minusMinutes(10)
            );

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            when(notificationQueryRepository.findUnreadNotifications(USER_ID, pageRequest))
                    .thenReturn(List.of(notification));


            // when

            List<NotificationResponse> response = notificationService.getUnReadNotifications(USER_ID, pageRequest);

            // then

            assertEquals(1, response.size());

            NotificationResponse result = response.get(0);

            assertEquals(NOTIFICATION_ID, result.notificationId());
            assertEquals(UPDATE_GAME, result.notificationType());
            assertEquals(CONTENT, result.content());

            verify(notificationQueryRepository).findUnreadNotifications(USER_ID, pageRequest);

            verifyNoInteractions(notificationRepository);

        }
        @Test
        @DisplayName("사용자를 찾을 수 없으면 알림 조회 불가")
        void getUnreadNotifications_fail_userNotFound() {
            // given

            PageRequest pageRequest = PageRequest.of(0, 20);

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.empty());

            // when

            CustomException exception = assertThrows(CustomException.class, () -> notificationService.getUnReadNotifications(USER_ID, pageRequest));

            // then

            assertEquals(USER_NOT_FOUND, exception.getErrorCode());

            verifyNoInteractions(notificationRepository, notificationQueryRepository);

        }
    }

    @Nested
    @DisplayName("알림 읽음 처리")
    class ReadNotification {
        @Test
        @DisplayName("사용자가 자신의 알림을 읽음 처리한다.")
        void readNotification_success() {
            // given

            NotificationEntity notification = createNotification();

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            when(notificationRepository.findByNotificationIdAndReceiver_UserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(Optional.of(notification));

            // when

            ReadNotificationResponse response = notificationService.readNotification(USER_ID, NOTIFICATION_ID);

            // then

            assertEquals(NOTIFICATION_ID, response.notificationId());
            assertTrue(response.isRead());
            assertEquals(NOW, response.readDateTime());

            assertTrue(notification.isRead());
            assertEquals(NOW, notification.getReadDateTime());

        }

        @Test
        @DisplayName("자신의 알림이 아니면 읽을 수 없다.")
        void readNotification_fail_notificationNotFound() {
            // given
            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));
            when(notificationRepository.findByNotificationIdAndReceiver_UserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // when

            CustomException exception = assertThrows(CustomException.class, () -> notificationService.readNotification(USER_ID, NOTIFICATION_ID));

            // then

            assertEquals(NOTIFICATION_NOT_FOUND, exception.getErrorCode())
            ;

        }

        @Test
        @DisplayName("이미 읽은 알림은 최초 읽음 시간을 유지한다.")
        void readNotification_success_alreadyRead() {
            // given
            NotificationEntity notification =
                    createNotification();

            LocalDateTime firstReadDateTime =
                    NOW.minusMinutes(5);

            notification.markAsRead(firstReadDateTime);

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                    .thenReturn(Optional.of(user));

            when(notificationRepository.findByNotificationIdAndReceiver_UserId(
                    NOTIFICATION_ID, USER_ID
            )).thenReturn(Optional.of(notification));

            // when

            ReadNotificationResponse response = notificationService.readNotification(USER_ID, NOTIFICATION_ID);

            // then

            assertTrue(response.isRead());
            assertEquals(firstReadDateTime, response.readDateTime());

            assertEquals(firstReadDateTime, notification.getReadDateTime());

        }
    }

    @Nested
    @DisplayName("알림 전송")
    class SendNotification {
        @Test
        @DisplayName("알림을 저장하고 SSE 재연결용 이벤트를 캐시에 저장")
        void send_success() {
            // given

            when(notificationRepository.save(any(NotificationEntity.class)))
                    .thenAnswer(invocation -> {
                        NotificationEntity notification = invocation.getArgument(0);

                        ReflectionTestUtils.setField(notification, "notificationId", NOTIFICATION_ID);
                        return notification;
                    });

            when(sseIdGenerator.createEventId(USER_ID))
                    .thenReturn(EVENT_ID);

            when(emitterRepository.findAllEmittersByUserId(USER_ID))
                    .thenReturn(Map.of());

            // when

            notificationService.send(UPDATE_GAME, user, CONTENT);

            // then

            ArgumentCaptor<NotificationEntity> notificationCaptor = ArgumentCaptor.forClass(NotificationEntity.class);

            verify(notificationRepository).save(notificationCaptor.capture());

            NotificationEntity savedNotification = notificationCaptor.getValue();

            assertEquals(user, savedNotification.getReceiver());

            assertEquals(UPDATE_GAME, savedNotification.getNotificationType());

            assertEquals(CONTENT, savedNotification.getContent());

            assertFalse(savedNotification.isRead());
            assertNull(savedNotification.getReadDateTime());

            ArgumentCaptor<NotificationResponse> responseCaptor = ArgumentCaptor.forClass(NotificationResponse.class);

            verify(emitterRepository).saveEvent(eq(EVENT_ID), responseCaptor.capture());

            NotificationResponse cacheResponse = responseCaptor.getValue();

            assertEquals(NOTIFICATION_ID, cacheResponse.notificationId());

            assertEquals(UPDATE_GAME, cacheResponse.notificationType());
            assertEquals(CONTENT, cacheResponse.content());

            verify(emitterRepository).findAllEmittersByUserId(USER_ID);

        }
    }


    private UserEntity createUser() {
        UserEntity createdUser = UserEntity.builder()
                .email("notification@test.com")
                .password("encoded-password")
                .nickname("알림사용자")
                .name("알림사용자")
                .birth(LocalDate.of(1997, 1, 1))
                .phone("010-1111-1111")
                .address("서울특별시 송파구")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();

        ReflectionTestUtils.setField(
                createdUser,
                "userId",
                USER_ID
        );

        return createdUser;
    }

    private NotificationEntity createNotification() {
        NotificationEntity notification =
                NotificationEntity.create(
                        user,
                        UPDATE_GAME,
                        CONTENT
                );

        ReflectionTestUtils.setField(
                notification,
                "notificationId",
                NOTIFICATION_ID
        );

        return notification;
    }

}