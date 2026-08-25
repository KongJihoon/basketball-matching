package com.example.basketballmatching.game.service;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlackListGameService {
    private final GameQueryRepository gameQueryRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public BlackListGameResultDto cleanup(Long userId, LocalDateTime bannedAt) {

        List<GameCancelNotificationDto> notices = cancelCreatedFutureGames(userId, bannedAt);

        cancelOtherFutureParticipation(userId, bannedAt);

        return new BlackListGameResultDto(notices);

    }

    private List<GameCancelNotificationDto> cancelCreatedFutureGames(Long userId, LocalDateTime bannedAt) {

        List<GameEntity> games = gameQueryRepository.findFutureGamesCreatedBy(userId, bannedAt);

        if (games.isEmpty()) {
            return List.of();
        }

        List<Long> gameIds = games.stream()
                .map(GameEntity::getGameId)
                .toList();

        List<ParticipantGameEntity> participants = gameQueryRepository.findActiveParticipantsByGameIds(gameIds);

        List<GameCancelNotificationDto> notices = new ArrayList<>();

        for (ParticipantGameEntity participant : participants) {

            participant.delete(bannedAt);

            Long participantUserId = participant.getUserEntity().getUserId();

            if (!participantUserId.equals(userId)) {
                notices.add(new GameCancelNotificationDto(participantUserId, participant.getGameEntity().getTitle()));
            }

        }

        games.forEach(
                game -> game.cancelByCreatorUnavailable(bannedAt)
        );

        return notices;

    }

    private void cancelOtherFutureParticipation(Long userId, LocalDateTime bannedAt) {

        List<ParticipantGameEntity> participations = gameQueryRepository.findFutureParticipationExcludingCreatedGames(userId, bannedAt);

        participations.forEach(
                participation -> participation.kickout(bannedAt)
        );
    }

}
