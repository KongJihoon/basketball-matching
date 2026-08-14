package com.example.basketballmatching.game.controller;


import com.example.basketballmatching.game.service.ParticipantGameService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game/creator")
@RequiredArgsConstructor
@Tag(name = "PARTICIPANT")
public class GameParticipationController {

    private final ParticipantGameService participantGameService;





    /**
     * 경기 수락자 강퇴
     */
    @Operation(summary = "경기 수락자 강퇴")
    @ApiResponse(responseCode = "200", description = "경기 수락자 강퇴 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/kickout")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> kickoutGameUser (
            @Parameter(name = "gameId", example = "1", required = true)
            @RequestParam Long gameId,
            @Parameter(name = "participantId", example = "1", required = true)
            @RequestParam Long participantId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.kickOutGameUser(participantId, userInfoDetails.getUserEntity().getUserId(), gameId);

        return ResponseEntity.ok(checkResponse);

    }

    /**
     * 경기 삭제
     */
    @Operation(summary = "경기 삭제")
    @ApiResponse(responseCode = "200", description = "경기 삭제 성공",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/delete")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> deleteGame (
            @Parameter(name = "gameId", example = "1L")
            @RequestParam Long gameId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.deleteGame(userInfoDetails.getUserEntity().getUserId(), gameId);


        return ResponseEntity.ok(checkResponse);
    }

}
