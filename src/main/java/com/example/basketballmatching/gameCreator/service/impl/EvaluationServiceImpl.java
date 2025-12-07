package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.EvaluationService;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.gameUsers.dto.EvaluatePlayerDto;
import com.example.basketballmatching.gameUsers.entity.LevelEntity;
import com.example.basketballmatching.gameUsers.repository.LevelRepository;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {


    private final GameRepository gameRepository;
    private final ParticipantGameRepository participantGameRepository;
    private final LevelRepository levelRepository;
    private final UserLevelService userLevelService;

    @Override
    @Transactional
    public CheckResponse evaluatePlayer(Long gameId, Long evaluatorUserId, EvaluatePlayerDto evaluatePlayerDto) {

        GameEntity gameEntity = getGame(gameId);

        validateGameEnded(gameEntity);

        ParticipantGameEntity evaluator = getParticipantGame(evaluatorUserId, gameId);
        ParticipantGameEntity receiver = getParticipantGame(evaluatePlayerDto.getReceiverId(), gameId);

        validateNotSelf(evaluator, receiver);
        validateScore(evaluatePlayerDto.getScore());
        validateNotDuplicated(gameId, evaluator.getUserEntity().getUserId(), receiver.getUserEntity().getUserId());

        validateOnlyAcceptedUser(evaluator, receiver);

        LevelEntity levelEntity = EvaluatePlayerDto.toEntity(
                evaluator.getUserEntity(),
                receiver.getUserEntity(),
                gameEntity,
                evaluatePlayerDto.getScore()
        );

        levelRepository.save(levelEntity);

        userLevelService.recalculateLevel(receiver.getUserEntity().getUserId());

        return CheckResponse.of(true, "경기 참가자 평가를 완료하였습니다.");
    }

    private static void validateOnlyAcceptedUser(ParticipantGameEntity evaluator, ParticipantGameEntity receiver) {
        if (!evaluator.getParticipantGameStatus().equals(ACCEPT) || !receiver.getParticipantGameStatus().equals(ACCEPT)) {

            throw new CustomException(ONLY_EVALUATE_ACCEPT_USER);
        }
    }


    private GameEntity getGame(Long gameId) {
        return gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));
    }

    private ParticipantGameEntity getParticipantGame(Long userId, Long gameId) {
        return participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));
    }

    private void validateGameEnded(GameEntity gameEntity) {

        if (gameEntity.getEndDateTime().isAfter(LocalDateTime.now())) {
            throw new CustomException(NOT_GAME_ENDED);
        }

    }

    private void validateNotSelf(ParticipantGameEntity evaluator, ParticipantGameEntity receiver) {

        if (evaluator.getParticipantGameId().equals(receiver.getParticipantGameId())) {
            throw new CustomException(CANNOT_EVALUATE_SELF);
        }

    }

    private void validateScore(int score) {
        if (score < 1 || score > 5) {
            throw new CustomException(INVALID_LEVEL_SCORE);
        }
    }

    private void validateNotDuplicated(Long gameId, Long evaluatorUserId, Long receiverUserId) {

        boolean exists = levelRepository.existsByGameEntity_GameIdAndEvaluator_UserIdAndReceiver_UserId(gameId, evaluatorUserId, receiverUserId);

        if (exists) {
            throw new CustomException(ALREADY_EVALUATED);
        }


    }
}
