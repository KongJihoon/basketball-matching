package com.example.basketballproject.user.dto;


import com.example.basketballproject.user.entity.UserEntity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private Long userId;

    private String loginId;

    private String password;

    private String email;

    private String name;

    private String nickname;

    private LocalDate birth;

    private String userType;

    private String genderType;

    private String position;

    private LocalDateTime createdDateTime;

    private LocalDateTime updatedDateTime;

    private LocalDateTime deletedDateTime;

    public static UserDto fromEntity(UserEntity userEntity) {

        return UserDto.builder()
                .userId(userEntity.getUserId())
                .loginId(userEntity.getLoginId())
                .password(userEntity.getPassword())
                .email(userEntity.getEmail())
                .name(userEntity.getName())
                .nickname(userEntity.getNickname())
                .birth(userEntity.getBirth())
                .userType(String.valueOf(userEntity.getUserType()))
                .position(String.valueOf(userEntity.getPosition()))
                .genderType(String.valueOf(userEntity.getGenderType()))
                .createdDateTime(userEntity.getCreatedDateTime())
                .updatedDateTime(userEntity.getUpdatedDateTime())
                .deletedDateTime(userEntity.getDeletedDateTime())
                .build();

    }

}
