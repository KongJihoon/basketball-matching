package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserServiceImpl implements GameUserService {

    private final ParticipantGameRepository participantGameRepository;

    private final UserRepository userRepository;

    private final GameRepository gameRepository;

    @Override
    @Transactional
    public ApiResponse<ApplyGameUserDto> applyGame(Long gameId, Long userId) {
        log.info("[경기 참가 신청 시작] gameId : {} userId : {}", gameId, userId);


        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        validateParticipantInfo(userEntity, gameEntity);

        ParticipantGameEntity entity = ParticipantGameEntity.builder()
                .participantGameStatus(ParticipantGameStatus.APPLY)
                .gameEntity(gameEntity)
                .userEntity(userEntity)
                .build();

        participantGameRepository.save(entity);

        ApplyGameUserDto participantDto = ApplyGameUserDto.fromEntity(entity);


        log.info("[경기 참가 신청 완료] gameId : {}, participantId : {}", gameId, entity.getParticipantGameId());

        return ApiResponse.of("경기 신청이 완료되었습니다.", participantDto);
    }

    private void validateParticipantInfo(UserEntity userEntity, GameEntity gameEntity) {

        LocalDateTime now = LocalDateTime.now();

        if (participantGameRepository.existsByUserEntity_UserIdAndGameEntity_GameId(userEntity.getUserId(), gameEntity.getGameId())) {
            throw new CustomException(ALREADY_APPLY_GAME_USER);
        }

        if (participantGameRepository.countByParticipantGameStatusAndGameEntity_GameId(
                ParticipantGameStatus.APPLY, gameEntity.getGameId()
        ) >= gameEntity.getHeadCount()) {
            throw new CustomException(FULL_HEADCOUNT_GAME);
        }

        if (now.isAfter(gameEntity.getStartDateTime().minusMinutes(30))) {
            throw new CustomException(NOT_ALLOWED_TO_JOIN);
        }

        if (gameEntity.getMatchGenderType().equals(MatchGenderType.FEMALE_ONLY) &&
        userEntity.getGenderType().equals(GenderType.MALE)) {
            throw new CustomException(ONLY_FEMALE_GAME);
        }


        if (gameEntity.getMatchGenderType().equals(MatchGenderType.MALE_ONLY) &&
                userEntity.getGenderType().equals(GenderType.FEMALE)) {
            throw new CustomException(ONLY_MALE_GAME);
        }



    }
}
