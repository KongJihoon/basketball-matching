package com.example.basketballmatching.blackList.controller;

import com.example.basketballmatching.blackList.dto.BlackListDto;
import com.example.basketballmatching.blackList.service.BlackListService;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/blacklist")
@Tag(name = "BLACK_LIST")
public class BlackListController {

    private final BlackListService blackListService;

    /**
     * 유저 블랙리스트 등록
     */
    @Operation(summary = "유저 블랙리스트 등록")
    @ApiResponse(responseCode = "200", description = "유저 블랙리스트 등록 성공",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CheckResponse> createBlackListUser(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "reportId", example = "1")
            @RequestParam Long reportId
            ) {

        CheckResponse checkResponse = blackListService.createBlackListUser(userInfoDetails.getUserEntity().getUserId(), reportId);


        return ResponseEntity.ok(checkResponse);


    }

    /**
     * 블랙리스트 유저 조회
     */
    @Operation(summary = "블랙리스트 유저 조회")
    @ApiResponse(responseCode = "200", description = "블랙리스트 유저 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonResponse<Page<BlackListDto>>> getBlackListUsers(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);

        CommonResponse<Page<BlackListDto>> blackLists = blackListService.getBlackLists(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(blackLists);
    }

}
