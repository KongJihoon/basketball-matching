package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.dto.CreateGameDto;
import com.example.basketballmatching.game.dto.EditGameDto;
import com.example.basketballmatching.game.dto.GameDto;
import com.example.basketballmatching.game.dto.SearchGameDto;
import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.global.dto.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface GameService {

    CommonResponse<CreateGameDto.Response> createGame(Long userId, CreateGameDto.Request request);

    CommonResponse<GameDto> detailGame(Long gameId);

    CommonResponse<Page<SearchGameDto>> searchGame(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, Pageable pageable);

    CommonResponse<GameDto> editGame(EditGameDto request, Long gameId, Long userId);

}
