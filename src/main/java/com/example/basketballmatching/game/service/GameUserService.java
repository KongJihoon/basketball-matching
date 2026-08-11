package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.dto.CurrentGameListDto;
import com.example.basketballmatching.game.dto.GameUserLevelDto;
import com.example.basketballmatching.game.dto.LastGameListDto;
import com.example.basketballmatching.global.dto.CommonResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GameUserService {




    CommonResponse<List<CurrentGameListDto>> getMyCurrentGameList(Long userId, Pageable pageable);

    CommonResponse<List<LastGameListDto>> getMyLastGameList(Long userId, Pageable pageable);


    CommonResponse<GameUserLevelDto> getMyGameUserLevel(Long userId);
}
