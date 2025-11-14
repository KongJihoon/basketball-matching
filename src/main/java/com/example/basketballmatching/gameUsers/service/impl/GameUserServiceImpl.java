package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.EvaluatePlayerDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.gameUsers.entity.LevelEntity;
import com.example.basketballmatching.gameUsers.repository.LevelRepository;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
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
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserServiceImpl implements GameUserService {

    private final ParticipantGameRepository participantGameRepository;

    private final GameQueryRepository gameQueryRepository;

    private final LevelRepository levelRepository;

    private final UserRepository userRepository;

    private final GameRepository gameRepository;

    /**
     * 경기 참가 신청
     */
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

    /**
     * 경기 참가 취소
     */
    @Override
    @Transactional
    public CheckResponse cancelGame(Long userId, Long gameId) {

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
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

    /**
     * 현재 예정 경기 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CurrentGameListDto>> getMyCurrentGameList(Long userId, Pageable pageable) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        List<CurrentGameListDto> currentGameList = gameQueryRepository.getCurrentGameList(userId, pageable);

        return ApiResponse.of("현재 예정된 게임 조회가 완료되었습니다.", currentGameList);
    }

    /**
     * 지난 경기 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<LastGameListDto>> getMyLastGameList(Long userId, Pageable pageable) {

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));


        List<LastGameListDto> lastGameList = gameQueryRepository.getLastGameList(userId, pageable);


        return ApiResponse.of("지난 게임 조회가 완료되었습니다.", lastGameList);
    }


    /**
     * 경기 참가자 평가
     */
    @Override
    @Transactional
    public CheckResponse evaluatePlayer(Long gameId, Long evaluatorId, EvaluatePlayerDto request) {

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        if (gameEntity.getEndDateTime().isAfter(LocalDateTime.now())) {
            throw new CustomException(NOT_GAME_ENDED);
        }

        ParticipantGameEntity evaluator = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), evaluatorId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        ParticipantGameEntity receiver = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, request.getReceiverId())
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        if (evaluator.getParticipantGameId().equals(receiver.getParticipantGameId())) {
            throw new CustomException(CANNOT_EVALUATE_SELF);
        }

        boolean exists = levelRepository.existsByGameEntity_GameIdAndEvaluator_UserIdAndReceiver_UserId(gameId, evaluator.getUserEntity().getUserId(), receiver.getUserEntity().getUserId());

        if (exists) {
            throw new CustomException(ALREADY_EVALUATED);
        }

        if (request.getScore()< 1 || request.getScore() > 5) {
            throw new CustomException(INVALID_LEVEL_SCORE);
        }

        LevelEntity levelEntity = EvaluatePlayerDto.toEntity(evaluator.getUserEntity(), receiver.getUserEntity(), gameEntity, request.getScore());


        levelRepository.save(levelEntity);

        updatePlayerLevel(receiver.getUserEntity());

        userRepository.save(receiver.getUserEntity());

        return CheckResponse.of(true, "경기 참가자 평가를 완료하였습니다.");
    }

    // 최근 10경기 평균으로 Level측정
    private void updatePlayerLevel(UserEntity receiver) {

        List<GameEntity> recent10GamesByUser = gameQueryRepository.findRecent10GamesByUser(receiver);

        if (recent10GamesByUser.size() < 10) {
            receiver.updateLevel(GameUserLevel.NONE);
            return;
        }

        List<Double> gameAverages = new ArrayList<>();

        for (GameEntity gameEntity : recent10GamesByUser) {

            List<LevelEntity> evaluations = levelRepository.findByReceiverAndGameEntity(receiver, gameEntity);

            if (evaluations.isEmpty()) {
                continue;
            }

            double gameAverage = evaluations.stream()
                    .mapToInt(LevelEntity::getScore)
                    .average()
                    .orElse(0.0);


            gameAverages.add(gameAverage);
        }

        // 10경기 이상의 경기 후 평가를 받은 경기가 5개 미만일시 Level -> NONE
        if (gameAverages.size() < 5) {
            receiver.updateLevel(GameUserLevel.NONE);
            return;
        }

        double average = gameAverages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);


        GameUserLevel newLevel = GameUserLevel.fromScore(average);


        receiver.updateLevel(newLevel);

    }







    private void validateParticipantInfo(UserEntity userEntity, GameEntity gameEntity) {

        LocalDateTime now = LocalDateTime.now();

        if (Objects.equals(userEntity.getUserId(), gameEntity.getUserEntity().getUserId())) {
            throw new CustomException(NOT_APPLY_GAME_CREATOR);

        }

        if (participantGameRepository.existsByParticipantGameIdAndGameEntity_GameId(userEntity.getUserId(), gameEntity.getGameId())) {
            throw new CustomException(ALREADY_APPLY_GAME_USER);
        }

        if (participantGameRepository.countByParticipantGameStatusAndGameEntity_GameId(
                ACCEPT, gameEntity.getGameId()
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
