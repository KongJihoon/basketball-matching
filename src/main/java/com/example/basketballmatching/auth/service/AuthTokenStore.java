package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenStore {

    private static final String REFRESH_TOKEN_PREFIX =
            "refreshToken:";

    private static final String REVOKED_ACCESS_TOKEN_PREFIX =
            "logout:access:";

    private static final String REVOKED_VALUE =
            "LOGOUT";

    private final RedisService redisService;

    public void saveRefreshToken(String email, String refreshToken, long expirationMillis) {
        redisService.setDataExpireMillis(
                refreshTokenKey(email),
                refreshToken,
                expirationMillis
        );
    }

    public String getRefreshToken(String email) {
        return redisService.getData(refreshTokenKey(email));
    }

    public void deleteRefreshToken(String email) {
        redisService.deleteData(refreshTokenKey(email));
    }

    public void revokeAccessToken(String accessToken, long expirationMillis) {
        if (expirationMillis <= 0) {
            return;
        }

        redisService.setDataExpireMillis(revokedAccessToken(accessToken), REVOKED_VALUE, expirationMillis);
    }

    public boolean isAccessTokenRevoked(String accessToken) {
        return redisService.getData(revokedAccessToken(accessToken)) != null;
    }

    private String revokedAccessToken(String accessToken) {
        return REVOKED_ACCESS_TOKEN_PREFIX + accessToken;
    }

    private String refreshTokenKey(String email) {

        return REFRESH_TOKEN_PREFIX + email;
    }

}
