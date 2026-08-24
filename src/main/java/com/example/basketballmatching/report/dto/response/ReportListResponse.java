package com.example.basketballmatching.report.dto.response;

import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.type.ReportStatus;
import com.example.basketballmatching.report.type.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ReportListResponse(
        @Schema(description = "신고 ID", example = "1")
        Long reportId,

        @Schema(description = "경기 ID", example = "1")
        Long gameId,

        @Schema(description = "경기 제목")
        String gameTitle,

        @Schema(description = "신고자 ID", example = "1")
        Long reporterId,

        @Schema(description = "신고자 닉네임")
        String reporterNickname,

        @Schema(description = "신고 대상 ID", example = "2")
        Long targetUserId,

        @Schema(description = "신고 대상 닉네임")
        String targetNickname,

        @Schema(description = "신고 유형")
        ReportType reportType,

        @Schema(description = "신고 내용")
        String content,

        @Schema(description = "신고 상태")
        ReportStatus reportStatus,

        @Schema(description = "신고 접수 시각")
        LocalDateTime reportedAt,

        @Schema(description = "신고 검토 시각")
        LocalDateTime reviewedAt,

        @Schema(description = "신고 검토 사유")
        String reviewReason
) {
    public static ReportListResponse fromEntity(ReportEntity report) {
        return new ReportListResponse(
                report.getReportId(),
                report.getGameEntity().getGameId(),
                report.getGameEntity().getTitle(),
                report.getReportUser().getUserId(),
                report.getReportUser().getNickname(),
                report.getTargetUser().getUserId(),
                report.getTargetUser().getNickname(),
                report.getReportType(),
                report.getContent(),
                report.getReportStatus(),
                report.getReportedDateTime(),
                report.getReviewedDateTime(),
                report.getReviewReason()
        );
    }
}
