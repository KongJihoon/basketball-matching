package com.example.basketballmatching.auth.dto;

import com.example.basketballmatching.user.domain.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokenResponse(
        @Schema(
                description = "Access Token"
        )
        String accessToken,

        @Schema(
                description = "Refresh Token"
        )
        String refreshToken,

        @Schema(
                description = "인증 사용자 정보"
        )
        AuthenticatedUserResponse user
) {

    public static AuthTokenResponse of(
            String accessToken,
            String refreshToken,
            UserEntity userEntity
    ) {
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                AuthenticatedUserResponse.fromEntity(userEntity)
        );
    }
}
