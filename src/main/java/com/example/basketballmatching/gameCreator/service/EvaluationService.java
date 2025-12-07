package com.example.basketballmatching.gameCreator.service;

import com.example.basketballmatching.gameUsers.dto.EvaluatePlayerDto;
import com.example.basketballmatching.global.dto.CheckResponse;

public interface EvaluationService {

    CheckResponse evaluatePlayer(Long gameId, Long evaluatorUserId, EvaluatePlayerDto evaluatePlayerDto);

}
