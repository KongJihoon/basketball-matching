package com.example.basketballmatching.blackList.controller;

import com.example.basketballmatching.blackList.dto.BlackListDto;
import com.example.basketballmatching.blackList.service.BlackListService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
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
public class BlackListController {

    private final BlackListService blackListService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CheckResponse> createBlackListUser(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam Long reportId
            ) {

        CheckResponse checkResponse = blackListService.createBlackListUser(userInfoDetails.getUserEntity().getUserId(), reportId);


        return ResponseEntity.ok(checkResponse);


    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BlackListDto>>> getBlackListUsers(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);

        ApiResponse<Page<BlackListDto>> blackLists = blackListService.getBlackLists(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(blackLists);
    }

}
