package com.example.basketballmatching.blacklist.service;


import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import com.example.basketballmatching.blacklist.dto.response.BlackListResponse;
import com.example.basketballmatching.blacklist.dto.response.CreateBlackListResponse;
import com.example.basketballmatching.blacklist.event.UserBlacklistedEvent;
import com.example.basketballmatching.blacklist.repository.BlackListRepository;
import com.example.basketballmatching.blacklist.type.BlackListStatus;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.service.BlackListGameService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class BlackListService {

    private static final long DEFAULT_BAN_DAYS = 7L;

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final BlackListRepository blackListRepository;
    private final BlackListGameService blackListGameService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public CreateBlackListResponse createBlackList(Long adminId, Long reportId) {

        UserEntity admin = getActiveUser(adminId);

        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_REPORT));

        validateApprovedReport(report);
        validateUnusedReport(reportId);

        LocalDateTime bannedAt = LocalDateTime.now(clock);

        UserEntity targetUser = report.getTargetUser();

        validateNotCurrentlyBlacklisted(targetUser.getUserId(), bannedAt);

        LocalDateTime expiresAt = bannedAt.plusDays(DEFAULT_BAN_DAYS);

        BlackListEntity blackList = BlackListEntity.create(report, admin, bannedAt, expiresAt);

        BlackListEntity savedBlackList = blackListRepository.save(blackList);

        BlackListGameResultDto gameResult = blackListGameService.cleanup(targetUser.getUserId(), bannedAt);

        eventPublisher.publishEvent(
                new UserBlacklistedEvent(
                        targetUser.getUserId(),
                        targetUser.getEmail(),
                        bannedAt,
                        expiresAt,
                        gameResult.notices()
                )
        );


        return CreateBlackListResponse.fromEntity(savedBlackList, bannedAt);

    }

    @Transactional(readOnly = true)
    public Page<BlackListResponse> getBlackLists(Long adminId, BlackListStatus status, Pageable pageable) {

        getActiveUser(adminId);

        LocalDateTime now = LocalDateTime.now(clock);

        Page<BlackListEntity> blackLists = switch (status) {
            case ACTIVE -> blackListRepository.findAllByExpiresAtAfterOrderByBannedDateTimeDesc(
                    now, pageable
            );
            case EXPIRED -> blackListRepository.findAllByExpiresAtLessThanEqualOrderByBannedDateTimeDesc(
                    now, pageable
            );
        };

        return blackLists.map(
                blackList ->
                    BlackListResponse.fromEntity(blackList, now)
        );


    }

    private UserEntity getActiveUser(Long userId) {

        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void validateApprovedReport(ReportEntity report) {
        if (!report.isApproved()) {
            throw new CustomException(REPORT_NOT_APPROVED);
        }
    }

    private void validateUnusedReport(Long reportId) {
        if (blackListRepository.existsByReportEntity_ReportId(reportId)) {
            throw new CustomException(BLACKLIST_REPORT_ALREADY_USED);
        }
    }

    private void validateNotCurrentlyBlacklisted(Long targetUserId, LocalDateTime now) {
        if (blackListRepository.existsByUserEntity_UserIdAndExpiresAtAfter(targetUserId, now)) {
            throw new CustomException(ALREADY_BLACK_USER);
        }
    }






}
