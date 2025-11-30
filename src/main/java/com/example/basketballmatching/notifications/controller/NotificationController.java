package com.example.basketballmatching.notifications.controller;


import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.notifications.dto.NotificationDto;
import com.example.basketballmatching.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
@Tag(name = "NOTIFICATION")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 구독 (SSE)
     */
    @Operation(summary = "알림 구독(SSE)")
    @ApiResponse(responseCode = "200", description = "SSE 구독 연결 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<SseEmitter> subscribe(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "lastEventId", description = "유실된 이벤트 복구를 위한 마지막 이벤트 ID")
            @RequestHeader(value = "lastEventId", required = false, defaultValue = "")
            String lastEventId) {


        SseEmitter sseEmitter = notificationService.subscribe(userInfoDetails.getUserEntity().getUserId(), lastEventId);


        return ResponseEntity.ok(sseEmitter);
    }
    /**
     * 읽지 않은 알림 조회
     */
    @Operation(summary = "읽지 않은 알림 조회")
    @ApiResponse(responseCode = "200", description = "읽지 않은 알림 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/unread-notification")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<List<NotificationDto>>> getUnreadNotifications(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);

        CommonResponse<List<NotificationDto>> unReadNotifications = notificationService.getUnReadNotifications(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(unReadNotifications);
    }



}
