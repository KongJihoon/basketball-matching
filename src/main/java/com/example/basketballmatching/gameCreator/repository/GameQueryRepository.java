package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.dto.SearchGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.entity.QGameEntity;
import com.example.basketballmatching.gameCreator.entity.QParticipantGameEntity;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.domain.QUserEntity;
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

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.*;

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

    public List<Long> findRecent10GamesByUser(UserEntity userEntity) {

        QParticipantGameEntity participantGameEntity = QParticipantGameEntity.participantGameEntity;

        QGameEntity gameEntity = QGameEntity.gameEntity;

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(participantGameEntity.userEntity.eq(userEntity));
        builder.and(gameEntity.endDateTime.before(LocalDateTime.now()));
        builder.and(gameEntity.deletedDateTime.isNull());
        builder.and(participantGameEntity.participantGameStatus.eq(ACCEPT));

        return jpaQueryFactory
                .select(gameEntity.gameId)
                .from(participantGameEntity)
                .join(participantGameEntity.gameEntity, gameEntity)
                .where(builder)
                .orderBy(gameEntity.endDateTime.desc())
                .limit(10)
                .fetch();

    }

    public List<CurrentGameListDto> getCurrentGameList(Long userId, Pageable pageable) {

        QParticipantGameEntity participantGame = QParticipantGameEntity.participantGameEntity;

        QGameEntity gameEntity = QGameEntity.gameEntity;

        LocalDateTime now = LocalDateTime.now();



        List<ParticipantGameEntity> gameEntities = jpaQueryFactory
                .select(participantGame)
                .from(participantGame)
//                .join(participantGame.gameEntity, gameEntity)
//                .fetchJoin()
                .where(
                        participantGame.userEntity.userId.eq(userId),
                        participantGame.participantGameStatus.in(ACCEPT, APPLY),
                        participantGame.gameEntity.startDateTime.after(now))
                .orderBy(gameEntity.startDateTime.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return gameEntities.stream()
                .map(CurrentGameListDto::fromEntity)
                .toList();

    }

    public List<LastGameListDto> getLastGameList(Long userId, Pageable pageable) {

        QParticipantGameEntity participantGameEntity = QParticipantGameEntity.participantGameEntity;
        QGameEntity gameEntity = QGameEntity.gameEntity;

        LocalDateTime now = LocalDateTime.now();



        List<ParticipantGameEntity> lastGameList = jpaQueryFactory
                .select(participantGameEntity)
                .from(participantGameEntity)
                .join(participantGameEntity.gameEntity, gameEntity)
                .fetchJoin()
                .where(
                        participantGameEntity.userEntity.userId.eq(userId),
                        participantGameEntity.participantGameStatus.eq(ACCEPT),
                        participantGameEntity.gameEntity.endDateTime.before(now)
                )
                .orderBy(gameEntity.endDateTime.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        return lastGameList.stream()
                .map(LastGameListDto::fromEntity)
                .toList();
    }


    public List<ParticipantGameEntity> getParticipantUsers(Long gameId, LocalDateTime now) {

        QParticipantGameEntity participantGameEntity = QParticipantGameEntity.participantGameEntity;


        BooleanBuilder builder = new BooleanBuilder();

        builder.and(participantGameEntity.gameEntity.gameId.eq(gameId));
        builder.and(participantGameEntity.participantGameStatus.in(ACCEPT, APPLY));
        builder.and(participantGameEntity.gameEntity.startDateTime.after(now));


        List<ParticipantGameEntity> gameEntities = jpaQueryFactory.
                select(participantGameEntity)
                .from(participantGameEntity)
                .where(builder)
                .fetch();

        return gameEntities;
    }


    /**
     * 생성한 예정경기 조회
     */
    public List<GameEntity> findFutureGamesCreatedBy(Long userId, LocalDateTime now) {

        QGameEntity game = QGameEntity.gameEntity;

        return jpaQueryFactory
                .selectFrom(game)
                .where(
                        game.userEntity.userId.eq(userId),
                        game.deletedDateTime.isNull(),
                        game.startDateTime.gt(now)
                )
                .fetch();

    }

    /**
     * 취소 경기의 활성 참가자 조회
     */
    public List<ParticipantGameEntity> findActiveParticipantsByGameIds(List<Long> gameIds) {

        if (gameIds.isEmpty()) {
            return List.of();
        }

        QParticipantGameEntity participantGame = QParticipantGameEntity.participantGameEntity;

        QGameEntity game = QGameEntity.gameEntity;

        QUserEntity user = QUserEntity.userEntity;

        return jpaQueryFactory
                .selectFrom(participantGame)
                .join(participantGame.gameEntity, game)
                .fetchJoin()
                .join(participantGame.userEntity, user)
                .fetchJoin()
                .where(
                        game.gameId.in(gameIds),
                        participantGame
                                .participantGameStatus
                                .in(APPLY, ACCEPT)
                )
                .fetch();

    }

    /**
     * 탈퇴자가 참가한 다른 예정 경기
     */
    public List<ParticipantGameEntity> findFutureParticipationExcludingCreatedGames(
            Long userId,
            LocalDateTime now
    ) {
        QParticipantGameEntity participantGame = QParticipantGameEntity.participantGameEntity;

        QGameEntity game = QGameEntity.gameEntity;

        return jpaQueryFactory
                .selectFrom(participantGame)
                .join(participantGame.gameEntity, game)
                .fetchJoin()
                .where(
                        participantGame.userEntity.userId.eq(userId),
                        participantGame
                                .participantGameStatus.in(APPLY, ACCEPT),
                        game.startDateTime.gt(now),
                        game.deletedDateTime.isNull(),
                        game.userEntity.userId.ne(userId)

                )
                .fetch();
    }



    private static void validationSearch(LocalDate date, CityName cityName, MatchFormat matchFormat, FieldStatus fieldStatus, MatchGenderType matchGenderType, GameStatus gameStatus, BooleanBuilder builder, QGameEntity qGameEntity) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        builder.and(qGameEntity.startDateTime.between(startOfDay, endOfDay));

        builder.and(qGameEntity.deletedDateTime.isNull());

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
