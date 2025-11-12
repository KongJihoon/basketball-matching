package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantGameRepository extends JpaRepository<ParticipantGameEntity, Long> {

    boolean existsByParticipantGameIdAndGameEntity_GameId(Long gameId, Long UserId);

    int countByParticipantGameStatusAndGameEntity_GameId(
            ParticipantGameStatus participantGameStatus, Long gameId
    );

    Page<ParticipantGameEntity> findByParticipantGameStatusAndGameEntity_GameId(ParticipantGameStatus status, Long gameId, Pageable pageable);

    List<ParticipantGameEntity> findByParticipantGameStatusInAndGameEntity_GameId(List<ParticipantGameStatus> statuses, Long gameId);

    Optional<ParticipantGameEntity> findByGameEntity_GameIdAndParticipantGameId(Long gameId, Long userId);


    Page<ParticipantGameEntity> findByUserEntity_UserIdAndParticipantGameStatusIn(Long userId, List<ParticipantGameStatus> statuses, Pageable pageable);


    Page<ParticipantGameEntity> findByUserEntity_UserIdAndParticipantGameStatus(Long userId, ParticipantGameStatus participantGameStatus, Pageable pageable);

}
