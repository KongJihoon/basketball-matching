package com.example.basketballmatching.notifications.controller;


import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.notifications.dto.NotificationDto;
import com.example.basketballmatching.notifications.service.NotificationService;
import io.lettuce.core.dynamic.annotation.Param;
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
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<SseEmitter> subscribe(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestHeader(value = "lastEventId", required = false, defaultValue = "")
            String lastEventId) {


        SseEmitter sseEmitter = notificationService.subscribe(userInfoDetails.getUserEntity().getUserId(), lastEventId);


        return ResponseEntity.ok(sseEmitter);
    }

    @GetMapping("/unread-notification")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);

        ApiResponse<List<NotificationDto>> unReadNotifications = notificationService.getUnReadNotifications(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(unReadNotifications);
    }



}
