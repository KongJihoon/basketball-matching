package com.example.basketballproject.gameUser.repository;

import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameUser.entity.ParticipantEntity;
import com.example.basketballproject.gameUser.type.ParticipantStatus;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.type.GenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, Long> {

    Optional<Long> countByStatusAndGameEntityGameId(ParticipantStatus participantStatus, Long gameId);


    Optional<Long> countByStatusAndGameEntityGameIdAndUserEntityGenderType(ParticipantStatus status, Long gameId, GenderType gender);

    boolean existsByUserEntity_UserIdAndGameEntity_GameId(Long userId, Long gameId);

    Optional<ParticipantEntity> findByUserEntityAndGameEntity(GameEntity gameEntity, UserEntity userEntity);

}


