package com.example.basketballmatching.game.service;

import com.example.basketballmatching.global.dto.CheckResponse;

public interface ParticipantGameService {






    CheckResponse kickOutGameUser(Long participantId, Long userId, Long gameId);

    CheckResponse deleteGame(Long userId, Long gameId);


}
