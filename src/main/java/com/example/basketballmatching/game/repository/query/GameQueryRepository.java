package com.example.basketballmatching.game.repository.query;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.domain.QGameEntity;
import com.example.basketballmatching.game.domain.QParticipantGameEntity;
import com.example.basketballmatching.game.dto.CurrentGameListDto;
import com.example.basketballmatching.game.dto.LastGameListDto;
import com.example.basketballmatching.game.dto.request.GameListCondition;
import com.example.basketballmatching.game.type.GameSortType;
import com.example.basketballmatching.user.domain.QUserEntity;
import com.example.basketballmatching.user.domain.UserEntity;
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

import static com.example.basketballmatching.game.type.GameSortType.*;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.game.type.ParticipantGameStatus.APPLY;

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
                        participantGame.participantGameStatus.in(ACCEPT),
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
