package com.example.basketballmatching.game.controller;

import com.example.basketballmatching.game.dto.CreateGameDto;
import com.example.basketballmatching.game.dto.EditGameDto;
import com.example.basketballmatching.game.dto.GameDto;
import com.example.basketballmatching.game.dto.SearchGameDto;
import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.game.service.GameService;
import com.example.basketballmatching.global.dto.CommonResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
@Tag(name = "GAME")
public class GameController {

    private final GameService gameService;

    /**
     * 경기 생성
     */
    @Operation(summary = "경기 생성")
    @ApiResponse(responseCode = "200", description = "경기 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<CreateGameDto.Response>> createGame(
            @RequestBody @Valid CreateGameDto.Request request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        CommonResponse<CreateGameDto.Response> game = gameService.createGame(userInfoDetails.getUserEntity().getUserId(), request);


        return ResponseEntity.ok(game);

    }

    /**
     * 경기 상세 조회
     */
    @Operation(summary = "경기 상세 조회")
    @ApiResponse(responseCode = "200", description = "경기 상세 조회 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/details")
    public ResponseEntity<CommonResponse<GameDto>> detailGame(
            @Parameter(name = "gameId", example = "1")
            @RequestParam Long gameId
    ) {

        CommonResponse<GameDto> gameDto = gameService.detailGame(gameId);

        return ResponseEntity.ok(gameDto);
    }

    /**
     * 경기 검색 정렬
     */
    @Operation(summary = "경기 검색 정렬 ")
    @ApiResponse(responseCode = "200", description = "경기 검색 정렬 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/search")
    public ResponseEntity<CommonResponse<Page<SearchGameDto>>> searchGame(
            @Parameter(name = "date", example = "2025-12-02", required = true)
            @RequestParam @Valid LocalDate date,

            @Parameter(name = "cityName", example = "INCHEON")
            @RequestParam(required = false) CityName cityName,

            @Parameter(name = "matchFormat", example = "THREE_ON_THREE")
            @RequestParam(required = false) MatchFormat matchFormat,

            @Parameter(name = "fieldStatus", example = "INDOOR")
            @RequestParam(required = false) FieldStatus fieldStatus,

            @Parameter(name = "matchGenderType", example = "MALE_ONLY")
            @RequestParam(required = false) MatchGenderType matchGenderType,

            @Parameter(name = "gameStatus", example = "RECRUITING")
            @RequestParam(required = false) GameStatus gameStatus,

            @Parameter(name = "page", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(name = "size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {

        PageRequest pageRequest = PageRequest.of(page, size);

        CommonResponse<Page<SearchGameDto>> searchGame = gameService.searchGame(date, cityName, matchFormat, fieldStatus, matchGenderType, gameStatus, pageRequest);

        return ResponseEntity.ok(searchGame);
    }

    /**
     * 경기 수정
     */
    @Operation(summary = "경기 수정")
    @ApiResponse(responseCode = "200", description = "경기 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/edit")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<GameDto>> editGame(
            @RequestBody @Valid EditGameDto request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @Parameter(name = "gameId", example = "1")
            @RequestParam Long gameId
    ) {

        CommonResponse<GameDto> editGame = gameService.editGame(request, gameId, userInfoDetails.getUserEntity().getUserId());


        return ResponseEntity.ok(editGame);
    }




}
