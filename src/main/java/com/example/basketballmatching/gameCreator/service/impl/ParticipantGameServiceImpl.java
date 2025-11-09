package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.ParticipantGameService;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.APPLY;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class ParticipantGameServiceImpl implements ParticipantGameService {

    private final ParticipantGameRepository participantGameRepository;

    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ApplyGameUserListDto>> getApplyParticipantList(Long gameId, Long userId, Pageable pageable) {


        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

        // 지원자 목록 조회 (엔티티 기준)
        Page<ParticipantGameEntity> pages = participantGameRepository.
                findByParticipantGameStatusAndGameEntity_GameId(APPLY, gameEntity.getGameId(), pageable);


        // DTO 변환
        List<ApplyGameUserListDto> participantGameList = pages.stream().map(ApplyGameUserListDto::fromEntity).toList();

        return ApiResponse.of("경기 신청자 조회가 완료되었습니다.", participantGameList);
    }

    @Override
    @Transactional
    public CheckResponse acceptGameUser(Long participantId, Long userId, Long gameId) {

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

        boolean exists = participantGameRepository.existsByUserEntity_UserIdAndGameEntity_GameId(participantId, gameId);

        if (!exists) {
            throw new CustomException(NOT_APPLY_USER);
        }
        LocalDateTime now = LocalDateTime.now();
        if (gameEntity.getStartDateTime().isBefore(now)) {
            throw new CustomException(ALREADY_START_GAME);
        }

        if (gameEntity.getParticipantCount() >= gameEntity.getHeadCount()) {
            throw new CustomException(FULL_HEADCOUNT_GAME);
        }




        ParticipantGameEntity participantGameEntity = participantGameRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        if (participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
            throw new CustomException(ALREADY_ACCEPT_USER);
        }

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, now);

        gameEntity.increaseParticipantCount();

        gameRepository.save(gameEntity);
        participantGameRepository.save(participantGameEntity);




        return CheckResponse.of(true, "경기 수락이 완료되었습니다.");
    }
}
