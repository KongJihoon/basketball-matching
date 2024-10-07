package com.example.basketballproject.gameUser.repository;

import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class GameSpecification {


    public static Specification<GameEntity> withCityName(CityName cityName) {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("cityName"), cityName));

    }


    public static Specification<GameEntity> withFieldStatus(FieldState fieldState) {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("fieldState"), fieldState));

    }


    public static Specification<GameEntity> withGender(Gender gender) {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("gender"), gender));

    }


    public static Specification<GameEntity> withMatchFormat(MatchFormat matchFormat) {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("matchFormat"), matchFormat));

    }

    public static Specification<GameEntity> startDate(LocalDate date) {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("startDateTime").as(LocalDate.class), date));
    }

    public static Specification<GameEntity> notDeleted() {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedDateTime")));
    }

    public static Specification<GameEntity> withDate(
            LocalDate date
    ) {

        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("startDateTime").as(LocalDate.class), date
        ));
    }



}
