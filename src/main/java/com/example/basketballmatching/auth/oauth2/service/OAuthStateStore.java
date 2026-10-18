package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.config.OAuthSecurityProperties;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static com.example.basketballmatching.global.exception.ErrorCode.OAUTH_STATE_INVALID;

@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String STATE_PREFIX = "oauth:state:";
    private static final String STATE_VALUE = "valid";
    private static final int STATE_BYTE_LENGTH = 32;

    private final RedisService redisService;
    private final OAuthSecurityProperties properties;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * OAuth 인증 요청을 식별할 무작위 State를 생성한다.
     *
     * 서버가 발급한 값인지 확인하고 재사용을 방지할 수 있도록
     * State를 Redis에 짧은 유효시간과 함께 저장한다.
     */
    public String issue() {
        String state = createState();

        /*
         * State자체를 Redis Key에 포함한다.
         * TTL이 지나면 완료되지 않은 OAuth 인증 요청은 자동으로 무효화된다.
         */
        redisService.setDataExpireMillis(
                stateKey(state),
                STATE_VALUE,
                properties.stateExpiration().toMillis()
        );

        return state;


    }

    /**
     * Callback으로 돌아온 State를 검증하고 Redis에서 한 번만 소비한다.
     *
     * 브라우저 Cookie와의 일치 여부로 요청의 연속성을 확인하고,
     * Redis 조회 및 삭제를 통해 서버가 발급한 State인지 검증한다.
     */
    public void consume(String returnedState, String cookieState) {

        // 요청 State와 Cookie State의 존재 여부 및 일치 여부를 먼저 확인한다.
        validateState(returnedState, cookieState);

        /*
         * State 조회와 삭제를 하나의 원자적인 연산으로 처리한다.
         * 한 번 사용된 State는 즉시 삭제되므로 재사용 공격을 방지할 수 있다.
         */
        String storedValue = redisService.getAndDeleteData(stateKey(returnedState));

        /*
         * Redis에 State가 없거나 이미 사용되었거나 만료되었다면
         * 유효하지 않은 OAuth 요청으로 처리
         */
        if (!STATE_VALUE.equals(storedValue)) {
            throw new CustomException(OAUTH_STATE_INVALID);
        }

    }

    /**
     * Callback으로 전달된 State와 Cookie에 저장된 State가
     * 모두 존재하고 동일한 값인지 검증한다.
     */
    private void validateState(String returnedState, String cookieState) {

        if (!StringUtils.hasText(returnedState) || !StringUtils.hasText(cookieState)
        || !matches(returnedState, cookieState)) {
            throw new CustomException(OAUTH_STATE_INVALID);
        }

    }

    /**
     * 문자열 비교 과정에서 발생하는 시간 차이를 줄이기 위해
     * 상수 시간 비교 방식으로 두 State를 비교한다.
     */
    private boolean matches(String returnedState, String cookieState) {


        return MessageDigest.isEqual(
                returnedState.getBytes(StandardCharsets.UTF_8),
                cookieState.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 공격자가 예측하기 어렵도록
     * 암호학적으로 안전한 난수를 이용해 State를 생성한다.
     */
    private String createState() {

        // 32바이트, 즉 256비트 길이의 무작위 값을 생성한다.
        byte[] randomBytes = new byte[STATE_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        /*
         * URL 쿼리 파라미터로 안전하게 전달될 수 있도록
         * URL-safe Base64로 인코딩하고 패딩을 제거한다.
         */
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String stateKey(String state)  {
        return STATE_PREFIX + state;
    }


}
