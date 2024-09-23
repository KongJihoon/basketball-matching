package com.example.basketballproject.user.repository;

import com.example.basketballproject.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {


    boolean existsByLoginIdAndDeletedDateTimeNull(String loginId);

    boolean existsByEmailAndDeletedDateTimeNull(String email);

    boolean existsByNicknameAndDeletedDateTimeNull(String nickname);




    Optional<UserEntity> findByEmailAndDeletedDateTimeNull(String email);

    Optional<UserEntity> findByLoginIdAndDeletedDateTimeNull(String loginId);

}
