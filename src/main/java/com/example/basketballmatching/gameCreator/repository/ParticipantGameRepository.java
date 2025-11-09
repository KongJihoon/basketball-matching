package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantGameRepository extends JpaRepository<ParticipantGameEntity, Long> {

    boolean existsByUserEntity_UserIdAndGameEntity_GameId(Long gameId, Long UserId);

    int countByParticipantGameStatusAndGameEntity_GameId(
            ParticipantGameStatus participantGameStatus, Long gameId
    );

}
