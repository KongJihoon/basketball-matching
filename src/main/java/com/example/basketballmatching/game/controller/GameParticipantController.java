package com.example.basketballmatching.game.controller;

import com.example.basketballmatching.game.dto.response.GameParticipantListResponse;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/games")
@Validated
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

    @Operation(summary = "경기 참가자 목록 조회", description = "경기 생성자가 참가 확정된 사용자 목록을 조회한다.")
    @ApiResponse(responseCode = "200", description = "경기 참가자 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "경기 생성자가 아님",
    content = @Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "사용자 또는 경기를 찾을 수 없다.",
    content = @Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{gameId}/participants")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommonResponse<Page<GameParticipantListResponse>>> getParticipants(
            @Parameter(description = "경기ID", example = "1", required = true)
            @PathVariable("gameId")
            Long gameId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100)
            int size,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "acceptDateTime"));


        Page<GameParticipantListResponse> response = gameParticipantService.getParticipants(gameId, userInfoDetails.getUserEntity().getUserId(), pageRequest);


        return ResponseEntity.ok(
                CommonResponse.of("경기 참가자 목록 조회가 완료되었습니다.", response)
        );
    }

    @Operation(summary = "경기 참가자 강퇴", description = "경기 생성자가 참가자를 경기에서 강퇴한다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "참가자 강퇴 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "경기 생성자 본인 강퇴 또는 경기 시작 1시간 이내 요청"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "경기 생성자가 아닌 사용자"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "경기 또는 참가 정보 없음"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 취소 또는 강퇴된 참가자"
            )
    })
    @PatchMapping("/{gameId}/participants/{participantId}/kickout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckResponse> kickoutParticipant(
            @PathVariable("gameId") Long gameId,
            @PathVariable("participantId") Long participantId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        gameParticipantService.kickoutParticipant(gameId, participantId, userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(
                CheckResponse.of(true, "참가자를 강퇴하였습니다.")
        );

    }
}
