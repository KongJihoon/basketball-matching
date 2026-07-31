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

    public String issue() {
        String state = createState();

        redisService.setDataExpireMillis(
                stateKey(state),
                STATE_VALUE,
                properties.stateExpiration().toMillis()
        );

        return state;


    }

    public void consume(String returnedState, String cookieState) {


        validateState(returnedState, cookieState);

        String storedValue = redisService.getAndDeleteData(stateKey(returnedState));

        if (!STATE_VALUE.equals(storedValue)) {
            throw new CustomException(OAUTH_STATE_INVALID);
        }

    }

    private void validateState(String returnedState, String cookieState) {

        if (!StringUtils.hasText(returnedState) || !StringUtils.hasText(cookieState)
        || !matches(returnedState, cookieState)) {
            throw new CustomException(OAUTH_STATE_INVALID);
        }

    }

    private boolean matches(String returnedState, String cookieState) {


        return MessageDigest.isEqual(
                returnedState.getBytes(StandardCharsets.UTF_8),
                cookieState.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String createState() {

        byte[] randomBytes = new byte[STATE_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String stateKey(String state) {
        return STATE_PREFIX + state;
    }


}
