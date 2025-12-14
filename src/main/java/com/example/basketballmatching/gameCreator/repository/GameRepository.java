package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query ("""
        select g
        from GameEntity g
        where g.deletedDateTime is null 
        and g.placeName = :placeName
        and g.address = :address
        and g.startDateTime < :endDateTime
        and g.endDateTime > :startDateTime
""")
    List<GameEntity> findByOverlapPlace(@Param("placeName") String placeName,
                                        @Param("address") String address,
                                        @Param("startDateTime") LocalDateTime startDateTime,
                                        @Param("endDateTime") LocalDateTime endDateTime);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameEntity g where g.gameId = :gameId and g.deletedDateTime is NULL ")
    Optional<GameEntity> findByGameIdWithLock(@Param("gameId") Long gameId);


    Optional<GameEntity> findByGameIdAndDeletedDateTimeIsNull(Long gameId);

    List<GameEntity> findByUserEntity_UserIdAndDeletedDateTimeIsNull(Long userId);


    long countByPlaceNameAndAddressAndStartDateTimeAndEndDateTime(String placeName, String address, LocalDateTime start, LocalDateTime end);

    Optional<GameEntity> findByTitle(String title);

}
