package com.example.basketballmatching.user.repository;

import com.example.basketballmatching.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUserIdAndDeletedDateTimeIsNull(Long userId);

}
