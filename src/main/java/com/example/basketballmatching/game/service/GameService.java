package com.example.basketballmatching.game.service;


import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.request.GameListCondition;
import com.example.basketballmatching.game.dto.request.UpdateGameRequest;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.dto.response.GameDetailResponse;
import com.example.basketballmatching.game.dto.response.GameListResponse;
import com.example.basketballmatching.game.event.GameCreateEvent;
import com.example.basketballmatching.game.event.UpdateGameEvent;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.type.CityName;
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
import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class GameService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final ParticipantGameRepository participantGameRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final Clock clock;
    private final GameQueryRepository gameQueryRepository;


    /**
     * 경기 생성
     */
    @Transactional
    public CreateGameResponse createGame(Long userId, CreateGameRequest request) {

        log.info("[경기 생성 시작] userId = {} title = {}", userId, request.title());


        LocalDateTime now = LocalDateTime.now(clock);


        UserEntity creator = getUser(userId);

        CityName cityName = CityName.fromAddress(request.address());

        GameEntity game = GameEntity.create(
                request.title(),
                request.content(),
                request.headCount(),
                request.fieldStatus(),
                request.matchFormat(),
                request.matchGenderType(),
                request.startDateTime(),
                request.endDateTime(),
                request.placeName(),
                request.address(),
                cityName,
                request.latitude(),
                request.longitude(),
                creator,
                now

        );

        validateOverLap(request);

        GameEntity savedGame = gameRepository.save(game);


        ParticipantGameEntity creatorParticipation = ParticipantGameEntity.createCreator(savedGame, creator, now);

        participantGameRepository.save(creatorParticipation);

        eventPublisher.publishEvent(new GameCreateEvent(savedGame.getGameId(), creator.getUserId(), savedGame.getTitle()));


        log.info("[경기 생성 완료] gameId = {}", savedGame.getGameId());


        return CreateGameResponse.fromEntity(savedGame);

    }

    /**
     * 경기 상세조회
     */
    @Transactional(readOnly = true)
    public GameDetailResponse getGameDetail(Long gameId) {

        log.info("[경기 상세 조회 시작] gameId : {}", gameId);

        GameEntity gameEntity = getGame(gameId);


        GameDetailResponse response = GameDetailResponse.fromEntity(gameEntity);

        log.info("[경기 상세조회 완료] gameId : {}", gameId);

        return response;
    }


    /**
     * 경기 검색 정렬
     */
    @Transactional(readOnly = true)
    public Page<GameListResponse> getGames(GameListCondition condition, Pageable pageable) {

        LocalDateTime now = LocalDateTime.now(clock);

        log.info(
                "[경기 목록 조회 시작] date={}, keyword={}, sortType={}, page={}, size={}",
                condition.date(),
                condition.keyword(),
                condition.sortType(),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<GameListResponse> response = gameQueryRepository.findGames(condition, pageable, now)
                .map(GameListResponse::fromEntity);




        log.info("[경기 검색 정렬 완료] totalElements={}", response.getTotalElements());


        return response;
    }

    /**
     * 경기 수정
     */
    @Transactional
    public GameDetailResponse updateGame(UpdateGameRequest request, Long gameId, Long userId) {

        log.info("[경기 수정 시작] loginId : {}, gameId : {}", userId, gameId);

        LocalDateTime now = LocalDateTime.now(clock);


        GameEntity game = getGame(gameId);



        // 경기 수정 사항 유효성 검사
        validateUpdateGame(game, userId, now);

        validateUpdateRequest(request);

        validateUpdateScheduleOverlap(game, request);

        boolean actuallyChanged = isActuallyChanged(game, request);

        game.updateGame(request.title(), request.content(), request.headCount(), request.matchFormat(), request.matchGenderType(), request.startDateTime(), request.endDateTime(), now);

        if (actuallyChanged) {
            publishUpdateGameEvent(game, userId);
        }

        GameDetailResponse response = GameDetailResponse.fromEntity(game);

        log.info("[경기 수정 완료] gameId : {}", gameId);

        return response;
    }

    private void validateUpdateRequest(UpdateGameRequest request) {

        if (!request.hasAnyChange()) {
            throw new CustomException(NO_GAME_UPDATE_FIELDS);
        }

        if (request.hasIncompleteSchedule()) {
            throw new CustomException(GAME_SCHEDULE_REQUIRED_TOGETHER);
        }
    }

    private void validateUpdateScheduleOverlap(GameEntity game, UpdateGameRequest request) {

        if (!request.hasScheduleInput()) {
            return;
        }

        boolean scheduleChanged = !request.startDateTime().equals(game.getStartDateTime())
                || !request.endDateTime().equals(game.getEndDateTime());

        if (!scheduleChanged) {
            return;
        }

        boolean exists = gameRepository.existsOverlappingGameExcludeCurrent(
                game.getGameId(),
                game.getPlaceName(),
                game.getAddress(),
                request.startDateTime(),
                request.endDateTime()
        );

        if (exists) {
            throw new CustomException(PLACE_SCHEDULE_OVERLAP);
        }

    }

    private void validateUpdateGame(
            GameEntity game,
            Long userId,
            LocalDateTime now
    ) {
        if (!game.getUserEntity()
                .getUserId()
                .equals(userId)) {
            throw new CustomException(
                    NOT_GAME_CREATOR
            );
        }

        LocalDateTime limitUpdateTime =
                game.getStartDateTime()
                        .minusDays(1);

        if (!now.isBefore(limitUpdateTime)) {
            throw new CustomException(
                    UPDATE_NOT_ALLOWED_AT_THIS_TIME
            );
        }
    }




    private void validateOverLap(CreateGameRequest request) {

        boolean exists = gameRepository.existsBySamePlaceAtSameTime(
                request.placeName(),
                request.address(),
                request.startDateTime(),
                request.endDateTime()
        );

        if (exists) {
            throw new CustomException(PLACE_SCHEDULE_OVERLAP);
        }

    }

    private boolean isActuallyChanged(GameEntity game, UpdateGameRequest request) {

        boolean titleChanged = request.title() != null && !Objects.equals(request.title(), game.getTitle());

        boolean contentChanged = request.content() != null && !Objects.equals(request.content(), game.getContent());

        boolean headCountChanged = request.headCount() != null && request.headCount() != game.getHeadCount();

        boolean matchFormatChanged = request.matchFormat() != null && request.matchFormat() != game.getMatchFormat();

        boolean genderChanged = request.matchGenderType() != null && request.matchGenderType() != game.getMatchGenderType();

        boolean startDateTimeChanged = request.startDateTime() != null && !Objects.equals(request.startDateTime(), game.getStartDateTime());

        boolean endDateTimeChanged = request.endDateTime() != null && !Objects.equals(request.endDateTime(), game.getEndDateTime());

        return titleChanged
                || contentChanged
                || headCountChanged
                || matchFormatChanged
                || genderChanged
                || startDateTimeChanged
                || endDateTimeChanged;

    }

    private void publishUpdateGameEvent(GameEntity game, Long creatorId) {

        List<Long> receiverIds = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(
                        List.of(ParticipantGameStatus.ACCEPT, ParticipantGameStatus.APPLY), game.getGameId()
                ).stream()
                .map(participant -> participant.getUserEntity().getUserId())
                .filter(receiverId -> !receiverId.equals(creatorId))
                .distinct()
                .toList();

        if (receiverIds.isEmpty()) {
            return;
        }


        eventPublisher.publishEvent(
                new UpdateGameEvent(
                        game.getGameId(),
                        game.getTitle(),
                        game.getStartDateTime(),
                        game.getEndDateTime(),
                        receiverIds
                )
        );

    }






    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private GameEntity getGame(Long gameId) {
        return gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));
    }
}
