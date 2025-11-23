package com.example.basketballmatching.gameCreator.controller;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.dto.GameDto;
import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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
public class GameController {

    private final GameService gameService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<CreateGameDto.Response>> createGame(
            @RequestBody @Valid CreateGameDto.Request request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        ApiResponse<CreateGameDto.Response> game = gameService.createGame(userInfoDetails.getUserEntity().getUserId(), request);


        return ResponseEntity.ok(game);

    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse<GameDto>> detailGame(
            @RequestParam Long gameId
    ) {

        ApiResponse<GameDto> gameDto = gameService.detailGame(gameId);

        return ResponseEntity.ok(gameDto);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<SearchGameDto>>> searchGame(
            @RequestParam @Valid LocalDate date,
            @RequestParam(required = false) CityName cityName,
            @RequestParam(required = false) MatchFormat matchFormat,
            @RequestParam(required = false) FieldStatus fieldStatus,
            @RequestParam(required = false) MatchGenderType matchGenderType,
            @RequestParam(required = false) GameStatus gameStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        PageRequest pageRequest = PageRequest.of(page, size);

        ApiResponse<Page<SearchGameDto>> searchGame = gameService.searchGame(date, cityName, matchFormat, fieldStatus, matchGenderType, gameStatus, pageRequest);

        return ResponseEntity.ok(searchGame);
    }

    @PatchMapping("/edit")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<GameDto>> editGame(
            @RequestBody @Valid EditGameDto request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam Long gameId
    ) {

        ApiResponse<GameDto> editGame = gameService.editGame(request, gameId, userInfoDetails.getUserEntity().getUserId());


        return ResponseEntity.ok(editGame);
    }




}
