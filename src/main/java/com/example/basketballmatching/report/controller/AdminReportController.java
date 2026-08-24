package com.example.basketballmatching.report.controller;


import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.report.dto.request.ReviewReportRequest;
import com.example.basketballmatching.report.dto.response.ReportListResponse;
import com.example.basketballmatching.report.dto.response.ReviewReportResponse;
import com.example.basketballmatching.report.service.ReportService;
import com.example.basketballmatching.report.type.ReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Tag(name = "ADMIN_REPORT")
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "관리자 신고 목록 조회", description = "관리자가 신고 처리 상태에 따라 신고 목록 조회")
    @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "관리자 권한이 없는 참가자",
    content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
    ))
    @ApiResponse(responseCode = "404", description = "관리자 사용자를 찾을 수 없다.",
    content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
    ))
    @GetMapping
    public ResponseEntity<CommonResponse<Page<ReportListResponse>>> getReports(
                    @RequestParam(defaultValue = "PENDING")
                    ReportStatus status,

                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(100)
                    int size,
                    @AuthenticationPrincipal
                    UserInfoDetails userInfoDetails) {
        Page<ReportListResponse> response =
                reportService.getReports(
                        userInfoDetails
                                .getUserEntity()
                                .getUserId(),
                        status,
                        PageRequest.of(page, size)
                );

        return ResponseEntity.ok(
                CommonResponse.of(
                        "신고 목록 조회에 성공하였습니다.",
                        response
                )
        );
    }

    @Operation(
            summary = "관리자 신고 승인 또는 거절",
            description = "관리자가 접수 대기 중인 신고를 승인하거나 거절합니다."
    )
    @ApiResponse(responseCode = "200", description = "신고 검토 성공")
    @ApiResponse(responseCode = "403", description = "관리자 권한이 없는 참가자",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "관리자 사용자를 찾을 수 없다.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{reportId}")
    public ResponseEntity<CommonResponse<ReviewReportResponse>> reviewReport(
            @PathVariable("reportId")
            Long reportId,
            @RequestBody @Valid
            ReviewReportRequest request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        ReviewReportResponse response = reportService.reviewReport(userInfoDetails.getUserEntity().getUserId(), reportId, request);

        return ResponseEntity.ok(
                CommonResponse.of("신고 검토가 완료되었습니다.", response)
        );
    }
}
