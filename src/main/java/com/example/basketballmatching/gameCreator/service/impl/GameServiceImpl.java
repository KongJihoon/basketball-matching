package com.example.basketballmatching.gameCreator.service.impl;


import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.dto.GameDto;
import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GameQueryRepository gameQueryRepository;

    @Override
    @Transactional
    public ApiResponse<CreateGameDto.Response> createGame(Long userId, CreateGameDto.Request request) {

        log.info("[경기 생성 시작] userId = {} title = {}", userId, request.getTitle());

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        validateCreateGame(request);

        GameEntity gameEntity = CreateGameDto.Request.toEntity(request, userEntity);

        gameRepository.save(gameEntity);

        log.info("[경기 생성 완료] gameId = {}", gameEntity.getGameId());


        return ApiResponse.of("경기 생성이 완료되었습니다.", CreateGameDto.Response.fromDto(GameDto.fromEntity(gameEntity)));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<GameDto> detailGame(Long gameId) {

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));


        GameDto gameDto = GameDto.fromEntity(gameEntity);

        return ApiResponse.of("경기 상세조회에 성공하였습니다.", gameDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<SearchGameDto>> searchGame(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, Pageable pageable) {

        Page<SearchGameDto> responses = gameQueryRepository.searchByKeyword(date, cityName, matchFormat, fieldStatus,matchGenderType, gameStatus, pageable);


        if (responses.isEmpty()) {
            return ApiResponse.of("경기 검색결과가 없습니다.", responses);
        }

        return ApiResponse.of("경기 검색이 완료되었습니다.", responses);
    }

    @Override
    @Transactional
    public ApiResponse<GameDto> editGame(EditGameDto request, Long gameId, Long userId) {

        log.info("[경기 수정 시작] loginId : {}, gameId : {}", userId, gameId);


        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!gameEntity.getUserEntity().getUserId().equals(userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

        if(request.getMatchFormat() != null && request.getHeadCount() == 0) {
            throw new CustomException(UPDATE_GAME_HEAD_COUNT);
        }



        if (request.getHeadCount() > 0) {

            if (request.getHeadCount() < gameEntity.getParticipantCount()) {
                throw new CustomException(INVALID_HEADCOUNT);
            }

            if (request.getMatchFormat() != null) {
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
        }



        gameEntity.editGameInfo(request);

        gameRepository.save(gameEntity);

        log.info("[경기 수정 완료] gameId : {}", gameId);

        return ApiResponse.of("경기 수정이 완료되었습니다.", GameDto.fromEntity(gameEntity));
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
}
