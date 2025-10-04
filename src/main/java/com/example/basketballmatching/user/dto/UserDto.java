package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.entity.UserEntity;
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

    private String name;

    private LocalDate birth;

    private String phone;

    private String position;

    private String userType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserDto fromEntity(UserEntity userEntity) {


        return UserDto.builder()
                .userId(userEntity.getUserId())
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .name(userEntity.getName())
                .birth(userEntity.getBirth())
                .phone(userEntity.getPhone())
                .position(String.valueOf(userEntity.getPosition()))
                .userType(String.valueOf(userEntity.getUserType()))
                .createdAt(userEntity.getCreatedAt())
                .updatedAt(userEntity.getUpdatedAt())
                .build();

    }





}
