package com.example.basketballproject.gameUser.controller;


import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import com.example.basketballproject.gameUser.dto.GameSearchDto;
import com.example.basketballproject.gameUser.dto.UserCancelGameDto;
import com.example.basketballproject.gameUser.dto.UserJoinGameDto;
import com.example.basketballproject.gameUser.service.GameUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game-user")
public class GameUserController {

    private final GameUserService gameUserService;

    @GetMapping("/search")
    public ResponseEntity<List<GameSearchDto>> findFilteredGame(
            @RequestParam(required = false) LocalDate localDate,
            @RequestParam(required = false) CityName cityName,
            @RequestParam(required = false) FieldState fieldState,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) MatchFormat matchFormat) {

        return ResponseEntity.ok(
               gameUserService.findFilteredGame(localDate, cityName, fieldState, gender, matchFormat)
        );
    }


    @GetMapping("/search-address")
    public ResponseEntity<List<GameSearchDto>> searchAddress(
            @RequestParam String address
    ) {
        return ResponseEntity.ok(
                gameUserService.searchAddress(address));
    }

    @PostMapping("/participant")
    @PreAuthorize("hasRole('ROLE_USER')")
    public UserJoinGameDto participantGame(
            @RequestParam @Validated Long gameId
    ) {
        return UserJoinGameDto.from(
                gameUserService.participantGame(gameId)
        );

    }

    @PostMapping("/participant/cancel")
    @PreAuthorize("hasRole('ROLE_USER')")
    public UserCancelGameDto cancelGame(
            @RequestParam @Validated Long gameId
    ) {

        return UserCancelGameDto.cancelGame(
                gameUserService.cancelParticipantGame(gameId)
        );
    }

}
