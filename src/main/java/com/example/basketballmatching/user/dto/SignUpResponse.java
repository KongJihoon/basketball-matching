package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SignUpResponse(
        @Schema(description = "이메일", example = "test@test.com")
        String email,

        @Schema(description = "닉네임", example = "커리")
        String nickname,

        @Schema(description = "이름", example = "서장훈")
        String name,

        @Schema(description = "생년월일", example = "1997-01-01")
        LocalDate birth,

        @Schema(description = "휴대폰 번호", example = "010-1111-0000")
        String phone,

        @Schema(description = "주소", example = "서울특별시 강남구")
        String address,

        @Schema(description = "포지션", example = "NONE", defaultValue = "NONE")
        Position position,

        @Schema(description = "성별", example = "MALE", defaultValue = "MALE")
        GenderType genderType,

        @Schema(description = "권한", example = "USER", defaultValue = "USER")
        UserType userType,

        @Schema(description = "생성 일시", example = "2026-07-26T15:00:00")
        LocalDateTime createdAt


) {

    public static SignUpResponse fromEntity(UserEntity userEntity) {

        return new SignUpResponse(
                userEntity.getEmail(),
                userEntity.getNickname(),
                userEntity.getName(),
                userEntity.getBirth(),
                userEntity.getPhone(),
                userEntity.getAddress(),
                userEntity.getPosition(),
                userEntity.getGenderType(),
                userEntity.getUserType(),
                userEntity.getCreatedAt()
        );

    }
}
