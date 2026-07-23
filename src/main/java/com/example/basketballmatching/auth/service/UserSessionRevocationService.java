package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionRevocationService {

    private static final String LOGOUT_ACCESS_PREFIX =
            "logout:access:";

    private static final String REFRESH_TOKEN_PREFIX =
            "refreshToken:";

    private final RedisService redisService;

    private final TokenProvider tokenProvider;

    public void revokeAll(String email, String accessToken) {

        validateAccessToken(accessToken);

        long remainingTime = tokenProvider.getRemainingTime(accessToken);

        if (remainingTime > 0) {
            redisService.setDataExpireMillis(LOGOUT_ACCESS_PREFIX + accessToken, "LOGOUT", remainingTime);
        }

        redisService.deleteData(REFRESH_TOKEN_PREFIX + email);

        log.info("[사용자 토큰 폐기 완료] email={}", email);

    }

    private void validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }
    }

}
