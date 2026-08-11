package com.example.basketballmatching.game.service;

import com.example.basketballmatching.blackList.repository.BlackListRepository;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.response.GameApplyResponse;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameParticipantService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final ParticipantGameRepository participantGameRepository;
    private final BlackListRepository blackListRepository;
    private final Clock clock;

    @Transactional
    public GameApplyResponse apply(Long gameId, Long userId) {

        log.info("[경기 참가 신청 시작] gameId={}, userId={}", gameId, userId);

        LocalDateTime now = LocalDateTime.now(clock);

        UserEntity applicant = getActiveUser(userId);

        validateNotBlackList(userId);

        GameEntity game = getActiveGame(gameId);

        ParticipantGameEntity existingParticipation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElse(null);

        validateExistingParticipation(existingParticipation);

        game.validateApply(applicant, now);

        ParticipantGameEntity participation = applyOrReapply(game, applicant, existingParticipation, now);

        log.info("[경기 신청 완료] gameId={}, userId={}", gameId, userId);

        return GameApplyResponse.fromEntity(participation);
    }

    @Transactional
    public void cancelApply(Long gameId, Long userId) {

        log.info("[경기 참가 취소 시작] gameId={}, userId={}", gameId, userId);

        LocalDateTime now = LocalDateTime.now(clock);

        getActiveUser(userId);

        GameEntity game = getActiveGame(gameId);

        ParticipantGameEntity participation = getParticipation(gameId, userId);


        game.validateParticipantCancel(now);

        participation.cancelApply(now);

        log.info("[경기 참가 신청 취소 완료] gameId={}, userId={}", gameId, userId);


    }

    private ParticipantGameEntity getParticipation(Long gameId, Long userId) {
        return participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(
                gameId, userId
        ).orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));
    }

    private ParticipantGameEntity applyOrReapply(GameEntity game, UserEntity applicant, ParticipantGameEntity existingParticipation, LocalDateTime now) {

        if (existingParticipation == null) {
            ParticipantGameEntity participation = ParticipantGameEntity.createApply(
                    game, applicant, now
            );

            return participantGameRepository.save(participation);
        }

        existingParticipation.reapply(now);

        return existingParticipation;

    }

    private void validateExistingParticipation(ParticipantGameEntity participation) {

        if (participation == null || participation.getParticipantGameStatus() == CANCEL) {
            return;
        }

        ParticipantGameStatus status = participation.getParticipantGameStatus();

        switch (status) {
            case APPLY -> {
                throw new CustomException(ALREADY_APPLY_GAME_USER);
            }
            case ACCEPT -> {
                throw new CustomException(ALREADY_ACCEPT_USER);
            }
            case KICKOUT -> {
                throw new CustomException(NOT_APPLY_KICKOUT_USER);
            }
            case REJECT, DELETE -> {
                throw new CustomException(ALREADY_FINAL_STATUS);
            }
        }

    }

    private UserEntity getActiveUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private GameEntity getActiveGame(Long gameId) {
        return gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));
    }

    private void validateNotBlackList(Long userId) {
        if (blackListRepository.existsByUserEntity_UserId(userId)) {
            throw new CustomException(BLACKLIST_USER);
        }
    }
}
