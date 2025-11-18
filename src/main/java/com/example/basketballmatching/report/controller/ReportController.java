package com.example.basketballmatching.report.controller;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.report.dto.CreateReportDto;
import com.example.basketballmatching.report.dto.ReportListDto;
import com.example.basketballmatching.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/report")
public class ReportController {

    private final ReportService reportService;


    @PostMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> createReport(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam Long reportedUserId,
            @RequestParam Long gameId,
            @RequestBody CreateReportDto request) {

        CheckResponse checkResponse = reportService.createReport(userInfoDetails.getUserEntity().getUserId(), reportedUserId, gameId, request);


        return ResponseEntity.ok(checkResponse);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReportListDto>>> getReportUserList(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);

        ApiResponse<Page<ReportListDto>> reportedUserList = reportService.getReportedUserList(userInfoDetails.getUserEntity().getUserId(), pageRequest);


        return ResponseEntity.ok(reportedUserList);
    }


}
