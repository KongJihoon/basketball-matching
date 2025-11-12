package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.*;
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
                .participantGameStatus(APPLY)
                .gameEntity(gameEntity)
                .userEntity(userEntity)
                .build();

        participantGameRepository.save(entity);

        ApplyGameUserDto participantDto = ApplyGameUserDto.fromEntity(entity);


        log.info("[경기 참가 신청 완료] gameId : {}, participantId : {}", gameId, entity.getParticipantGameId());

        return ApiResponse.of("경기 신청이 완료되었습니다.", participantDto);
    }

    @Override
    @Transactional
    public CheckResponse cancelGame(Long userId, Long gameId) {

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndParticipantGameId(gameId, userId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));



        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(gameEntity.getStartDateTime().minusMinutes(30))) {
            throw new CustomException(NOT_ALLOWED_CANCEL);
        }

        if (participantGameEntity.getParticipantGameStatus().equals(CANCEL)) {
            throw new CustomException(ALREADY_CANCELED_USER);
        }

        if (!participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
            throw new CustomException(NOT_ACCEPT_USER);
        }



        participantGameEntity.setParticipantGameStatusAndCanceledDateTime(CANCEL, now);
        participantGameRepository.save(participantGameEntity);

        gameEntity.decreaseParticipantCount();
        gameRepository.save(gameEntity);




        return CheckResponse.of(true, "경기 취소가 완료되었습니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CurrentGameListDto>> getMyCurrentGameList(Long userId, Pageable pageable) {

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        Page<ParticipantGameEntity> participantGameEntities = participantGameRepository.findByUserEntity_UserIdAndParticipantGameStatusIn(userEntity.getUserId(), List.of(ACCEPT, APPLY), pageable);

        List<CurrentGameListDto> gameListDtos = participantGameEntities.stream()
                .filter(p -> p.getGameEntity().getStartDateTime().isAfter(now))
                .map(CurrentGameListDto::fromEntity)
                .toList();


        return ApiResponse.of("현재 예정된 게임 조회가 완료되었습니다.", gameListDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<LastGameListDto>> getMyLastGameList(Long userId, Pageable pageable) {

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        Page<ParticipantGameEntity> participantGameEntities = participantGameRepository.findByUserEntity_UserIdAndParticipantGameStatus(userEntity.getUserId(), ACCEPT, pageable);

        List<LastGameListDto> gameListDtos = participantGameEntities.stream()
                .filter(p -> p.getGameEntity().getStartDateTime().isBefore(now))
                .map(LastGameListDto::fromEntity)
                .toList();


        return ApiResponse.of("지난 게임 조회가 완료되었습니다.", gameListDtos);
    }



    private void validateParticipantInfo(UserEntity userEntity, GameEntity gameEntity) {

        LocalDateTime now = LocalDateTime.now();

        if (participantGameRepository.existsByParticipantGameIdAndGameEntity_GameId(userEntity.getUserId(), gameEntity.getGameId())) {
            throw new CustomException(ALREADY_APPLY_GAME_USER);
        }

        if (participantGameRepository.countByParticipantGameStatusAndGameEntity_GameId(
                APPLY, gameEntity.getGameId()
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
