package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.dto.AcceptGameUserListDto;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ParticipantGameService {




    CommonResponse<List<AcceptGameUserListDto>> getAcceptParticipantList(Long gameId, Long userId, Pageable pageable);


    CheckResponse kickOutGameUser(Long participantId, Long userId, Long gameId);

    CheckResponse deleteGame(Long userId, Long gameId);


}
