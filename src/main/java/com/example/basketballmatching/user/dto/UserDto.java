package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class UserDto {

    @Schema(description = "PK", example = "1", defaultValue = "1")
    private Long userId;

    @Schema(description = "이메일", example = "test@test.com")
    private String email;

    @Schema(description = "닉네임", example = "커리")
    private String nickname;

    @Schema(description = "이름", example = "서장훈")
    private String name;

    @Schema(description = "생년월일", example = "19970101")
    private LocalDate birth;

    @Schema(description = "휴대폰 번호", example = "010-1111-0000")
    private String phone;

    @Schema(description = "주소", example = "서울특별시 강남구")
    private String address;

    @Schema(description = "포지션", example = "NONE", defaultValue = "NONE")
    private Position position;

    @Schema(description = "성별", example = "MALE", defaultValue = "MALE")
    private GenderType genderType;

    @Schema(description = "권한", example = "USER", defaultValue = "USER")
    private UserType userType;

    @Schema(description = "생성 일시", example = "2025-11-22T15:00:00:00", defaultValue = "2025-11-22T15:00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "변경 일시", example = "2025-11-22T15:00:00:00", defaultValue = "2025-11-22T15:00:00:00")
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
