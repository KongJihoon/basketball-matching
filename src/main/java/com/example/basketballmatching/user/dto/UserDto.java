package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
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

    private String nickname;

    private String name;

    private LocalDate birth;

    private String phone;

    private String address;

    private Position position;

    private GenderType genderType;

    private UserType userType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserDto fromEntity(UserEntity userEntity) {


        return UserDto.builder()
                .userId(userEntity.getUserId())
                .email(userEntity.getEmail())
                .nickname(userEntity.getNickname())
                .name(userEntity.getName())
                .birth(userEntity.getBirth())
                .phone(userEntity.getPhone())
                .address(userEntity.getAddress())
                .position(userEntity.getPosition())
                .userType(userEntity.getUserType())
                .genderType(userEntity.getGenderType())
                .createdAt(userEntity.getCreatedAt())
                .updatedAt(userEntity.getUpdatedAt())
                .build();

    }





}
