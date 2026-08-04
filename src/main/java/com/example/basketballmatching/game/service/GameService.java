package com.example.basketballmatching.game.service;


import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.dto.EditGameDto;
import com.example.basketballmatching.game.dto.GameDto;
import com.example.basketballmatching.game.dto.request.CreateGameRequest;
import com.example.basketballmatching.game.dto.request.GameListCondition;
import com.example.basketballmatching.game.dto.response.CreateGameResponse;
import com.example.basketballmatching.game.dto.response.GameDetailResponse;
import com.example.basketballmatching.game.dto.response.GameListResponse;
import com.example.basketballmatching.game.event.GameCreateEvent;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.type.CityName;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.global.dto.CommonResponse;
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
    public CommonResponse<GameDto> editGame(EditGameDto request, Long gameId, Long userId) {

        log.info("[경기 수정 시작] loginId : {}, gameId : {}", userId, gameId);


        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        // 경기 수정 사항 유효성 검사
        validateEditGame(request, gameEntity, userEntity);


        gameEntity.editGameInfo(request.getTitle(), request.getContent(), request.getHeadCount(), request.getMatchFormat(), request.getMatchGenderType());


        log.info("[경기 수정 완료] gameId : {}", gameId);

        return CommonResponse.of("경기 수정이 완료되었습니다.", GameDto.fromEntity(gameEntity));
    }

    private void validateEditGame(EditGameDto request, GameEntity gameEntity, UserEntity userEntity) {
        if (!gameEntity.getUserEntity().getUserId().equals(userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

        if (request.getMatchFormat() != null && request.getHeadCount() == 0) {
            throw new CustomException(UPDATE_GAME_HEAD_COUNT);
        }


        if (request.getHeadCount() > 0) {

            if (request.getHeadCount() < gameEntity.getParticipantCount()) {
                throw new CustomException(INVALID_HEADCOUNT);
            }


            MatchFormat matchFormat = (request.getMatchFormat() != null) ? request.getMatchFormat() : gameEntity.getMatchFormat();

            switch (matchFormat) {
                case THREE_ON_THREE -> {

                    if (request.getHeadCount() < 6 || request.getHeadCount() > 9) {
                        throw new CustomException(INVALID_HEADCOUNT);
                    }

                }
                case FIVE_ON_FIVE -> {
                    if (request.getHeadCount() < 10 || request.getHeadCount() > 15) {
                        throw new CustomException(INVALID_HEADCOUNT);
                    }
                }

            }


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
