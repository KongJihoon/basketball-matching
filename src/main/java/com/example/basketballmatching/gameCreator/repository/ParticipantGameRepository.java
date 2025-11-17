package com.example.basketballmatching.gameCreator.repository;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipantGameRepository extends JpaRepository<ParticipantGameEntity, Long> {

    boolean existsByUserEntity_UserIdAndGameEntity_GameId(Long userId, Long gameId);


    Page<ParticipantGameEntity> findByParticipantGameStatusAndGameEntity_GameId(ParticipantGameStatus status, Long gameId, Pageable pageable);

    List<ParticipantGameEntity> findByParticipantGameStatusInAndGameEntity_GameId(List<ParticipantGameStatus> statuses, Long gameId);



    Optional<ParticipantGameEntity> findByGameEntity_GameIdAndUserEntity_UserId(Long gameId, Long userId);





}
