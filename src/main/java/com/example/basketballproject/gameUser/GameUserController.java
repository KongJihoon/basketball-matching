package com.example.basketballproject.gameUser;


import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import com.example.basketballproject.gameUser.dto.GameSearchDto;
import com.example.basketballproject.gameUser.service.GameUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
