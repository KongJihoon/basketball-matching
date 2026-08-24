package com.example.basketballmatching.report.dto.request;

import com.example.basketballmatching.report.type.ReportDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReportRequest(
        @Schema(description = "신고 처리 결정", example = "APPROVE")
        @NotNull(message = "신고 처리 결정을 입력해주세요.")
        ReportDecision decision,

        @Schema(description = "신고 처리 사유", example = "신고 내용을 확인하여 제재 대상으로 판단했습니다.")
        @NotBlank(message = "신고 처리 사유를 입력해주세요.")
        @Size(max = 500, message = "신고 처리 사유는 500자 이하여야 합니다.")
        String reason
) {
}
