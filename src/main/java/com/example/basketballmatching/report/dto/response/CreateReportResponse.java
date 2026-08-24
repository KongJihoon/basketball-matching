package com.example.basketballmatching.report.dto.response;

import com.example.basketballmatching.report.domain.ReportEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CreateReportResponse(
        @Schema(description = "신고 ID", example = "1")
        Long reportId,

        @Schema(description = "신고 접수 시각", example ="2026-08-24T15:00:00")
        LocalDateTime reportedAt
) {

    public static CreateReportResponse fromEntity(ReportEntity report) {
        return new CreateReportResponse(
                report.getReportId(),
                report.getReportedDateTime()
        );
    }
}
