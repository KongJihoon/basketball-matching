package com.example.basketballmatching.report.dto.response;

import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.type.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ReviewReportResponse(
        @Schema(description = "신고 ID", example = "1")
        Long reportId,

        @Schema(description = "신고 처리 상태")
        ReportStatus reportStatus,

        @Schema(description = "신고 처리 시각")
        LocalDateTime reviewedAt
) {
    public static ReviewReportResponse fromEntity(
            ReportEntity report
    ) {
        return new ReviewReportResponse(
                report.getReportId(),
                report.getReportStatus(),
                report.getReviewedDateTime()
        );
    }
}
