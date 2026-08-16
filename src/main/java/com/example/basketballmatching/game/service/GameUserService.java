package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.dto.GameUserLevelDto;
import com.example.basketballmatching.global.dto.CommonResponse;

public interface GameUserService {







    CommonResponse<GameUserLevelDto> getMyGameUserLevel(Long userId);
}
