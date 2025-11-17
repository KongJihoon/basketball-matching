package com.example.basketballmatching.report.dto;

import com.example.basketballmatching.report.type.ReportType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class CreateReportDto {

    @NotBlank(message = "신고 유형을 입력해주세요.")
    private ReportType reportType;

    @NotBlank(message = "신고 내용을 입력해주세요.")
    private String content;

}
