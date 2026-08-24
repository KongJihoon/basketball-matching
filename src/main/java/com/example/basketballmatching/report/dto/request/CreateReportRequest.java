package com.example.basketballmatching.report.dto.request;

import com.example.basketballmatching.report.type.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @Schema(description = "신고 대상 사용자 ID", example = "2")
        @NotNull(message = "신고 대상 사용자를 입력해주세요.")
        @Positive(message = "신고 대상 사용자 ID가 올바르지 않습니다.")
        Long targetUserId,

        @Schema(description = "신고 유형", example = "POOR_SPORTSMANSHIP")
        @NotNull(message = "신고 유형을 입력해주세요.")
        ReportType reportType,

        @Schema(description = "신고 내용", example = "경기 중 고의로 비매너 행위를 반복했습니다.")
        @NotBlank(message = "신고 내용을 입력해주세요.")
        @Size(max = 500, message = "신고 내용은 500자 이하여야 합니다.")
        String content
) {
}
