package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static com.example.basketballmatching.user.type.LoginProvider.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX =
            "refreshToken:";

    private static final String BLACKLIST_PREFIX =
            "blackList:";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;

    private final RedisService redisService;
    private final UserSessionRevocationService userSessionRevocationService;


    @Transactional
    public TokenDto loginUser(String email, String password) {

        log.info("[유저 로그인 시작]: {}", email);



        UserEntity user = getActiveUser(email);



        // 로그인 형식 검사
        validateLocalLogin(user);

        // 비밀번호 유효성 검사
        validatePassword(password, user);

        // 블랙리스트 유저 검사
        validateNotBlackList(user.getEmail());


        TokenDto tokenDto = issueTokens(user);

        log.info("[유저 로그인 완료] email : {}", email);

        return tokenDto;
    }




    @Transactional(readOnly = true)
    public TokenDto kakaoLogin(String email) {

        log.info("[카카오 로그인 검증 시작] email : {}", email);

        UserEntity user = getActiveUser(email);

        // 로그인 형식 검사
        validateKakaoLogin(user);

        validateNotBlacklisted(email);

        TokenDto token = issueTokens(user);

        log.info("[카카오 로그인 완료] email : {}", email);

        return token;
    }


    @Transactional(readOnly = true)
    public TokenDto reissue(String email, String refreshToken) {

        log.info("[토큰 재발급 시작]: {}", email);

        String redisToken = redisService.getData("refreshToken:" + email);


        // 재발금 유효성 검사
        validateReissue(email, refreshToken, redisToken);

        UserEntity user = getActiveUser(email);

        // refreshToken 검증
        tokenProvider.validateRefreshToken(refreshToken);

        UserDto userDto = UserDto.fromEntity(user);

        String reissuedAccessToken = issueAccessToken(userDto);

        log.info("[토큰 재발급 완료] email={}", email);
        return new TokenDto(reissuedAccessToken, redisToken, userDto);
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

    private void validateNotBlackList(String email) {

        String data = redisService.getData("blackList:" + email);

        if (data != null) {
            throw new CustomException(BLACKLIST_USER);
        }

    }

    private void validatePassword(String password, UserEntity user) {
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
    }

    private TokenDto issueTokens(UserEntity user) {
        UserDto userDto = UserDto.fromEntity(user);

        String accessToken = issueAccessToken(userDto);

        String refreshToken = issueRefreshToken(userDto);

        return new TokenDto(accessToken, refreshToken, userDto);
    }

    private String issueAccessToken(UserDto userDto) {

        return tokenProvider.createAccessToken(userDto.getEmail(), userDto.getName(), userDto.getUserType());

    }

    private String issueRefreshToken(UserDto userDto) {
        return tokenProvider.createRefreshToken(userDto.getEmail());
    }


    private UserEntity getActiveUser(String email) {
        return userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private void validateReissue(String email, String refreshToken, String redisToken) {
        if (redisToken == null) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }

        if (!redisToken.equals(refreshToken)) {
            throw new CustomException(INVALID_TOKEN);
        }

        String userEmail = tokenProvider.parseToken(refreshToken).getSubject();

        if (!userEmail.equals(email)) {
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
