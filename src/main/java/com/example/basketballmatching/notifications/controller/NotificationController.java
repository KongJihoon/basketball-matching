package com.example.basketballmatching.notifications.controller;


import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.notifications.dto.response.NotificationResponse;
import com.example.basketballmatching.notifications.dto.response.ReadNotificationResponse;
import com.example.basketballmatching.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasRole('USER')")
@Validated
@Tag(name = "NOTIFICATION")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 구독 (SSE)
     */
    @Operation(summary = "알림 구독(SSE)", description = "SSE 연결을 통해 실시간 알림 구독")
    @ApiResponse(responseCode = "200", description = "SSE 구독 연결 성공")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
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
    @Operation(summary = "읽지 않은 알림 조회", description = "현재 사용자의 읽지 않은 알림을 조회한다.")
    @ApiResponse(responseCode = "200", description = "읽지 않은 알림 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
    content = @Content(
            mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)
    ))
    @GetMapping("/unread")
    public ResponseEntity<CommonResponse<List<NotificationResponse>>> getUnReadNotifications(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(0) int page,
            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100) int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);

        List<NotificationResponse> unReadNotifications = notificationService.getUnReadNotifications(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(
                CommonResponse.of("읽지 않은 알림 조회에 성공하였습니다.", unReadNotifications)
        );
    }

    @Operation(summary = "알림 읽음 처리", description = "현재 사용자가 받은 알림을 읽음 상태로 변경한다.")
    @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공")
    @ApiResponse(responseCode = "404", description = "알림 또는 사용자를 찾을 수 없음",
    content = @Content(
            mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)
    ))
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonResponse<ReadNotificationResponse>> readNotification(
            @PathVariable("notificationId") Long notificationId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        ReadNotificationResponse response = notificationService.readNotification(
                userInfoDetails.getUserEntity().getUserId() , notificationId
        );
        return ResponseEntity.ok(
                CommonResponse.of("알림 읽음 처리에 성공하였습니다.", response)
        );
    }



}
