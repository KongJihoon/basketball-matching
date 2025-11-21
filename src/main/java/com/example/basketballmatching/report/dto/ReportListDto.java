package com.example.basketballmatching.report.dto;

import com.example.basketballmatching.report.entity.ReportEntity;
import com.example.basketballmatching.report.type.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ReportListDto {

    private Long reportId;

    private Long reportUserId;

    private Long reportedUserId;

    private String reportedUserNickname;

    private ReportType reportType;

    private String content;

    private LocalDateTime reportedDateTime;

    public static ReportListDto fromEntity(ReportEntity report) {

        return ReportListDto.builder()
                .reportId(report.getReportId())
                .reportUserId(report.getReportUser().getUserId())
                .reportedUserId(report.getTargetUser().getUserId())
                .reportedUserNickname(report.getTargetUser().getNickname())
                .reportType(report.getReportType())
                .content(report.getContent())
                .reportedDateTime(report.getReportedDateTime())
                .build();


    }

}
