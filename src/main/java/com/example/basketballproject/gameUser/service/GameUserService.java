package com.example.basketballproject.gameUser.service;


import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.repository.GameRepository;
import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import com.example.basketballproject.gameUser.dto.GameSearchDto;
import com.example.basketballproject.gameUser.repository.GameSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameUserService {

    private final GameRepository gameRepository;


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
