package com.example.basketballproject.gameCreator.service;

import com.example.basketballproject.auth.security.JwtTokenExtract;
import com.example.basketballproject.gameCreator.dto.CreateGameDto;
import com.example.basketballproject.gameCreator.dto.DeleteGameDto;
import com.example.basketballproject.gameCreator.dto.UpdateGameDto;
import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.repository.GameRepository;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.util.Util;
import com.example.basketballproject.gameUser.repository.ParticipantRepository;
import com.example.basketballproject.gameUser.type.ParticipantStatus;
import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.global.exception.ErrorCode;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.repository.UserRepository;
import com.example.basketballproject.user.type.GenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.example.basketballproject.gameCreator.type.Gender.FEMALEONLY;
import static com.example.basketballproject.gameCreator.type.Gender.MALEONLY;
import static com.example.basketballproject.gameUser.type.ParticipantStatus.ACCEPT;
import static com.example.basketballproject.global.exception.ErrorCode.*;
import static com.example.basketballproject.user.type.GenderType.FEMALE;
import static com.example.basketballproject.user.type.GenderType.MALE;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {


    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    private final ParticipantRepository participantRepository;

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

        gameEntity.setCreatedDateTime(now);

        gameRepository.save(gameEntity);

        log.info("경기 생성 종료");

        return CreateGameDto.Response.toDto(gameEntity);

    }

    public UpdateGameDto.Response updateGame(UpdateGameDto.Request request) {

        log.info("경기 수정 시작");

        String loginId = jwtTokenExtract.currentUser().getLoginId();

        GameEntity gameEntity = gameRepository.findByUserEntity_LoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(NOT_GAME_CREATOR));

        LocalDateTime startDateTime = request.getStartDateTime();

        LocalDateTime beforeDateTime = startDateTime.minusMinutes(30);

        LocalDateTime plusDateTime = startDateTime.plusMinutes(30);

        LocalDateTime now = LocalDateTime.now();

        Long aroundGameCount = gameRepository.countByStartDateTimeBetweenAndAddressAndDeletedDateTimeNull(beforeDateTime, plusDateTime, request.getAddress())
                .orElse(0L);


        if (aroundGameCount == 0) {

            if (beforeDateTime.isBefore(now)) {

                throw new CustomException(NOT_AFTER_THIRTY_MINUTE);
            }
        } else {
            throw new CustomException(ALREADY_GAME_CREATED);
        }

        Long headCount = participantRepository.countByStatusAndGameEntityGameId(ACCEPT, gameEntity.getGameId())
                .orElse(0L);

        if (request.getHeadCount() < headCount) {
            throw new CustomException(NOT_UPDATED_HEADCOUNT);
        }

        Gender gender = request.getGender();

        if (gender == MALEONLY || gender == FEMALEONLY) {

            GenderType queryGender = gender == MALEONLY ? FEMALE : MALE;

            Long count = participantRepository.countByStatusAndGameEntityGameIdAndUserEntityGenderType(
                            ACCEPT, gameEntity.getGameId(), queryGender)
                    .orElse(0L);

            if (count >= 1) {
                if (gender == MALEONLY) {
                    throw new CustomException(NOT_UPDATE_MAN);
                } else {
                    throw new CustomException(NOT_UPDATE_FEMALE);
                }
            }


        }

        GameEntity game = GameEntity.builder()
                .gameId(gameEntity.getGameId())
                .title(request.getTitle())
                .content(request.getContent())
                .headCount(request.getHeadCount())
                .fieldState(request.getFieldState())
                .address(request.getAddress())
                .startDateTime(request.getStartDateTime())
                .createdDateTime(gameEntity.getCreatedDateTime())
                .gender(request.getGender())
                .cityName(Util.getCityName(request.getAddress()))
                .matchFormat(request.getMatchFormat())
                .userEntity(gameEntity.getUserEntity())
                .build();

        gameRepository.save(game);

        log.info("경기 수정 종료");

        return UpdateGameDto.Response.toDto(game);
    }

    public DeleteGameDto deleteGame(DeleteGameDto request) {

        String loginId = jwtTokenExtract.currentUser().getLoginId();

        GameEntity gameEntity = gameRepository.findByUserEntity_LoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(NOT_GAME_CREATOR));


        LocalDateTime beforeDateTime = gameEntity.getStartDateTime().minusMinutes(30);

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(beforeDateTime)) {
            throw new CustomException(NOT_DELETE_GAME);
        }


        GameEntity game = GameEntity.builder()
                .gameId(gameEntity.getGameId())
                .title(gameEntity.getTitle())
                .content(gameEntity.getContent())
                .headCount(gameEntity.getHeadCount())
                .fieldState(gameEntity.getFieldState())
                .gender(gameEntity.getGender())
                .address(gameEntity.getAddress())
                .cityName(gameEntity.getCityName())
                .matchFormat(gameEntity.getMatchFormat())
                .createdDateTime(gameEntity.getCreatedDateTime())
                .startDateTime(gameEntity.getStartDateTime())
                .userEntity(gameEntity.getUserEntity())
                .deletedDateTime(LocalDateTime.now())
                .build();

        gameRepository.save(game);

        return DeleteGameDto.toDto(game);

    }

}
