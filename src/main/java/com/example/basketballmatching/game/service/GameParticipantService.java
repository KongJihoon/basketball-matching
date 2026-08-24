package com.example.basketballmatching.game.service;

import com.example.basketballmatching.blackList.repository.BlackListRepository;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.response.GameParticipantListResponse;
import com.example.basketballmatching.game.dto.response.GameParticipantResponse;
import com.example.basketballmatching.game.event.GameParticipantKickedOutEvent;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

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
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public GameParticipantResponse join(Long gameId, Long userId) {

        log.info("[경기 참가 신청 시작] gameId={}, userId={}", gameId, userId);

        LocalDateTime joinedAt = LocalDateTime.now(clock);

        UserEntity participant = getActiveUser(userId);

        validateNotBlackList(userId);

        GameEntity game = getActiveGame(gameId);

        ParticipantGameEntity existingParticipation = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElse(null);

        validateExistingParticipation(existingParticipation);

        game.validateJoin(participant, joinedAt);

        ParticipantGameEntity participation = joinOrRejoin(game, participant, existingParticipation, joinedAt);

        log.info("[경기 신청 완료] gameId={}, userId={}", gameId, userId);

        return GameParticipantResponse.fromEntity(participation);
    }

    @Transactional
    public void cancelParticipation(Long gameId, Long userId) {

        log.info("[경기 참가 취소 시작] gameId={}, userId={}", gameId, userId);

        LocalDateTime canceledAt = LocalDateTime.now(clock);

        UserEntity participant = getActiveUser(userId);

        GameEntity game = getActiveGame(gameId);

        ParticipantGameEntity participation = getParticipation(gameId, userId);


        game.validateParticipantCancel(participant, canceledAt);

        participation.cancelParticipation(canceledAt);

        log.info("[경기 참가 신청 취소 완료] gameId={}, userId={}", gameId, userId);


    }

    @Transactional(readOnly = true)
    public Page<GameParticipantListResponse> getParticipants(Long gameId, Long userId, Pageable pageable) {

        log.info("[경기 참가자 목록 조회 시작] gameId={}, userId={}", gameId, userId);

       getActiveUser(userId);

        GameEntity game = getActiveGame(gameId);

        validateParticipantListAccess(gameId, userId);


        Long creatorId = game.getUserEntity().getUserId();

        Page<GameParticipantListResponse> participants = participantGameRepository.findByGameEntity_GameIdAndParticipantGameStatus(
                gameId, ACCEPT, pageable
        ).map(participation -> GameParticipantListResponse.fromEntity(participation, creatorId));


        log.info("[경기 참가자 목록 조회 완료] gameId={}, totalElements={}", gameId, participants.getTotalElements());


        return participants;
    }

    @Transactional
    public void kickoutParticipant(Long gameId, Long participantId, Long requesterId) {

        log.info("[경기 참가자 강퇴 시작] gameId={}, participantId={}, requesterId={}", gameId, participantId, requesterId);

        LocalDateTime kickoutAt = LocalDateTime.now(clock);


        UserEntity requester = getActiveUser(requesterId);

        GameEntity game = getActiveGame(gameId);

        validateGameCreator(game, requester);

        ParticipantGameEntity participation = getParticipant(gameId, participantId);

        UserEntity participant = participation.getUserEntity();

        game.validateParticipantKickout(participant, kickoutAt);

        participation.kickout(kickoutAt);

        log.info("[경기 참가자 강퇴 완료] gameId={}, participantId={}, requesterId={}", gameId, participantId, requesterId);

        eventPublisher.publishEvent(new GameParticipantKickedOutEvent(
                game.getGameId(),game.getTitle(), participant.getUserId()
        ));


    }

    private void validateParticipantListAccess(Long gameId, Long userId) {

        boolean acceptedParticipant = participantGameRepository.existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
                gameId, userId, ACCEPT
        );

        if (!acceptedParticipant) {
            throw new CustomException(PARTICIPANT_LIST_ACCESS_DENIED);
        }

    }

    private void validateGameCreator(GameEntity game, UserEntity requester) {
        if (!Objects.equals(requester.getUserId(), game.getUserEntity().getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

    }

    private ParticipantGameEntity getParticipation(Long gameId, Long userId) {
        return participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(
                gameId, userId
        ).orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));
    }

    private ParticipantGameEntity joinOrRejoin(GameEntity game, UserEntity participant, ParticipantGameEntity existingParticipation, LocalDateTime joinedAt) {

        if (existingParticipation == null) {
            ParticipantGameEntity participation = ParticipantGameEntity.createParticipation(
                    game, participant, joinedAt
            );

            return participantGameRepository.save(participation);
        }

        existingParticipation.join(joinedAt);

        return existingParticipation;

    }

    private void validateExistingParticipation(ParticipantGameEntity participation) {

        if (participation == null) {
            return;
        }

        ParticipantGameStatus status = participation.getParticipantGameStatus();

        switch (status) {
            case CANCEL -> {
            }
            case ACCEPT -> {
                throw new CustomException(ALREADY_ACCEPT_USER);
            }
            case KICKOUT -> {
                throw new CustomException(NOT_APPLY_KICKOUT_USER);
            }
            case DELETE -> {
                throw new CustomException(ALREADY_FINAL_STATUS);
            }
        }

    }

    private ParticipantGameEntity getParticipant(Long gameId, Long participantId) {

        return participantGameRepository.findByParticipantGameIdAndGameEntity_GameId(participantId, gameId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

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
