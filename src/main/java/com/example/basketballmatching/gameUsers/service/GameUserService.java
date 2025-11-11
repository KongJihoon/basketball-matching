package com.example.basketballmatching.gameUsers.service;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.global.dto.CheckResponse;

public interface GameUserService {


    ApiResponse<ApplyGameUserDto> applyGame(Long gameId, Long UserId);

    CheckResponse cancelGame(Long userId, Long gameId);



}
