package com.example.basketballmatching.report.dto;

import com.example.basketballmatching.report.type.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CreateReportDto {

    @Schema(name = "신고 유형", example = "POOR_SPORTSMANSHIP", defaultValue = "POOR_SPORTSMANSHIP")
    @NotBlank(message = "신고 유형을 입력해주세요.")
    private ReportType reportType;

    @Schema(name = "신고 내용", example = "비매너 행위", defaultValue = "비매너 행위")
    @NotBlank(message = "신고 내용을 입력해주세요.")
    private String content;

}
