package com.example.basketballproject.gameCreator.repository;

import com.example.basketballproject.gameCreator.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GameRepository extends JpaRepository<GameEntity, Long> {


    Optional<Long> countByStartDateTimeBetweenAndAddressAndDeletedDateTimeNull(
            LocalDateTime beforeDateTime, LocalDateTime afterDateTime, String address
    );

}
