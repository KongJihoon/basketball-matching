package com.example.basketballmatching.gameCreator.service;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.global.dto.ApiResponse;

public interface GameService {

    ApiResponse<CreateGameDto.Response> createGame(Long UserId, CreateGameDto.Request request);

}
