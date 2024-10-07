package com.example.basketballproject.gameCreator.controller;


import com.example.basketballproject.gameCreator.dto.CreateGameDto;
import com.example.basketballproject.gameCreator.dto.DeleteGameDto;
import com.example.basketballproject.gameCreator.dto.UpdateGameDto;
import com.example.basketballproject.gameCreator.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 경기 수정
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UpdateGameDto.Response> updateGame(
            @RequestBody @Validated UpdateGameDto.Request request) {

        UpdateGameDto.Response response = gameService.updateGame(request);

        return ResponseEntity.ok(response);

    }


    /**
     * 경기 삭제
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<DeleteGameDto> updateGame(
            @RequestBody @Validated DeleteGameDto request) {


        DeleteGameDto deleteGame = gameService.deleteGame(request);

        return ResponseEntity.ok(deleteGame);

    }
}
