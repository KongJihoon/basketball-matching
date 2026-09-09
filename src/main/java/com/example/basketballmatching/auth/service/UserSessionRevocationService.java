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

    /**
     * 탈퇴 사용자의 현재 AccessToken과 저장된 RefreshToken 폐기
     */
    public void revokeAll(String email, String accessToken) {

        validateAccessToken(accessToken);

        /*
         * AccessToken은 서버에 저장된 세션이 없는 JWT이므로 직접 삭제 불가
         * 토큰의 남은 유효시간을 계산해 Redis 블랙리스트 TTL로 사용하여 접근 제어
         */
        long remainingTime = tokenProvider.getRemainingTime(accessToken);

        authTokenStore.revokeAccessToken(accessToken, remainingTime);

        // RefreshToken 폐기
        authTokenStore.deleteRefreshToken(email);

        log.info("[사용자 토큰 폐기 완료] email={}", email);

    }

    private void validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }
    }

}
