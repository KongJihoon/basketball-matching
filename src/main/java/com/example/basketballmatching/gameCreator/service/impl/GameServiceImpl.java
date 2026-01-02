package com.example.basketballmatching.gameCreator.service.impl;


import com.example.basketballmatching.gameCreator.dto.*;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.cache.event.GameSearchCacheBumpEvent;
import com.example.basketballmatching.global.cache.version.GameSearchCacheVersionService;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.lock.RedissonLockExecutor;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class GameServiceImpl implements GameService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final ParticipantGameRepository participantGameRepository;
    private final GameSearchCacheVersionService versionService;
    private final GameSearchCacheService cacheService;

    private final ApplicationEventPublisher eventPublisher;

    private final RedissonLockExecutor lockExecutor;
    private final TransactionTemplate transactionTemplate;


    private static final String LOCK_PREFIX = "lock:game-create:";
    private static final long LOCK_WAIT_MS = 3000;
    private static final long LOCK_LEASE_MS = 10000;

    /**
     * 경기 생성
     */
    @Override
    public CommonResponse<CreateGameDto.Response> createGame(Long userId, CreateGameDto.Request request) {

        String lockKey = buildPlaceLockKey(request.getPlaceName(), request.getAddress());
        String redisKey = LOCK_PREFIX + lockKey;


        return lockExecutor.executeWithLock(redisKey, LOCK_WAIT_MS, LOCK_LEASE_MS, () ->
                transactionTemplate.execute(status -> {

                    log.info("[경기 생성 시작] userId = {} title = {}", userId, request.getTitle());



                    validateCreateGame(request);



                    validateOverLap(request);

                    UserEntity userEntity = getUser(userId);


                    GameEntity gameEntity = CreateGameDto.Request.toEntity(request, userEntity);

                    GameUserLevel gameUserLevel = userEntity.getGameUserLevel();

                    gameEntity.setGameUserLevel(gameUserLevel);

                    gameRepository.save(gameEntity);

                    ParticipantGameEntity participantGameEntity = new ParticipantGameEntity().toGameCreatorEntity(gameEntity, userEntity);

                    participantGameRepository.save(participantGameEntity);


                    eventPublisher.publishEvent(new GameSearchCacheBumpEvent());

                    log.info("[경기 생성 완료] gameId = {}", gameEntity.getGameId());


                    return CommonResponse.of("경기 생성이 완료되었습니다.", CreateGameDto.Response.fromDto(GameDto.fromEntity(gameEntity)));


                }));
    }


    /**
     * 경기 상세조회
     */
    @Override
    @Transactional(readOnly = true)
    public CommonResponse<GameDto> detailGame(Long gameId) {

        log.info("[경기 상세 조회 시작] gameId : {}", gameId);

        GameEntity gameEntity = getGame(gameId);


        GameDto gameDto = GameDto.fromEntity(gameEntity);

        log.info("[경기 상세조회 완료] gameId : {}", gameId);

        return CommonResponse.of("경기 상세조회에 성공하였습니다.", gameDto);
    }


    /**
     * 경기 검색 정렬
     */
    @Override
    @Transactional(readOnly = true)
    public CommonResponse<Page<SearchGameDto>> searchGame(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, Pageable pageable) {

        log.info("[경기 검색 정렬 시작] date : {}", date);

        long version = versionService.getVersion();


        GameSearchCacheDto<SearchGameDto> searchGameCached = cacheService.searchGameCached(
                version, date, cityName, matchFormat, fieldStatus, matchGenderType, gameStatus, pageable
        );

        if (searchGameCached.getContent().isEmpty()) {
            return CommonResponse.of("경기 검색결과가 없습니다.", new PageImpl<>(searchGameCached.getContent(), pageable, searchGameCached.getTotalElement()));
        }

        log.info("[경기 검색 정렬 완료] date : {}", date);

        return CommonResponse.of("경기 검색이 완료되었습니다.", new PageImpl<>(searchGameCached.getContent(), pageable, searchGameCached.getTotalElement()));
    }

    /**
     * 경기 수정
     */
    @Override
    @Transactional
    public CommonResponse<GameDto> editGame(EditGameDto request, Long gameId, Long userId) {

        log.info("[경기 수정 시작] loginId : {}, gameId : {}", userId, gameId);


        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        // 경기 수정 사항 유효성 검사
        validateEditGame(request, gameEntity, userEntity);


        gameEntity.editGameInfo(request);

        eventPublisher.publishEvent(new GameSearchCacheBumpEvent());

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


    private void validateCreateGame(CreateGameDto.Request request) {

        LocalDateTime now = LocalDateTime.now();

        if (request.getStartDateTime().isBefore(now)) {
            throw new CustomException(INVALID_GAME_TIME);
        }

        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new CustomException(INVALID_GAME_TIME);
        }

        long between = Duration.between(request.getStartDateTime(), request.getEndDateTime()).toMinutes();

        if (between < 60 || between > 120) {
            throw new CustomException(INVALID_GAME_TIME);
        }

        switch (request.getMatchFormat()) {
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

    private void validateOverLap(CreateGameDto.Request request) {

        boolean exists = gameRepository.existsBySamePlaceAtSameTime(
                request.getPlaceName(),
                request.getAddress(),
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        if (exists) {
            throw new CustomException(PLACE_SCHEDULE_OVERLAP);
        }

    }

    private String buildPlaceLockKey(String placeName, String address) {

        String pn = normalize(placeName).replace("|", " ");
        String ad = normalize(address).replace("|", " ");
        return pn + "|" + ad;


    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }

        return s.trim().replaceAll("\\s+", " ");
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
