package com.example.basketballmatching.game.controller;


import com.example.basketballmatching.game.dto.AcceptGameUserListDto;
import com.example.basketballmatching.game.service.ParticipantGameService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/creator")
@RequiredArgsConstructor
@Tag(name = "PARTICIPANT")
public class GameParticipationController {

    private final ParticipantGameService participantGameService;



    /**
     * 경기 참가 수락자 조회
     */
    @Operation(summary = "경기 참가 수락자 조회")
    @ApiResponse(responseCode = "200", description = "경기 참가 수락자 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/search/accept")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<List<AcceptGameUserListDto>>> getAcceptParticipantList(
            @Parameter(name = "gameId", example = "1", required = true)
            @RequestParam Long gameId,
            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");

        CommonResponse<List<AcceptGameUserListDto>> acceptParticipantList = participantGameService.getAcceptParticipantList(gameId, userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(acceptParticipantList);
    }



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
