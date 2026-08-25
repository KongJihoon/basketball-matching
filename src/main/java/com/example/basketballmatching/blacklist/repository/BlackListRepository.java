package com.example.basketballmatching.blacklist.repository;

import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlackListRepository extends JpaRepository<BlackListEntity, Long> {

    boolean existsByUserEntity_UserId(Long userId);

    List<BlackListEntity> findAllByOrderByBannedDateTimeDesc();

}
