package com.example.basketballmatching.blacklist.event;

import com.example.basketballmatching.auth.service.AuthTokenStore;
import com.example.basketballmatching.blacklist.dto.response.CreateBlackListResponse;
import com.example.basketballmatching.blacklist.repository.BlackListRepository;
import com.example.basketballmatching.blacklist.service.BlackListService;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.service.BlackListGameService;
import com.example.basketballmatching.notifications.repository.NotificationRepository;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.type.ReportType;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@IntegrationTest
@DisplayName("블랙리스트 이벤트 리스너 통합 테스트")
public class UserBlacklistedEventListenerIntegrationTest {

    private static final String TARGET_EMAIL = "blacklist-target@test.com";

    @Autowired
    private BlackListService blackListService;

    @Autowired
    private BlackListRepository blackListRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuthTokenStore authTokenStore;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Clock clock;

    /*
     * 경기 상태 정리 자체는 BlackListGameService 테스트의 책임이다.
     * 여기서는 정리 결과가 이벤트 알림으로 연결되는지만 검증한다.
     */
    @MockBean
    private BlackListGameService blackListGameService;

    @AfterEach
    void cleanUp() {
        authTokenStore.deleteRefreshToken(TARGET_EMAIL);

        notificationRepository.deleteAllInBatch();
        blackListRepository.deleteAllInBatch();
        reportRepository.deleteAllInBatch();
        gameRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("블랙리스트 등록 커밋 후 RefreshToken 폐기 후 제재 및경기 취소 알림 저장")
    void createBlackList_success_afterCommit() {
        // given

        TestFixture fixture = createFixture();

        GameCancelNotificationDto gameNotice = new GameCancelNotificationDto(fixture.receiverId, "주말 농구 경기");

        when(blackListGameService.cleanup(eq(fixture.targetId), any(LocalDateTime.class)))
                .thenReturn(new BlackListGameResultDto(List.of(gameNotice)));

        String refreshToken = "test-refresh-token";

        authTokenStore.saveRefreshToken(TARGET_EMAIL, refreshToken, 60_000L);

        assertEquals(refreshToken, authTokenStore.getRefreshToken(TARGET_EMAIL));

        // when

        CreateBlackListResponse response = blackListService.createBlackList(fixture.adminId, fixture.reportId);

        // then

        /**
         * createBlackList 트랜잭션이 커밋되면서
         * AFTER_COMMIT 리스너도 동기적으로 실행된다.
         */

        assertTrue(blackListRepository.findById(response.blackListId()).isPresent());

        /**
         * 제재 대상의 Refresh Token 삭제 확인
         */

        assertNull(authTokenStore.getRefreshToken(TARGET_EMAIL));

        List<NotificationResult> notificaitons = transactionTemplate.execute(
                status -> notificationRepository.findAll()
                        .stream()
                        .map(notification -> new NotificationResult(
                                notification.getReceiver().getUserId(), notification.getNotificationType(), notification.getContent()
                        )).toList()
        );

        assertNotNull(notificaitons);

        assertEquals(2, notificaitons.size());

        NotificationResult restrictNotification = notificaitons.stream()
                .filter(notification -> notification.notificationType == NotificationType.BLACKLISTED)
                .findFirst()
                .orElseThrow();

        assertEquals(fixture.targetId, restrictNotification.receiverId);

        assertEquals("신고 검토 결과 " + response.expiresAt() + "까지 서비스 이용이 제한되었습니다.", restrictNotification.content);

        NotificationResult gameCancelNotification = notificaitons.stream()
                .filter(notification -> notification.notificationType == NotificationType.DELETE_GAME)
                .findFirst()
                .orElseThrow();

        assertEquals(fixture.receiverId, gameCancelNotification.receiverId);

        assertEquals("주말 농구 경기의 게임이 취소되었습니다.", gameCancelNotification.content);

    }



    private TestFixture createFixture() {
        return transactionTemplate.execute(status -> {
            UserEntity creator = saveUser(
                    "event-creator@test.com",
                    "경기생성자",
                    "010-1111-1111",
                    UserType.USER
            );

            UserEntity reporter = saveUser(
                    "event-reporter@test.com",
                    "신고자",
                    "010-2222-2222",
                    UserType.USER
            );

            UserEntity target = saveUser(
                    TARGET_EMAIL,
                    "신고대상",
                    "010-3333-3333",
                    UserType.USER
            );

            UserEntity receiver = saveUser(
                    "event-receiver@test.com",
                    "알림수신자",
                    "010-4444-4444",
                    UserType.USER
            );

            UserEntity admin = saveUser(
                    "event-admin@test.com",
                    "관리자",
                    "010-5555-5555",
                    UserType.ADMIN
            );

            LocalDateTime now = LocalDateTime.now(clock)
                    .withSecond(0)
                    .withNano(0);

            LocalDateTime startDateTime = now.minusDays(2);

            LocalDateTime endDateTime = startDateTime.plusHours(2);

            GameEntity game = GameEntity.create(
                    "블랙리스트 이벤트 테스트 경기",
                    "종료된 경기입니다.",
                    6,
                    INDOOR,
                    THREE_ON_THREE,
                    MIXED,
                    startDateTime,
                    endDateTime,
                    "잠실 농구장",
                    "서울특별시 송파구 올림픽로 25",
                    SEOUL,
                    37.515,
                    127.073,
                    creator,
                    startDateTime.minusDays(1)
            );

            gameRepository.save(game);

            ReportEntity report = ReportEntity.create(
                    reporter, target, game, ReportType.POOR_SPORTSMANSHIP, "경기 중 비매너 행위를 반복했습니다.", now.minusHours(2)
            );

            report.approve(admin, "신고 내용을 확인하여 승인합니다.", now.minusHours(1));

            reportRepository.save(report);

            return new TestFixture(
                    admin.getUserId(),
                    target.getUserId(),
                    receiver.getUserId(),
                    report.getReportId()
            );
        });
    }


    private UserEntity saveUser(
            String email,
            String nickname,
            String phone,
            UserType userType
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
                .userType(userType)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();

        return userRepository.save(user);
    }
    private record TestFixture(
            Long adminId,
            Long targetId,
            Long receiverId,
            Long reportId
    ) {

    }

    private record NotificationResult(
            Long receiverId,
            NotificationType notificationType,
            String content
    ) {}
}
