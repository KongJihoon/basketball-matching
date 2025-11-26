package com.example.basketballmatching.gameUsers.repository;

import com.example.basketballmatching.gameUsers.dto.GameAvgScoreDto;
import com.example.basketballmatching.gameUsers.entity.QLevelEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LevelQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<GameAvgScoreDto> findAvgScoreByGames(Long receiverId, List<Long> gameIds) {

        QLevelEntity levelEntity = QLevelEntity.levelEntity;

        return jpaQueryFactory
                .select(Projections.constructor(
                        GameAvgScoreDto.class,
                        levelEntity.gameEntity.gameId,
                        levelEntity.score.avg()
                ))
                .from(levelEntity)
                .where(
                        levelEntity.receiver.userId.eq(receiverId),
                        levelEntity.gameEntity.gameId.in(gameIds)
                )
                .groupBy(levelEntity.gameEntity.gameId)
                .fetch();

    }


}
