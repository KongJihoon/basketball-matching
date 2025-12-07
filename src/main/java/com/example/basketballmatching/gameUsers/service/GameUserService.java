package com.example.basketballmatching.gameUsers.service;

import com.example.basketballmatching.gameUsers.dto.*;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameUserService {


    CommonResponse<ApplyGameUserDto> applyGame(Long gameId, Long UserId);

    CheckResponse cancelGame(Long userId, Long gameId);

    CommonResponse<List<CurrentGameListDto>> getMyCurrentGameList(Long userId, Pageable pageable);

    CommonResponse<List<LastGameListDto>> getMyLastGameList(Long userId, Pageable pageable);


    CommonResponse<GameUserLevelDto> getMyGameUserLevel(Long userId);
}
