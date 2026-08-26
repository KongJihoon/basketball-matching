package com.example.basketballmatching.report.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.dto.request.CreateReportRequest;
import com.example.basketballmatching.report.dto.request.ReviewReportRequest;
import com.example.basketballmatching.report.dto.response.CreateReportResponse;
import com.example.basketballmatching.report.dto.response.ReviewReportResponse;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.type.ReportDecision;
import com.example.basketballmatching.report.type.ReportStatus;
import com.example.basketballmatching.report.type.ReportType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static com.example.basketballmatching.report.type.ReportStatus.REJECTED;
import static com.example.basketballmatching.report.type.ReportType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService 단위 테스트")
class ReportServiceUnitTest {

    private static final Long GAME_ID = 1L;
    private static final Long REPORT_ID = 10L;

    private static final Long REPORTER_ID = 1L;
    private static final Long TARGET_ID = 2L;
    private static final Long ADMIN_ID = 3L;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 27, 12, 0);

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    private ReportService reportService;

    private UserEntity reporter;
    private UserEntity target;
    private UserEntity admin;
    private GameEntity endedGame;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE_ID).toInstant(), ZONE_ID);

        reportService = new ReportService(
                gameRepository, participantGameRepository, userRepository, reportRepository, clock
        );

        reporter = createUser(REPORTER_ID, "reporter@test.com", "신고자", UserType.USER);

        target = createUser(TARGET_ID, "target@test.com", "신고 대상", UserType.USER);

        admin = createUser(ADMIN_ID, "admin@test.com", "관리자", UserType.ADMIN);

        endedGame = createEndedGame(NOW.minusDays(1));
    }


    @Nested
    @DisplayName("신고 접수")
    class CreateReport {

        @Test
        @DisplayName("종료된 경기의 확정 참가자는 다른 확정 참가자 신고할 수 있다.")
        void createReport_success() {
            // given

            CreateReportRequest request = createReportRequest(TARGET_ID);

            stubValidReportCondition();

            when(reportRepository.save(any(ReportEntity.class)))
                    .thenAnswer(invocation -> {
                        ReportEntity report = invocation.getArgument(0);

                        ReflectionTestUtils.setField(report, "reportId", REPORT_ID);
                        return report;
                    });
            // when

            CreateReportResponse response = reportService.createReport(REPORTER_ID, GAME_ID, request);


            // then

            assertEquals(REPORT_ID, response.reportId());
            assertEquals(NOW, response.reportedAt());
            verify(reportRepository).save(any(ReportEntity.class));
        }

        @Test
        @DisplayName("자기 자신은 신고 불가")
        void createReport_fail_selfReport() {
            // given

            CreateReportRequest request = createReportRequest(REPORTER_ID);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, GAME_ID, request));

            // then

            assertEquals(SELF_REPORT_NOT_ALLOWED, exception.getErrorCode());


            verifyNoInteractions(
                    gameRepository, participantGameRepository, userRepository, reportRepository
            );

        }

        @Test
        @DisplayName("종료되지 않은 경기는 신고할 수 없다.")
        void createReport_fail_gameNotEnded() {
            // given

            GameEntity futureGame = createFutureGame();

            when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                    .thenReturn(Optional.of(futureGame));

            CreateReportRequest request = createReportRequest(TARGET_ID);
            // when

            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, GAME_ID, request));

            // then

            assertEquals(NOT_GAME_ENDED, exception.getErrorCode());
            verifyNoInteractions(participantGameRepository, userRepository, reportRepository);

        }

        @Test
        @DisplayName("경기 종료 후 7일이 지나면 신고할 수 없다.")
        void createReport_fail_reportPeriodExpired() {
            // given

            GameEntity expiredGame = createEndedGame(NOW.minusDays(8));

            when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                    .thenReturn(Optional.of(expiredGame));

            CreateReportRequest request = createReportRequest(TARGET_ID);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, GAME_ID, request));

            // then

            assertEquals(REPORT_PERIOD_EXPIRED, exception.getErrorCode());

            verifyNoInteractions(participantGameRepository, userRepository, reportRepository);

        }

        @Test
        @DisplayName("확정 참가자가 아닌 사용자는 신고할 수 없다.")
        void createReport_fail_reporterNotAccepted() {
            // given

            when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                    .thenReturn(Optional.of(endedGame));

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(REPORTER_ID))
                    .thenReturn(Optional.of(reporter));

            when(participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                    GAME_ID, REPORTER_ID, ACCEPT
            )).thenReturn(false);

            CreateReportRequest request = createReportRequest(TARGET_ID);
            // when

            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, GAME_ID, request));


            // then

            assertEquals(REPORTER_NOT_ACCEPTED_PARTICIPANT, exception.getErrorCode());

            verify(reportRepository, never()).save(any(ReportEntity.class));
        }


        @Test
        @DisplayName("신고 대상이 확정 참가자가 아니면 신고할 수 없다.")
        void createReport_fail_targetNotAccepted() {
            // given

            when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                    .thenReturn(Optional.of(endedGame));

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(REPORTER_ID))
                    .thenReturn(Optional.of(reporter));

            when(participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                    GAME_ID, REPORTER_ID, ACCEPT
            )).thenReturn(true);

            when(participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                    GAME_ID, TARGET_ID, ACCEPT
            )).thenReturn(false);

            CreateReportRequest request = createReportRequest(TARGET_ID);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, GAME_ID, request));

            // then

            assertEquals(REPORT_TARGET_NOT_ACCEPTED_PARTICIPANT, exception.getErrorCode());

            verify(reportRepository, never()).save(any(ReportEntity.class));

        }

        @Test
        @DisplayName("같은 경기에서 같은 사용자 중복 신고 불가")
        void createReport_fail_duplicateReport() {
            // given

            stubValidReportCondition();

            when(reportRepository.existsByReportUser_UserIdAndTargetUser_UserIdAndGameEntity_GameId(
                    REPORTER_ID, TARGET_ID, GAME_ID
            )).thenReturn(true);

            CreateReportRequest request = createReportRequest(TARGET_ID);
            // when

            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, GAME_ID, request));

            // then

            assertEquals(ALREADY_REPORTED_USER, exception.getErrorCode());

            verify(reportRepository, never()).save(any(ReportEntity.class));

        }
    }

    @Nested
    @DisplayName("신고 검토")
    class ReviewReport {

        @Test
        @DisplayName("관리자는 신고를 승인할 수 있다.")
        void reviewReport_success() {
            // given

            ReportEntity report = createPendingReport();

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(reportRepository.findById(REPORT_ID))
                    .thenReturn(Optional.of(report));

            ReviewReportRequest request = new ReviewReportRequest(ReportDecision.APPROVE, "신고 내용을 확인하여 승인합니다.");

            // when

            ReviewReportResponse response = reportService.reviewReport(ADMIN_ID, REPORT_ID, request);

            // then

            assertEquals(REPORT_ID, response.reportId());
            assertEquals(ReportStatus.APPROVED, response.reportStatus());
            assertEquals(NOW, response.reviewedAt());

            assertEquals(ReportStatus.APPROVED, report.getReportStatus());
            assertEquals(admin, report.getReviewedBy());

            assertEquals("신고 내용을 확인하여 승인합니다.", report.getReviewReason());



        }

        @Test
        @DisplayName("관리자는 신고를 거절할 수 있다")
        void reviewReport_success_reject() {
            // given

            ReportEntity report = createPendingReport();

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(reportRepository.findById(REPORT_ID))
                    .thenReturn(Optional.of(report));

            ReviewReportRequest request = new ReviewReportRequest(ReportDecision.REJECT, "신고 사유가 충분하지 않습니다.");

            // when

            ReviewReportResponse response = reportService.reviewReport(ADMIN_ID, REPORT_ID, request);

            // then
            assertEquals(REPORT_ID, response.reportId());
            assertEquals(REJECTED, response.reportStatus());
            assertEquals(NOW, response.reviewedAt());

            assertEquals(REJECTED, report.getReportStatus());
            assertEquals(admin, report.getReviewedBy());
            assertEquals("신고 사유가 충분하지 않습니다.", report.getReviewReason());
        }
    }

    private void stubValidReportCondition() {
        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(GAME_ID))
                .thenReturn(Optional.of(endedGame));

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(REPORTER_ID))
                .thenReturn(Optional.of(reporter));

        when(participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                GAME_ID, REPORTER_ID, ACCEPT
        )).thenReturn(true);

        when(participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                GAME_ID, TARGET_ID, ACCEPT
        )).thenReturn(true);

        when(userRepository.findById(TARGET_ID))
                .thenReturn(Optional.of(target));

        when(reportRepository.existsByReportUser_UserIdAndTargetUser_UserIdAndGameEntity_GameId(
                REPORTER_ID, TARGET_ID, GAME_ID
        )).thenReturn(false);
    }

    private CreateReportRequest createReportRequest(Long targetUserId) {
        return new CreateReportRequest(
                targetUserId, POOR_SPORTSMANSHIP, "경기 중 반복적으로 비매너 행위"
        );
    }

    private ReportEntity createPendingReport() {

        ReportEntity report = ReportEntity.create(
                reporter, target, endedGame, POOR_SPORTSMANSHIP, "경기 중 반복적으로 비매너 행위", NOW.minusHours(1)
        );

        ReflectionTestUtils.setField(report, "reportId", REPORT_ID);

        return report;
    }

    private GameEntity createEndedGame(LocalDateTime endDateTime) {
        LocalDateTime startDateTime = endDateTime.minusHours(2);

        LocalDateTime creationTime = startDateTime.minusDays(2);

        return createGame(startDateTime, endDateTime, creationTime);
    }

    private GameEntity createFutureGame() {
        LocalDateTime startDateTime = NOW.plusDays(2);

        return createGame(startDateTime, startDateTime.plusHours(2), NOW);
    }

    private GameEntity createGame(LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime creationTime) {

        GameEntity game = GameEntity.create(
                "신고 테스트 경기",
                "신고 정책 검증을 위한 경기입니다.",
                6,
                INDOOR,
                THREE_ON_THREE,
                MIXED,
                startDateTime,
                endDateTime,
                "테스트 농구장",
                "서울특별시 송파구",
                SEOUL,
                37.5,
                127.0,
                reporter,
                creationTime
        );

        ReflectionTestUtils.setField(game, "gameId", GAME_ID);

        return game;
    }

    private UserEntity createUser(Long userId, String email, String nickname, UserType userType) {

        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name(nickname)
                .birth(LocalDate.of(1997, 1, 1))
                .phone("010-1234-5678")
                .address("서울특별시")
                .position(Position.GUARD)
                .userType(userType)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }
}