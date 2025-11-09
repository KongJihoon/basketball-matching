package com.example.basketballmatching.gameCreator.service;

import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ParticipantGameService {



    ApiResponse<List<ApplyGameUserListDto>> getApplyParticipantList(Long gameId, Long userId, Pageable pageable);


    CheckResponse acceptGameUser(Long participantId, Long userId, Long GameId);

}
