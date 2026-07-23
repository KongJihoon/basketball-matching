package com.example.basketballmatching.user.repository;

import com.example.basketballmatching.user.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface
UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);

    boolean existsByNickname(String nickname);

    Optional<UserEntity> findByEmailAndDeletedDateTimeIsNull(String email);

    Optional<UserEntity> findByUserIdAndDeletedDateTimeIsNull(Long userId);



}
