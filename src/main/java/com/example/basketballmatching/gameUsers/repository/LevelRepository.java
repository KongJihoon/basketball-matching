package com.example.basketballmatching.gameUsers.repository;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameUsers.entity.LevelEntity;
import com.example.basketballmatching.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LevelRepository extends JpaRepository<LevelEntity, Long> {

    boolean existsByGameEntity_GameIdAndEvaluator_UserIdAndReceiver_UserId(
            Long gameId, Long evaluatorId, Long receiverId
    );

    List<LevelEntity> findByReceiverAndGameEntity(UserEntity receiver, GameEntity gameEntity);

}
