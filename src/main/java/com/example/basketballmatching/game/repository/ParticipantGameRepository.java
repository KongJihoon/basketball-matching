package com.example.basketballmatching.game.repository;

import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantGameRepository extends JpaRepository<ParticipantGameEntity, Long> {

    boolean existsByUserEntity_UserIdAndGameEntity_GameId(Long userId, Long gameId);

    boolean existsByGameEntity_GameIdAndUserEntity_UserIdAndParticipantGameStatus(
            Long gameId, Long userId, ParticipantGameStatus status
    );

    List<ParticipantGameEntity> findByParticipantGameStatusInAndGameEntity_GameId(List<ParticipantGameStatus> statuses, Long gameId);



    Optional<ParticipantGameEntity> findByGameEntity_GameIdAndUserEntity_UserId(Long gameId, Long userId);


    List<ParticipantGameEntity> findByUserEntity_UserIdAndParticipantGameStatusIn(Long userId, List<ParticipantGameStatus> statuses);

    Page<ParticipantGameEntity> findByGameEntity_GameIdAndParticipantGameStatus(Long gameId, ParticipantGameStatus status, Pageable pageable);


    Optional<ParticipantGameEntity> findByParticipantGameIdAndGameEntity_GameId(Long participantGameId, Long gameId);
}
