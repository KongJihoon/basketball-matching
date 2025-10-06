package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class UserDto {

    private Long userId;

    private String email;

    private String password;

    private String nickname;

    private String name;

    private LocalDate birth;

    private String phone;

    private Position position;

    private UserType userType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserDto fromEntity(UserEntity userEntity) {


        return UserDto.builder()
                .userId(userEntity.getUserId())
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .nickname(userEntity.getNickname())
                .name(userEntity.getName())
                .birth(userEntity.getBirth())
                .phone(userEntity.getPhone())
                .position(userEntity.getPosition())
                .userType(userEntity.getUserType())
                .createdAt(userEntity.getCreatedAt())
                .updatedAt(userEntity.getUpdatedAt())
                .build();

    }





}
