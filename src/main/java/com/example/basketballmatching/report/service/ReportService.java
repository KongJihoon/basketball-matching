package com.example.basketballmatching.report.service;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.report.dto.CreateReportDto;
import com.example.basketballmatching.report.dto.ReportListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {


    CheckResponse createReport(Long reportId, Long reportedId, Long gameId, CreateReportDto createReportDto);

    ApiResponse<Page<ReportListDto>> getReportedUserList(Long userId, Pageable pageable);


}
