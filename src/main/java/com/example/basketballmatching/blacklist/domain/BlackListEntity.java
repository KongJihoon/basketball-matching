package com.example.basketballmatching.blacklist.domain;


import com.example.basketballmatching.blacklist.type.BlackListStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(
        name = "black_list_entity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_black_list_report",
                        columnNames = "report_id"
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlackListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blackListId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private ReportEntity reportEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banned_by_user_id", nullable = false)
    private UserEntity bannedBy;

    @Column(nullable = false)
    private LocalDateTime bannedDateTime;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder(access = AccessLevel.PRIVATE)
    private BlackListEntity(ReportEntity reportEntity, UserEntity userEntity, UserEntity bannedBy, LocalDateTime bannedDateTime, LocalDateTime expiresAt) {
        this.reportEntity = reportEntity;
        this.userEntity = userEntity;
        this.bannedBy = bannedBy;
        this.bannedDateTime = bannedDateTime;
        this.expiresAt = expiresAt;
    }


    public static BlackListEntity create(ReportEntity report, UserEntity bannedBy, LocalDateTime bannedAt, LocalDateTime expiresAt) {

        validateApprovedReport(report);

        validatePeriod(bannedAt, expiresAt);

        return BlackListEntity.builder()
                .reportEntity(report)
                .userEntity(report.getTargetUser())
                .bannedBy(bannedBy)
                .bannedDateTime(bannedAt)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isActive(LocalDateTime now) {
        return expiresAt.isAfter(now);
    }

    public BlackListStatus getStatus(LocalDateTime now) {
        return isActive(now) ? BlackListStatus.ACTIVE : BlackListStatus.EXPIRED;
    }



    private static void validateApprovedReport(ReportEntity report) {
        if (!report.isApproved()) {
            throw new CustomException(ErrorCode.REPORT_NOT_APPROVED);
        }
    }

    private static void validatePeriod(LocalDateTime bannedAt, LocalDateTime expiresAt) {

        if (!expiresAt.isAfter(bannedAt)) {
            throw new CustomException(ErrorCode.INVALID_BLACKLIST_PERIOD);
        }

    }




}
