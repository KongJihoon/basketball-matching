package com.example.basketballmatching.game.repository.query;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.domain.QGameEntity;
import com.example.basketballmatching.game.domain.QParticipantGameEntity;
import com.example.basketballmatching.game.dto.request.GameListCondition;
import com.example.basketballmatching.game.type.GameSortType;
import com.example.basketballmatching.user.domain.QUserEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.basketballmatching.game.type.GameSortType.START_TIME_ASC;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.ACCEPT;

@Repository
@RequiredArgsConstructor
public class GameQueryRepository {


    private final JPAQueryFactory jpaQueryFactory;


    public Page<GameEntity> findGames(GameListCondition condition, Pageable pageable, LocalDateTime now) {

        QGameEntity game = QGameEntity.gameEntity;

        BooleanBuilder builder = createGameListCondition(condition, game, now);

        List<GameEntity> content = jpaQueryFactory
                .selectFrom(game)
                .where(builder)
                .orderBy(resolveOrderSpecifiers(
                        condition.sortType(), game
                ))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(
                jpaQueryFactory
                        .select(game.count())
                        .from(game)
                        .where(builder)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);


    }



    public Page<ParticipantGameEntity> findUpcomingGamesByUser(Long userId, Pageable pageable, LocalDateTime now) {

        QParticipantGameEntity participation = QParticipantGameEntity.participantGameEntity;

        BooleanBuilder condition = new BooleanBuilder();

        condition.and(participation.userEntity.userId.eq(userId));

        condition.and(participation.participantGameStatus.eq(ACCEPT));

        condition.and(participation.gameEntity.deletedDateTime.isNull());

        condition.and(participation.gameEntity.startDateTime.gt(now));

        List<ParticipantGameEntity> content = jpaQueryFactory
                .selectFrom(participation)
                .where(condition)
                .orderBy(participation.gameEntity.startDateTime.asc(),
                        participation.participantGameId.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(
                jpaQueryFactory
                        .select(participation.count())
                        .from(participation)
                        .where(condition)
                        .fetchOne()
        ).orElse(0L);


        return new PageImpl<>(content, pageable, total);

    }



    public Page<ParticipantGameEntity> findCompletedGamesByUser(Long userId, Pageable pageable, LocalDateTime now) {

        QParticipantGameEntity participation = QParticipantGameEntity.participantGameEntity;

        BooleanBuilder condition = new BooleanBuilder();


        condition.and(participation.userEntity.userId.eq(userId));

        condition.and(participation.participantGameStatus.eq(ACCEPT));

        condition.and(participation.gameEntity.deletedDateTime.isNull());

        condition.and(participation.gameEntity.endDateTime.loe(now));

        List<ParticipantGameEntity> content = jpaQueryFactory
                .selectFrom(participation)
                .where(condition)
                .orderBy(participation.gameEntity.endDateTime.desc(), participation.participantGameId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(
                jpaQueryFactory
                        .select(participation.count())
                        .from(participation)
                        .where(condition)
                        .fetchOne()
        ).orElse(0L);


        return new PageImpl<>(content, pageable, total);

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
                                .eq(ACCEPT)
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
                                .participantGameStatus.eq(ACCEPT),
                        game.startDateTime.gt(now),
                        game.deletedDateTime.isNull(),
                        game.userEntity.userId.ne(userId)

                )
                .fetch();
    }

    private BooleanBuilder createGameListCondition(GameListCondition condition, QGameEntity game, LocalDateTime now) {

        BooleanBuilder builder = new BooleanBuilder();

        builder.and(game.deletedDateTime.isNull());

        if (condition.date() != null) {
            LocalDateTime startOfDay = condition.date().atStartOfDay();

            LocalDateTime nextDay = condition.date().plusDays(1).atStartOfDay();

            builder.and(game.startDateTime.goe(startOfDay));

            builder.and(game.startDateTime.lt(nextDay));
        } else {
            builder.and(game.startDateTime.goe(now));
        }

        if (StringUtils.hasText(condition.keyword())) {
            String keyword = condition.keyword().trim();


            builder.and(
                    game.title.containsIgnoreCase(keyword)
                            .or(game.placeName.containsIgnoreCase(keyword))
            );

        }



        if (condition.cityName() != null) {
            builder.and(game.cityName.eq(condition.cityName()));
        }

        if (condition.matchFormat() != null) {
            builder.and(game.matchFormat.eq(condition.matchFormat()));
        }

        if (condition.fieldStatus() != null) {
            builder.and(
                    game.fieldStatus.eq(condition.fieldStatus())
            );
        }

        if (condition.matchGenderType() != null) {
            builder.and(
                    game.matchGenderType.eq(
                            condition.matchGenderType()
                    )
            );
        }

        if (condition.gameStatus() != null) {
            builder.and(
                    game.gameStatus.eq(condition.gameStatus())
            );
        }


        return builder;
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(GameSortType sortType, QGameEntity game) {


        GameSortType resolveSortType = sortType == null ? START_TIME_ASC : sortType;

        return switch (resolveSortType) {
            case START_TIME_ASC -> new OrderSpecifier[]{
                    game.startDateTime.asc(),
                    game.gameId.asc()
            };
            case LATEST -> new OrderSpecifier[]{
                    game.createdAt.desc(),
                    game.gameId.desc()
            };
        };

    }




}
