package com.example.basketballmatching.report.controller;

import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.report.dto.CreateReportDto;
import com.example.basketballmatching.report.dto.ReportListDto;
import com.example.basketballmatching.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "REPORT")
public class ReportController {

    private final ReportService reportService;


    @Operation(summary = "유저 신고 등록")
    @ApiResponse(responseCode = "200", description = "유저 신고 등록 성공",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> createReport(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "신고받은 유저 아이디", example = "1")
            @RequestParam Long targetUserId,
            @Parameter(name = "해당 게임 아이디", example = "1")
            @RequestParam Long gameId,
            @RequestBody @Valid CreateReportDto request) {

        CheckResponse checkResponse = reportService.createReport(userInfoDetails.getUserEntity().getUserId(), targetUserId, gameId, request);


        return ResponseEntity.ok(checkResponse);
    }

    @Operation(summary = "신고 받은 유저 조회")
    @ApiResponse(responseCode = "200", description = "신고 받은 유저 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonResponse<Page<ReportListDto>>> getReportUserList(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "페이지", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "페이지 사이즈", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);

        CommonResponse<Page<ReportListDto>> reportedUserList = reportService.getReportedUserList(userInfoDetails.getUserEntity().getUserId(), pageRequest);


        return ResponseEntity.ok(reportedUserList);
    }


}
