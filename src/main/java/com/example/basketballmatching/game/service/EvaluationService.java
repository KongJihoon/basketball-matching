package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.dto.EvaluatePlayerDto;
import com.example.basketballmatching.global.dto.CheckResponse;

public interface EvaluationService {

    CheckResponse evaluatePlayer(Long gameId, Long evaluatorUserId, EvaluatePlayerDto evaluatePlayerDto);

}
