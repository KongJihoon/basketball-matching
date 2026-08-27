package com.example.basketballmatching.report.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.dto.request.CreateReportRequest;
import com.example.basketballmatching.report.dto.request.ReviewReportRequest;
import com.example.basketballmatching.report.dto.response.CreateReportResponse;
import com.example.basketballmatching.report.dto.response.ReportListResponse;
import com.example.basketballmatching.report.dto.response.ReviewReportResponse;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.type.ReportDecision;
import com.example.basketballmatching.report.type.ReportType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.global.exception.ErrorCode.ALREADY_REPORTED_USER;
import static com.example.basketballmatching.report.type.ReportStatus.APPROVED;
import static com.example.basketballmatching.report.type.ReportStatus.PENDING;
import static org.junit.jupiter.api.Assertions.*;
@Transactional
@IntegrationTest
@DisplayName("ReportService 통합 테스트")
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ParticipantGameRepository participantGameRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private Long gameId;
    private Long reporterId;
    private Long secondReporterId;
    private Long targetId;
    private Long adminId;

    @BeforeEach
    void setUp() {

        UserEntity creator = saveUser(
                "creator@test.com",
                "경기생성자",
                "010-1111-1111",
                UserType.USER
        );

        UserEntity reporter = saveUser(
                "reporter@test.com",
                "신고자",
                "010-2222-2222",
                UserType.USER
        );

        UserEntity secondReporter = saveUser(
                "second-reporter@test.com",
                "두번째신고자",
                "010-3333-3333",
                UserType.USER
        );

        UserEntity target = saveUser(
                "target@test.com",
                "신고대상",
                "010-4444-4444",
                UserType.USER
        );

        UserEntity admin = saveUser(
                "admin@test.com",
                "관리자",
                "010-5555-5555",
                UserType.ADMIN
        );

        LocalDateTime now = LocalDateTime.now(clock)
                .withSecond(0)
                .withNano(0);

        LocalDateTime startDateTime = now.minusDays(2);
        LocalDateTime endDateTime = startDateTime.plusHours(2);
        LocalDateTime createdAt = startDateTime.minusDays(1);

        GameEntity game = GameEntity.create(
                "신고 통합 테스트 경기",
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
                createdAt
        );

        gameRepository.save(game);

        participantGameRepository.save(
                ParticipantGameEntity.createCreator(
                        game,
                        creator,
                        createdAt
                )
        );

        participantGameRepository.save(
                ParticipantGameEntity.createParticipation(
                        game,
                        reporter,
                        createdAt
                )
        );

        participantGameRepository.save(
                ParticipantGameEntity.createParticipation(
                        game,
                        secondReporter,
                        createdAt
                )
        );

        participantGameRepository.save(
                ParticipantGameEntity.createParticipation(
                        game,
                        target,
                        createdAt
                )
        );

        gameId = game.getGameId();
        reporterId = reporter.getUserId();
        secondReporterId = secondReporter.getUserId();
        targetId = target.getUserId();
        adminId = admin.getUserId();

        flushAndClear();
    }

    @Test
    @DisplayName("종료된 경기의 확정 참가자가 신고 시 신고 정보가 저장된다.")
    void createReport_success() {
        // given

        CreateReportRequest request = createReportRequest();

        // when

        CreateReportResponse response = reportService.createReport(reporterId, gameId, request);

        flushAndClear();
        // then

        ReportEntity report = reportRepository.findById(response.reportId()).orElseThrow();


        assertEquals(reporterId, report.getReportUser().getUserId());
        assertEquals(targetId, report.getTargetUser().getUserId());
        assertEquals(gameId, report.getGameEntity().getGameId());
        assertEquals("경기 중 비매너 행위를 반복했습니다.", report.getContent());

        assertEquals(PENDING, report.getReportStatus());

        assertEquals(
                response.reportedAt().truncatedTo(ChronoUnit.SECONDS),
                report.getReportedDateTime().truncatedTo(ChronoUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("동일한 경기에서 같은 사용자를 중복 신고할 수 없다.")
    void createReport_fail_duplicateReport() {
        // given

        CreateReportRequest request = createReportRequest();

        reportService.createReport(reporterId, gameId, request);

        flushAndClear();

        // when

        CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(reporterId, gameId, request));

        // then

        assertEquals(ALREADY_REPORTED_USER, exception.getErrorCode());
        assertEquals(1, reportRepository.count());

    }

    @Test
    @DisplayName("서로 다른 참가자는 같은 대상을 신고할 수 있다.")
    void createReport_success_differentReporters() {
        // given

        CreateReportRequest request = createReportRequest();

        // when
        reportService.createReport(reporterId, gameId, request);

        reportService.createReport(secondReporterId, gameId, request);

        flushAndClear();
        // then

        assertEquals(2, reportRepository.count());


    }

    @Test
    @DisplayName("관리자는 처리 상태에 따라 신고 목록을 조회할 수 있다.")
    void getReports_success_filterByStatus() {
        // given

        CreateReportResponse pendingReport = reportService.createReport(
                secondReporterId, gameId, createReportRequest()
        );

        CreateReportResponse approvedReport = reportService.createReport(reporterId, gameId, createReportRequest());

        reportService.reviewReport(adminId, approvedReport.reportId(), new ReviewReportRequest(
                ReportDecision.APPROVE, "신고 내용을 확인하여 승인합니다."
        ));

        flushAndClear();

        // when

        Page<ReportListResponse> response = reportService.getReports(adminId, PENDING, PageRequest.of(0, 20));

        // then

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());

        ReportListResponse report = response.getContent().get(0);

        assertEquals(pendingReport.reportId(), report.reportId());
        assertEquals(gameId, report.gameId());
        assertEquals(secondReporterId, report.reporterId());
        assertEquals(targetId, report.targetUserId());
        assertEquals(ReportType.POOR_SPORTSMANSHIP, report.reportType());

    }

    @Test
    @DisplayName("신고 승인 시 검토 정보가 저장")
    void reviewReport_success_approve() {
        // given

        CreateReportResponse created = reportService.createReport(reporterId, gameId, createReportRequest());

        flushAndClear();

        ReviewReportRequest request = new ReviewReportRequest(ReportDecision.APPROVE, "신고 내용을 확인하여 승인합니다.");
        // when

        ReviewReportResponse response = reportService.reviewReport(adminId, created.reportId(), request);

        // then
        ReportEntity report = reportRepository.findById(created.reportId()).orElseThrow();

        assertEquals(APPROVED, report.getReportStatus());
        assertEquals(APPROVED, response.reportStatus());

        assertNotNull(response.reviewedAt());

        assertEquals("신고 내용을 확인하여 승인합니다.", report.getReviewReason());


    }




    private CreateReportRequest createReportRequest() {
        return new CreateReportRequest(
                targetId, ReportType.POOR_SPORTSMANSHIP, "경기 중 비매너 행위를 반복했습니다."
        );
    }

    private UserEntity saveUser(String email, String nickname, String phone, UserType userType) {
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

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
