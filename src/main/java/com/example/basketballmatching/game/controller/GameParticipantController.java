package com.example.basketballmatching.game.controller;

import com.example.basketballmatching.game.dto.response.GameParticipantResponse;
import com.example.basketballmatching.game.service.GameParticipantService;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/games")
@Tag(name = "GAME_PARTICIPANT")
public class GameParticipantController {

    private final GameParticipantService gameParticipantService;

    @Operation(summary = "경기 선착순 참가 ")
    @ApiResponse(responseCode = "201", description = "경기 참가 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 참가 신청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/{gameId}/participations")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommonResponse<GameParticipantResponse>> join(
            @Parameter(description = "경기 ID", example = "1", required = true)
            @PathVariable("gameId")
            Long gameId,
            @AuthenticationPrincipal
            UserInfoDetails userInfoDetails
    ) {

        GameParticipantResponse response = gameParticipantService.join(gameId, userInfoDetails.getUserEntity().getUserId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{participationId}")
                .buildAndExpand(
                        response.participationId()
                ).toUri();
        return ResponseEntity.created(location)
                .body(CommonResponse.of("경기 참가가 완료되었습니다.", response));
    }

    @Operation(summary = "경기 참가 신청 취소")
    @ApiResponse(responseCode = "200", description = "경기 참가 신청 취소 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 경기 참가 취소",
    content = @Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{gameId}/participations/me/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckResponse> cancelParticipation(
            @Parameter(description = "경기 ID", example = "1", required = true)
            @PathVariable("gameId") Long gameId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        gameParticipantService.cancelParticipation(gameId, userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(
                CheckResponse.of(true, "경기 참가 취소가 완료되었습니다.")
        );

    }
}
