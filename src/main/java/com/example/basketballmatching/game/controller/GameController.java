package com.example.basketballmatching.game.controller;

import com.example.basketballmatching.game.dto.EditGameDto;
import com.example.basketballmatching.game.dto.GameDto;
import com.example.basketballmatching.game.dto.SearchGameDto;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.dto.response.GameDetailResponse;
import com.example.basketballmatching.game.service.GameService;
import com.example.basketballmatching.game.type.*;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
@Tag(name = "GAME")
public class GameController {

    private final GameService gameService;

    /**
     * 경기 생성
     */
    @Operation(summary = "경기 생성")
    @ApiResponse(responseCode = "201", description = "경기 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = "409", description = "동일 장소의 경기 시간 중복",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<CreateGameResponse>> createGame(
            @RequestBody @Valid CreateGameRequest request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {


        CreateGameResponse response = gameService.createGame(userInfoDetails.getUserEntity().getUserId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{gameId}")
                .buildAndExpand(response.gameId())
                .toUri();

        return ResponseEntity.created(location)
                .body(CommonResponse.of("경기 생성이 완료되었습니다.", response));

    }

    /**
     * 경기 상세 조회
     */
    @Operation(summary = "경기 상세 조회")
    @ApiResponse(responseCode = "200", description = "경기 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "경기를 찾을 수 없음",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/{gameId}")
    public ResponseEntity<CommonResponse<GameDetailResponse>> detailGame(
            @Parameter(name = "gameId", example = "1", required = true)
            @PathVariable("gameId") Long gameId
    ) {

        GameDetailResponse response = gameService.getGameDetail(gameId);

        return ResponseEntity.ok(
                CommonResponse.of("경기 상세 조회에 성공하였습니다.", response)
        );
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
