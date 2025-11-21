package com.example.basketballmatching.blackList.entity;


import com.example.basketballmatching.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlackListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blackListId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private UserEntity userEntity;

    private LocalDateTime bannedDateTime;

    public static BlackListEntity create(UserEntity userEntity, LocalDateTime bannedDateTime) {
        return BlackListEntity.builder()
                .userEntity(userEntity)
                .bannedDateTime(bannedDateTime)
                .build();
    }




}
