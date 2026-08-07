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
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.type.CityName;
import com.example.basketballmatching.game.type.MatchFormat;
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


        game.updateGame(request.title(), request.content(), request.headCount(), request.matchFormat(), request.matchGenderType());

        GameDetailResponse response = GameDetailResponse.fromEntity(game);

        log.info("[경기 수정 완료] gameId : {}", gameId);

        return response;
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






    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private GameEntity getGame(Long gameId) {
        return gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));
    }
}
