package com.example.basketballmatching.gameUsers.controller;


import com.example.basketballmatching.gameCreator.service.EvaluationService;
import com.example.basketballmatching.gameCreator.service.impl.UserLevelService;
import com.example.basketballmatching.gameUsers.dto.*;
import com.example.basketballmatching.gameUsers.service.GameUserService;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/game")
@Tag(name = "GAME_USER")
public class GameUserController {

    private final GameUserService gameUserService;
    private final EvaluationService evaluationService;

    /**
     * 경기 참가
     */
    @Operation(summary = "경기 참가")
    @ApiResponse(responseCode = "200", description = "경기 참가 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<ApplyGameUserDto>> applyGame(
            @Parameter(name = "gameId", example = "1")
            @RequestParam Long gameId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        CommonResponse<ApplyGameUserDto> applyGame = gameUserService.applyGame(gameId, userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(applyGame);

    }
    /**
     * 참가 경기 취소
     */
    @Operation(summary = "참가 경기 취소")
    @ApiResponse(responseCode = "200", description = "참가 경기 취소 성공",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/cancel")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> cancelGame(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "gameId", example = "1")
            @RequestParam Long gameId
    ) {

        CheckResponse checkResponse = gameUserService.cancelGame(userInfoDetails.getUserEntity().getUserId(), gameId);


        return ResponseEntity.ok(checkResponse);
    }

    /**
     * 현재 예정 경기 조회
     */
    @Operation(summary = "현재 예정 경기 조회")
    @ApiResponse(responseCode = "200", description = "현재 예정 경기 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/user/current-game")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<List<CurrentGameListDto>>> getMyCurrentGameList (
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ){

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC,"gameEntity_startDateTime");

        CommonResponse<List<CurrentGameListDto>> myCurrentGameList = gameUserService.getMyCurrentGameList(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(myCurrentGameList);
    }


    /**
     * 지난 경기 조회
     */
    @Operation(summary = "지난 경기 조회")
    @ApiResponse(responseCode = "200", description = "지난 경기 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/user/last-game")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<List<LastGameListDto>>> getMyLastGameList (
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ){

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC,"gameEntity_startDateTime");

        CommonResponse<List<LastGameListDto>> myCurrentGameList = gameUserService.getMyLastGameList(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(myCurrentGameList);
    }

    /**
     * 참가자 경기 실력 평가
     */
    @Operation(summary = "참가자 경기 실력 평가")
    @ApiResponse(responseCode = "200", description = "참가자 경기 실력 평가 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/user/evaluate/{gameId}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> evaluatePlayer(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "gameId", example = "1")
            @PathVariable Long gameId,
            @RequestBody @Valid EvaluatePlayerDto request
            ) {

        CheckResponse checkResponse = evaluationService.evaluatePlayer(gameId, userInfoDetails.getUserEntity().getUserId(), request);


        return ResponseEntity.ok(checkResponse);
    }

    /**
     * 유저 랭크 조회
     */
    @Operation(summary = "유저 랭크 조회")
    @ApiResponse(responseCode = "200", description = "유저 랭크 조회")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/user/rank")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<GameUserLevelDto>> getMyGameUserLevel(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CommonResponse<GameUserLevelDto> myGameUserLevel = gameUserService.getMyGameUserLevel(userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(myGameUserLevel);
    }

}
