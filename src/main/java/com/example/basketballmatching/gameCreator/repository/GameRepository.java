package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GameRepository extends JpaRepository<GameEntity, Long> {


    @Query ("""
        select count(g) > 0
        from GameEntity g
        where g.deletedDateTime IS NULL 
        and g.placeName = :placeName
        and g.address = :address
        and g.startDateTime < :endDateTime
        and g.endDateTime > :startDateTime
""")
    boolean existsBySamePlaceAtSameTime(@Param("placeName") String placeName,
                                        @Param("address") String address,
                                        @Param("startDateTime") LocalDateTime startDateTime,
                                        @Param("endDateTime") LocalDateTime endDateTime);

    Optional<GameEntity> findByGameIdAndDeletedDateTimeIsNull(Long gameId);

}
