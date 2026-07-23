package com.example.basketballmatching.auth.dto;

import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthenticatedUserResponse(
        @Schema(
                description = "사용자 ID",
                example = "1"
        )
        Long userId,

        @Schema(
                description = "이메일",
                example = "test@test.com"
        )
        String email,

        @Schema(
                description = "닉네임",
                example = "커리"
        )
        String nickname,

        @Schema(
                description = "사용자 권한",
                example = "USER"
        )
        UserType userType
) {

    public static AuthenticatedUserResponse fromEntity(UserEntity userEntity) {
        return new AuthenticatedUserResponse(
                userEntity.getUserId(),
                userEntity.getEmail(),
                userEntity.getNickname(),
                userEntity.getUserType()
        );
    }

}
