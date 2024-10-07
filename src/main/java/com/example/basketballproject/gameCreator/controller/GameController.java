package com.example.basketballproject.gameCreator.controller;


import com.example.basketballproject.gameCreator.dto.CreateGameDto;
import com.example.basketballproject.gameCreator.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;


    /**
     * 경기 생성
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CreateGameDto.Response> createGame(
            @RequestBody @Validated CreateGameDto.Request request
    ) {

        CreateGameDto.Response game = gameService.createGame(request);

        return ResponseEntity.ok(game);

    }

}
