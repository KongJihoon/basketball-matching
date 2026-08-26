package com.example.basketballmatching.blacklist.controller;

import com.example.basketballmatching.blacklist.dto.request.CreateBlackListRequest;
import com.example.basketballmatching.blacklist.dto.response.BlackListResponse;
import com.example.basketballmatching.blacklist.dto.response.CreateBlackListResponse;
import com.example.basketballmatching.blacklist.service.BlackListService;
import com.example.basketballmatching.blacklist.type.BlackListStatus;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/blacklists")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@Tag(name = "ADMIN_BLACKLIST")
public class AdminBlackListController {

    private final BlackListService blackListService;

    @Operation(
            summary = "블랙리스트 제재 등록",
            description = "승인된 신고를 근거로 사용자를 7일간 제재합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "블랙리스트 제재 등록 성공"
    )
    @ApiResponse(
            responseCode = "404",
            description = "신고 또는 관리자를 찾을 수 없음",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ErrorResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "미승인·재사용 신고 또는 이미 제재 중인 사용자",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = ErrorResponse.class
                    )
            )
    )
    @PostMapping
    public ResponseEntity<CommonResponse<CreateBlackListResponse>> createBlackList(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestBody @Valid CreateBlackListRequest request
            ) {

        CreateBlackListResponse response = blackListService.createBlackList(userInfoDetails.getUserEntity().getUserId() , request.reportId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        CommonResponse.of("블랙리스트 제재 등록에 성공하였습니다.", response)
                );

    }

    @Operation(
            summary = "블랙리스트 목록 조회",
            description = "관리자가 활성 또는 만료된 블랙리스트 이력을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "블랙리스트 목록 조회 성공"
    )
    @GetMapping
    public ResponseEntity<CommonResponse<Page<BlackListResponse>>> getBlackLists(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam(defaultValue = "ACTIVE")
            BlackListStatus status,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        Page<BlackListResponse> response = blackListService.getBlackLists(userInfoDetails.getUserEntity().getUserId() , status, PageRequest.of(page, size));

        return ResponseEntity.ok(
                CommonResponse.of("블랙리스트 목록 조회에 성공하였습니다.", response)
        );
    }

}
