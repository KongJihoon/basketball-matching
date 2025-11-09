package com.example.basketballmatching.gameUsers.service;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;

public interface GameUserService {


    ApiResponse<ApplyGameUserDto> applyGame(Long gameId, Long UserId);

}
