package com.example.basketballmatching.gameCreator.service;

import com.example.basketballmatching.gameCreator.dto.AcceptGameUserListDto;
import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ParticipantGameService {



    CommonResponse<List<ApplyGameUserListDto>> getApplyParticipantList(Long gameId, Long userId, Pageable pageable);

    CommonResponse<List<AcceptGameUserListDto>> getAcceptParticipantList(Long gameId, Long userId, Pageable pageable);

    CheckResponse acceptGameUser(Long participantId, Long userId, Long gameId);


    CheckResponse rejectGameUser(Long participantId, Long userId, Long gameId);

    CheckResponse kickOutGameUser(Long participantId, Long userId, Long gameId);

    CheckResponse deleteGame(Long userId, Long gameId);


}
