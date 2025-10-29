package com.example.basketballmatching.gameCreator.controller;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.global.security.UserInfoDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
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


}
