package com.example.basketballmatching.gameUsers.controller;


import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.EvaluatePlayerDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
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
public class GameUserController {

    private final GameUserService gameUserService;

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<ApplyGameUserDto>> applyGame(
            @RequestParam Long gameId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        ApiResponse<ApplyGameUserDto> applyGame = gameUserService.applyGame(gameId, userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(applyGame);

    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> cancelGame(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long gameId
    ) {

        CheckResponse checkResponse = gameUserService.cancelGame(userId, gameId);


        return ResponseEntity.ok(checkResponse);
    }

    @GetMapping("/user/current-game")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<List<CurrentGameListDto>>> getMyCurrentGameList (
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC,"gameEntity_startDateTime");

        ApiResponse<List<CurrentGameListDto>> myCurrentGameList = gameUserService.getMyCurrentGameList(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(myCurrentGameList);
    }


    @GetMapping("/user/last-game")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<List<LastGameListDto>>> getMyLastGameList (
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC,"gameEntity_startDateTime");

        ApiResponse<List<LastGameListDto>> myCurrentGameList = gameUserService.getMyLastGameList(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(myCurrentGameList);
    }

    @PostMapping("/user/evaluate/{gameId}")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> evaluatePlayer(
            @AuthenticationPrincipal UserInfoDetails userInfoDetails,
            @PathVariable Long gameId,
            @RequestBody @Valid EvaluatePlayerDto request
            ) {

        CheckResponse checkResponse = gameUserService.evaluatePlayer(gameId, userInfoDetails.getUserEntity().getUserId(), request);


        return ResponseEntity.ok(checkResponse);
    }

}
