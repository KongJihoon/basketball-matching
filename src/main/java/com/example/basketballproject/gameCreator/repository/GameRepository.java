package com.example.basketballproject.gameCreator.repository;

import com.example.basketballproject.gameCreator.entity.GameEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<GameEntity, Long> {


    Optional<Long> countByStartDateTimeBetweenAndAddressAndDeletedDateTimeNull(
            LocalDateTime beforeDateTime, LocalDateTime afterDateTime, String address
    );



    Optional<GameEntity> findByUserEntity_LoginIdAndDeletedDateTimeNull(String loginId);

    List<GameEntity> findAll(Specification<GameEntity> specification);

    List<GameEntity> findByAddressContainsIgnoreCaseAndStartDateTimeAfterOrderByStartDateTimeAsc(
            String partOfAddress, LocalDateTime currentDateTime
    );

}
