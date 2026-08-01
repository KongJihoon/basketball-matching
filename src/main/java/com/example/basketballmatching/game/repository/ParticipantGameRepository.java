package com.example.basketballmatching.game.repository;

import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantGameRepository extends JpaRepository<ParticipantGameEntity, Long> {

    boolean existsByUserEntity_UserIdAndGameEntity_GameId(Long userId, Long gameId);


    Page<ParticipantGameEntity> findByParticipantGameStatusAndGameEntity_GameId(ParticipantGameStatus status, Long gameId, Pageable pageable);

    List<ParticipantGameEntity> findByParticipantGameStatusInAndGameEntity_GameId(List<ParticipantGameStatus> statuses, Long gameId);



    Optional<ParticipantGameEntity> findByGameEntity_GameIdAndUserEntity_UserId(Long gameId, Long userId);


    List<ParticipantGameEntity> findByUserEntity_UserIdAndParticipantGameStatusIn(Long userId, List<ParticipantGameStatus> statuses);



    @Query(
            """
        select p from ParticipantGameEntity p
        join fetch p.userEntity
        where p.gameEntity.gameId = :gameId
        and p.participantGameStatus = :participantGameStatus
"""
    )
    List<ParticipantGameEntity> findByGameEntity_GameIdAndParticipantGameStatus(@Param("gameId") Long gameId, @Param("participantGameStatus") ParticipantGameStatus participantGameStatus);
}
