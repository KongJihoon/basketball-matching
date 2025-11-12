package com.example.basketballmatching.gameUsers.service;

import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameUserService {


    ApiResponse<ApplyGameUserDto> applyGame(Long gameId, Long UserId);

    CheckResponse cancelGame(Long userId, Long gameId);

    ApiResponse<List<CurrentGameListDto>> getMyCurrentGameList(Long userId, Pageable pageable);

    ApiResponse<List<LastGameListDto>> getMyLastGameList(Long userId, Pageable pageable);
}
