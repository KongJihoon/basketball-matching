package com.example.basketballmatching.report.service;

import com.example.basketballmatching.game.domain.GameEntity;
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
import com.example.basketballmatching.report.type.ReportStatus;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.basketballmatching.game.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final long REPORT_AVAILABLE_DAYS = 7L;

    private final GameRepository gameRepository;
    private final ParticipantGameRepository participantGameRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final Clock clock;


    @Transactional
    public CreateReportResponse createReport(Long reporterId, Long gameId, CreateReportRequest request) {

        validateNotSelfReport(reporterId, request.targetUserId());

        GameEntity game = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        LocalDateTime reportedAt = LocalDateTime.now(clock);

        validateReportPeriod(game, reportedAt);

        UserEntity reporter = getActiveUser(reporterId);

        validateAcceptedReporter(gameId, reporterId);

        validateAcceptedTarget(gameId, request.targetUserId());

        UserEntity target = userRepository.findById(request.targetUserId())
                        .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        validateDuplicateReport(reporterId, request.targetUserId(), gameId);

        ReportEntity report = ReportEntity.create(
                reporter, target, game, request.reportType(), request.content(), reportedAt
        );

        ReportEntity savedReport = reportRepository.save(report);

        return CreateReportResponse.fromEntity(savedReport);
    }

    @Transactional(readOnly = true)
    public Page<ReportListResponse> getReports(Long adminId, ReportStatus status, Pageable pageable) {

        getActiveUser(adminId);

        return reportRepository.findAllByReportStatusOrderByReportedDateTimeDesc(status, pageable)
                .map(ReportListResponse::fromEntity);
    }

    @Transactional
    public ReviewReportResponse reviewReport(Long adminId, Long reportId, ReviewReportRequest request) {

        UserEntity reviewer = getActiveUser(adminId);

        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_REPORT));

        LocalDateTime reviewedAt = LocalDateTime.now(clock);

        switch (request.decision()) {
            case APPROVE -> report.approve(
                    reviewer, request.reason(), reviewedAt
            );

            case REJECT -> report.reject(
                    reviewer, request.reason(), reviewedAt
            );
        }

        return ReviewReportResponse.fromEntity(report);

    }



    private UserEntity getActiveUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void validateReportPeriod(GameEntity game, LocalDateTime reportedAt) {

        LocalDateTime gameEndedAt = game.getEndDateTime();

        if (reportedAt.isBefore(gameEndedAt)) {
            throw new CustomException(NOT_GAME_ENDED);
        }

        LocalDateTime reportDeadline = gameEndedAt.plusDays(REPORT_AVAILABLE_DAYS);

        if (reportedAt.isAfter(reportDeadline)) {
            throw new CustomException(REPORT_PERIOD_EXPIRED);
        }

    }

    private void validateNotSelfReport(Long reporterId, Long targetId) {

        if (Objects.equals(reporterId, targetId)) {
            throw new CustomException(SELF_REPORT_NOT_ALLOWED);
        }

    }

    private void validateAcceptedReporter(Long gameId, Long reporterId) {
        boolean accepted = participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                gameId, reporterId, ACCEPT
        );

        if (!accepted) {
            throw new CustomException(REPORTER_NOT_ACCEPTED_PARTICIPANT);
        }
    }

    private void validateAcceptedTarget(Long gameId, Long targetId) {
        boolean accepted = participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                gameId, targetId, ACCEPT
        );

        if (!accepted) {
            throw new CustomException(REPORT_TARGET_NOT_ACCEPTED_PARTICIPANT);
        }
    }

    private void validateDuplicateReport(Long reporterId, Long targetUserId, Long gameId) {

        boolean duplicate = reportRepository.existsByReportUser_UserIdAndTargetUser_UserIdAndGameEntity_GameId(
                reporterId, targetUserId, gameId
        );

        if (duplicate) {
            throw new CustomException(ALREADY_REPORTED_USER);
        }

    }
}
