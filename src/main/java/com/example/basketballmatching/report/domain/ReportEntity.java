package com.example.basketballmatching.report.domain;


import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.report.type.ReportStatus;
import com.example.basketballmatching.report.type.ReportType;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.REPORT_ALREADY_PROCESSED;
import static com.example.basketballmatching.report.type.ReportStatus.*;

@Table(
        name = "report_entity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_report_reporter_target_game",
                        columnNames = {
                                "report_user_id",
                                "target_user_id",
                                "game_entity_game_id"
                        }
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_user_id", nullable = false)
    private UserEntity reportUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private GameEntity gameEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime reportedDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus reportStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private UserEntity reviewedBy;

    @Column(length = 500)
    private String reviewReason;

    private LocalDateTime reviewedDateTime;


    @Builder(access = AccessLevel.PRIVATE)
    private ReportEntity(UserEntity reportUser, UserEntity targetUser, GameEntity game, ReportType reportType, String content, LocalDateTime reportedAt) {
        this.reportUser = reportUser;
        this.targetUser = targetUser;
        this.gameEntity = game;
        this.reportType = reportType;
        this.content = content;
        this.reportedDateTime = reportedAt;
        this.reportStatus = PENDING;
    }



    public static ReportEntity create(UserEntity reportUser, UserEntity targetUser, GameEntity game, ReportType reportType, String content, LocalDateTime reportedAt) {
        return ReportEntity.builder()
                .reportUser(reportUser)
                .targetUser(targetUser)
                .game(game)
                .reportType(reportType)
                .content(content)
                .reportedAt(reportedAt)
                .build();
    }

    public void approve(UserEntity reviewer, String reason, LocalDateTime reviewedAt) {
        validatePending();

        this.reportStatus = APPROVED;
        this.reviewedBy = reviewer;
        this.reviewReason = reason;
        this.reviewedDateTime = reviewedAt;
    }

    public void reject(UserEntity reviewer, String reason, LocalDateTime reviewedAt) {
        validatePending();
        this.reportStatus = REJECTED;
        this.reviewedBy = reviewer;
        this.reviewReason = reason;
        this.reviewedDateTime = reviewedAt;

    }

    public boolean isApproved() {
        return reportStatus == APPROVED;
    }

    private void validatePending() {
        if (reportStatus != PENDING) {
            throw new CustomException(REPORT_ALREADY_PROCESSED);
        }
    }


}
