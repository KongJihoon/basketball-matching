package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.example.basketballmatching.global.exception.ErrorCode.NOT_FOUND_TOKEN;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionRevocationService {



    private final TokenProvider tokenProvider;
    private final AuthTokenStore authTokenStore;

    public void revokeAll(String email, String accessToken) {

        validateAccessToken(accessToken);

        long remainingTime = tokenProvider.getRemainingTime(accessToken);

        authTokenStore.revokeAccessToken(accessToken, remainingTime);

        authTokenStore.deleteRefreshToken(email);

        log.info("[사용자 토큰 폐기 완료] email={}", email);

    }

    private void validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }
    }

}
