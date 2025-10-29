package com.example.basketballmatching.gameCreator.service.impl;


import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.dto.GameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.service.GameService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class GameServiceImpl implements GameService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;

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
