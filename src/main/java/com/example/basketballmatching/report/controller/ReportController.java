package com.example.basketballmatching.report.controller;

import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.report.dto.request.CreateReportRequest;
import com.example.basketballmatching.report.dto.response.CreateReportResponse;
import com.example.basketballmatching.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/games/{gameId}/reports")
@Tag(name = "REPORT")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "경기 참가자 신고", description = """
            종료된 경기의 참가 확정 사용자가 같은 경기의 다른 확정 참가자를 신고한다.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "신고 접수 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "자기 신고 또는 잘못된 신고 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "신고 자격이 없는 사용자"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "경기 또는 사용자를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "신고 기간 만료 또는 중복 신고"
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommonResponse<CreateReportResponse>> createReport(
            @PathVariable("gameId") Long gameId,
            @RequestBody @Valid CreateReportRequest request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        CreateReportResponse response = reportService.createReport(userInfoDetails.getUserEntity().getUserId(), gameId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{reportId}")
                .buildAndExpand(response.reportId())
                .toUri();


        return ResponseEntity.created(location)
                .body(CommonResponse.of("신고가 접수되었습니다.", response));
    }







}
