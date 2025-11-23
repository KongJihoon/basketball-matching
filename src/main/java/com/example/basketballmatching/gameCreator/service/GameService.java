package com.example.basketballmatching.gameCreator.service;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.dto.GameDto;
import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.global.dto.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface GameService {

    CommonResponse<CreateGameDto.Response> createGame(Long UserId, CreateGameDto.Request request);

    CommonResponse<GameDto> detailGame(Long gameId);

    CommonResponse<Page<SearchGameDto>> searchGame(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, Pageable pageable);

    CommonResponse<GameDto> editGame(EditGameDto request, Long gameId, Long userId);

}
