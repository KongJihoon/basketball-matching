package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.QGameEntity;
import com.example.basketballmatching.gameCreator.type.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameQueryRepository {


    private final JPAQueryFactory jpaQueryFactory;


    public Page<SearchGameDto> searchByKeyword(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, Pageable pageable) {


        QGameEntity qGameEntity = QGameEntity.gameEntity;

        BooleanBuilder builder = new BooleanBuilder();

        validationSearch(date, cityName, matchFormat, fieldStatus, matchGenderType, gameStatus, builder, qGameEntity);

        List<GameEntity> gameEntities = jpaQueryFactory
                .select(qGameEntity)
                .from(qGameEntity)
                .where(builder)
                .orderBy(qGameEntity.startDateTime.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<SearchGameDto> searchGames = gameEntities.stream()
                .map(SearchGameDto::fromEntity)
                .toList();

        Long total = Optional.ofNullable(
                jpaQueryFactory
                        .select(qGameEntity.count())
                        .from(qGameEntity)
                        .where(builder)
                        .fetchOne()
        ).orElse(0L);


        return new PageImpl<>(searchGames, pageable, total);
    }

    private static void validationSearch(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, BooleanBuilder builder, QGameEntity qGameEntity) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        builder.and(qGameEntity.startDateTime.between(startOfDay, endOfDay));

        if (cityName != null) {
            builder.and(qGameEntity.cityName.eq(cityName));
        }

        if (gameStatus != null) {
            builder.and(qGameEntity.gameStatus.eq(gameStatus));
        }

        if (fieldStatus != null) {
            builder.and(qGameEntity.fieldStatus.eq(fieldStatus));
        }

        if (matchFormat != null) {
            builder.and(qGameEntity.matchFormat.eq(matchFormat));
        }

        if (matchGenderType != null) {
            builder.and(qGameEntity.matchGenderType.eq(matchGenderType));
        }
    }


}
