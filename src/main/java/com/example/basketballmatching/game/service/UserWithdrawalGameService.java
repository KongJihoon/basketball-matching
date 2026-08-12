package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.dto.UserWithdrawalGameResultDto;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalGameService {

    private final GameQueryRepository gameQueryRepository;

    @Transactional
    public UserWithdrawalGameResultDto cleanup(
            Long userId,
            LocalDateTime withdrawnAt
    ) {

        List<GameCancelNotificationDto> notices = cancelCreatedFutureGames(userId, withdrawnAt);

        cancelOtherFutureParticipation(userId, withdrawnAt);


        return new UserWithdrawalGameResultDto(notices);
    }

    private List<GameCancelNotificationDto> cancelCreatedFutureGames(Long userId, LocalDateTime withdrawnAt) {

        List<GameEntity> games = gameQueryRepository.findFutureGamesCreatedBy(userId, withdrawnAt);

        if (games.isEmpty()) {
            return List.of();
        }

        List<Long> gameIds = games.stream()
                .map(GameEntity::getGameId)
                .toList();

        List<ParticipantGameEntity> participants = gameQueryRepository.findActiveParticipantsByGameIds(gameIds);

        List<GameCancelNotificationDto> notices = new ArrayList<>();

        for (ParticipantGameEntity participant : participants) {

            participant.delete(withdrawnAt);

            Long participantUserId = participant.getUserEntity().getUserId();

            if (!participantUserId.equals(userId)) {
                notices.add(new GameCancelNotificationDto(participantUserId, participant.getGameEntity().getTitle()));
            }
        }

        games.forEach(
                game -> game.cancelByCreatorWithdrawal(
                        withdrawnAt
                )
        );

        return notices;
    }

    private void cancelOtherFutureParticipation(Long userId, LocalDateTime withdrawnAt) {
        List<ParticipantGameEntity> participations = gameQueryRepository.findFutureParticipationExcludingCreatedGames(userId, withdrawnAt);

        for (ParticipantGameEntity participation : participations) {
            ParticipantGameStatus status = participation.getParticipantGameStatus();

            switch (status) {
                case ACCEPT -> participation.kickout(withdrawnAt);
                default -> throw new CustomException(INVALID_STATUS_TRANSITION);
            }
        }
    }


}
