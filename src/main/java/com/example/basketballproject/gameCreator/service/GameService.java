package com.example.basketballproject.gameCreator.service;

import com.example.basketballproject.auth.security.JwtTokenExtract;
import com.example.basketballproject.gameCreator.dto.CreateGameDto;
import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.repository.GameRepository;
import com.example.basketballproject.gameCreator.util.Util;
import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.global.exception.ErrorCode;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.example.basketballproject.global.exception.ErrorCode.ALREADY_GAME_CREATED;
import static com.example.basketballproject.global.exception.ErrorCode.NOT_AFTER_THIRTY_MINUTE;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {


    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    private final JwtTokenExtract jwtTokenExtract;

    public CreateGameDto.Response createGame(CreateGameDto.Request request) {

        log.info("게임 생성 시작");

        UserEntity userEntity = jwtTokenExtract.currentUser();

        UserEntity currentUser = userRepository.findByLoginIdAndDeletedDateTimeNull(userEntity.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        LocalDateTime startDateTime = request.getStartDateTime();

        LocalDateTime beforeDateTime = startDateTime.minusMinutes(30);

        LocalDateTime afterDateTime = startDateTime.plusMinutes(30);

        LocalDateTime now = LocalDateTime.now();

        Long aroundGameCount = gameRepository.countByStartDateTimeBetweenAndAddressAndDeletedDateTimeNull(
                beforeDateTime, afterDateTime, request.getAddress()
        ).orElse(0L);


        if (aroundGameCount == 0) {

            if (beforeDateTime.isBefore(now)) {
                throw new CustomException(NOT_AFTER_THIRTY_MINUTE);
            }

        } else {
            throw new CustomException(ALREADY_GAME_CREATED);
        }

        GameEntity gameEntity = CreateGameDto.Request.toEntity(request, currentUser);

        gameEntity.setCityName(Util.getCityName(request.getAddress()));

        gameRepository.save(gameEntity);

        log.info("경기 생성 종료");

        return CreateGameDto.Response.toDto(gameEntity);

    }

}
