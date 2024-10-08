package com.example.basketballproject.gameUser.service;


import com.example.basketballproject.auth.security.JwtTokenExtract;
import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.repository.GameRepository;
import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import com.example.basketballproject.gameUser.dto.GameSearchDto;
import com.example.basketballproject.gameUser.dto.ParticipantGameDto;
import com.example.basketballproject.gameUser.entity.ParticipantEntity;
import com.example.basketballproject.gameUser.repository.GameSpecification;
import com.example.basketballproject.gameUser.repository.ParticipantRepository;
import com.example.basketballproject.gameUser.type.ParticipantStatus;
import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.global.exception.ErrorCode;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.repository.UserRepository;
import com.example.basketballproject.user.type.GenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.basketballproject.gameCreator.type.Gender.FEMALEONLY;
import static com.example.basketballproject.gameCreator.type.Gender.MALEONLY;
import static com.example.basketballproject.gameUser.type.ParticipantStatus.ACCEPT;
import static com.example.basketballproject.gameUser.type.ParticipantStatus.CANCEL;
import static com.example.basketballproject.global.exception.ErrorCode.*;
import static com.example.basketballproject.user.type.GenderType.FEMALE;
import static com.example.basketballproject.user.type.GenderType.MALE;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserService {

    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    private final ParticipantRepository participantRepository;

    private final JwtTokenExtract jwtTokenExtract;


    public List<GameSearchDto> findFilteredGame(
            LocalDate localDate,
            CityName cityName,
            FieldState fieldState,
            Gender gender,
            MatchFormat matchFormat
    ) {

        Specification<GameEntity> gameSpec =
                getGameEntitySpec(localDate, cityName, fieldState, gender, matchFormat);


        List<GameEntity> gameListNow = gameRepository.findAll(gameSpec);


        return getGameSearch(gameListNow);
    }

    public List<GameSearchDto> searchAddress(String address) {

        List<GameEntity> gameEntities = gameRepository.findByAddressContainsIgnoreCaseAndStartDateTimeAfterOrderByStartDateTimeAsc(address, LocalDateTime.now());



        return getGameSearch(gameEntities);
    }

    public ParticipantGameDto participantGame(Long gameId) {

        String loginId = jwtTokenExtract.currentUser().getLoginId();

        UserEntity userEntity = userRepository.findByLoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        if (participantRepository.existsByUserEntity_UserIdAndGameEntity_GameId(userEntity.getUserId(), gameEntity.getGameId())) {
            throw new CustomException(ALREADY_PARTICIPANT_USER);
        }

        if (gameEntity.getApplicantNum() >= gameEntity.getHeadCount()) {
            throw new CustomException(FULL_PEOPLE_GAME);
        }

        if (gameEntity.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new CustomException(OVER_TIME_GAME);
        }

        if (gameEntity.getGender().equals(MALEONLY) && userEntity.getGenderType().equals(FEMALE)) {
            throw new CustomException(ONLY_MALE_GAME);
        }

        if (gameEntity.getGender().equals(FEMALEONLY) && userEntity.getGenderType().equals(MALE)) {
            throw new CustomException(ONLY_FEMALE_GAME);
        }

        gameRepository.save(gameEntity);

        return ParticipantGameDto.fromEntity(participantRepository.save(
                ParticipantEntity.builder()
                        .status(ParticipantStatus.APPLY)
                        .createdDateTime(LocalDateTime.now())
                        .gameEntity(gameEntity)
                        .userEntity(userEntity)
                        .build()
        ));

    }

    public ParticipantGameDto cancelParticipantGame(Long gameId) {

        String loginId = jwtTokenExtract.currentUser().getLoginId();

        UserEntity userEntity = userRepository.findByLoginIdAndDeletedDateTimeNull(loginId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        GameEntity gameEntity = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        ParticipantEntity participantEntity = participantRepository.findByUserEntityAndGameEntity(gameEntity, userEntity)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        if (participantEntity.getStatus().equals(ACCEPT) && gameEntity.getStartDateTime().isBefore(LocalDateTime.now().plusMinutes(10))) {
            throw new CustomException(NOT_ALLOWED_CANCEL);
        }

        participantEntity.setStatus(CANCEL);

        participantEntity.setCanceledDateTime(LocalDateTime.now());

        gameEntity.cancelNum();

        gameRepository.save(gameEntity);

        participantRepository.save(participantEntity);

        return ParticipantGameDto.fromEntity(participantEntity);



    }



    private Specification<GameEntity> getGameEntitySpec(LocalDate localDate, CityName cityName, FieldState fieldState, Gender gender, MatchFormat matchFormat) {


        log.info("경기 검색 시작");

        Specification<GameEntity> specification = Specification.where(
                GameSpecification.startDate(LocalDate.now()).and(GameSpecification.notDeleted()));

        if (localDate != null) {
            specification = specification.and(GameSpecification.withDate(localDate).and(GameSpecification.notDeleted()));
        }

        if (cityName != null) {
            specification = specification.and(GameSpecification.withCityName(cityName));
        }

        if (fieldState != null) {
            specification = specification.and(GameSpecification.withFieldStatus(fieldState));
        }

        if (gender != null) {
            specification = specification.and(GameSpecification.withGender(gender));
        }

        if (matchFormat != null) {
            specification.and(GameSpecification.withMatchFormat(matchFormat));
        }

        return specification;
    }

    private static List<GameSearchDto> getGameSearch(
            List<GameEntity> gameListNow
    ) {

        List<GameSearchDto> gameList = new ArrayList<>();

        gameListNow.forEach((e) -> gameList.add(GameSearchDto.of(e)));

        return gameList;
    }

}
