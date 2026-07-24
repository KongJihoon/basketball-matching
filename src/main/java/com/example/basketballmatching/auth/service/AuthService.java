package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static com.example.basketballmatching.user.type.LoginProvider.KAKAO;
import static com.example.basketballmatching.user.type.LoginProvider.LOCAL;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String BLACKLIST_PREFIX =
            "blackList:";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;

    private final RedisService redisService;

    private final AuthTokenStore authTokenStore;

    private final UserSessionRevocationService userSessionRevocationService;


    @Transactional(readOnly = true)
    public AuthTokenResponse login(String email, String password) {

        log.info("[유저 로그인 시작]: {}", email);



        UserEntity user = getActiveUser(email);



        // 로그인 형식 검사
        validateLocalLogin(user);

        // 비밀번호 유효성 검사
        validatePassword(password, user);

        // 블랙리스트 유저 검사
        validateNotBlacklisted(user.getEmail());


        log.info("[유저 로그인 완료] email : {}", email);

        return  issueTokens(user);
    }




    @Transactional(readOnly = true)
    public AuthTokenResponse loginWithKakao(String email) {

        log.info("[카카오 로그인 검증 시작] email : {}", email);

        UserEntity user = getActiveUser(email);

        // 로그인 형식 검사
        validateKakaoLogin(user);

        validateNotBlacklisted(email);

        log.info("[카카오 로그인 완료] email : {}", email);

        return issueTokens(user);
    }


    @Transactional(readOnly = true)
    public AuthTokenResponse reissue(String refreshToken) {

        tokenProvider.validateRefreshToken(refreshToken);

        String email = tokenProvider.getEmailFromToken(refreshToken);

        log.info("[토큰 재발급 시작]: {}", email);


        String savedRefreshToken = authTokenStore.getRefreshToken(email);

        // 재발금 유효성 검사
        validateReissue(refreshToken, savedRefreshToken);

        UserEntity user = getActiveUser(email);

        String accessToken = issueAccessToken(user);

        log.info("[토큰 재발급 완료] email={}", email);
        return AuthTokenResponse.of(accessToken, savedRefreshToken, user);
    }



    @Transactional(readOnly = true)
    public void logoutUser(String email, String accessToken) {

        log.info("[유저 로그아웃 시작] : {}", email);

        userSessionRevocationService.revokeAll(email, accessToken);


        log.info("[유저 로그아웃 완료] : {}", email);

    }

    private void validateKakaoLogin(UserEntity userEntity) {
        if (userEntity.getLoginProvider().equals(LOCAL)) {
            throw new CustomException(PROVIDER_NOT_MATCH);
        }
    }

    private void validateLocalLogin(UserEntity userEntity) {
        if (userEntity.getLoginProvider().equals(KAKAO)) {
            throw new CustomException(PROVIDER_NOT_MATCH);
        }
    }

    private void validatePassword(String password, UserEntity user) {
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
    }

    private AuthTokenResponse issueTokens(UserEntity user) {

        String accessToken = issueAccessToken(user);

        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        authTokenStore.saveRefreshToken(user.getEmail(), refreshToken, tokenProvider.getRefreshTokenExpirationMillis());

        return AuthTokenResponse.of(accessToken, refreshToken, user);
    }

    private String issueAccessToken(UserEntity user) {

        return tokenProvider.createAccessToken(user.getEmail(), user.getName(), user.getUserType());

    }



    private UserEntity getActiveUser(String email) {
        return userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void validateReissue(String refreshToken, String savedRefreshToken) {
        if (savedRefreshToken == null) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(INVALID_TOKEN);
        }

    }

    private void validateNotBlacklisted(String email) {
        String blacklist = redisService.getData(BLACKLIST_PREFIX + email);

        if (blacklist != null) {
            throw new CustomException(BLACKLIST_USER);
        }
    }

}
