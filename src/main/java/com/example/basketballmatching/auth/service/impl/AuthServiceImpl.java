package com.example.basketballmatching.auth.service.impl;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.auth.service.UserSessionRevocationService;
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
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;

    private final RedisService redisService;
    private final UserSessionRevocationService userSessionRevocationService;


    @Override
    @Transactional
    public TokenDto loginUser(String email, String password) {

        log.info("[유저 로그인 시작]: {}", email);



        UserEntity userEntity = getUser(email);



        // 로그인 형식 검사
        validateLocalLogin(userEntity);

        // 비밀번호 유효성 검사
        validatePassword(password, userEntity);

        // 블랙리스트 유저 검사
        validateNotBlackList(userEntity.getEmail());


        UserDto userDto = UserDto.fromEntity(userEntity);


        String accessToken = issueAccessToken(userDto);
        log.info("accessToken 생성 완료.");

        String refreshToken = issueRefreshToken(userDto);
        log.info("refreshToken 생성 완료");

        log.info("[유저 로그인 완료] email : {}", userDto.getEmail());

        return new TokenDto(accessToken, refreshToken, userDto);
    }




    @Override
    @Transactional(readOnly = true)
    public TokenDto kakaoLogin(String email) {

        log.info("[카카오 로그인 검증 시작] email : {}", email);

        UserEntity userEntity = getUser(email);

        // 로그인 형식 검사
        validateKakaoLogin(userEntity);

        UserDto userDto = UserDto.fromEntity(userEntity);

        String accessToken = issueAccessToken(userDto);

        String refreshToken = issueRefreshToken(userDto);

        log.info("[카카오 로그인 완료] email : {}", userDto.getEmail());

        return new TokenDto(accessToken, refreshToken, userDto);
    }


    @Override
    @Transactional(readOnly = true)
    public TokenDto reissue(String email, String refreshToken) {

        log.info("[토큰 재발급 시작]: {}", email);

        String redisToken = redisService.getData("refreshToken:" + email);


        // 재발금 유효성 검사
        validateReissue(email, refreshToken, redisToken);

        UserEntity userEntity = getUser(email);

        // refreshToken 검증
        tokenProvider.validateRefreshToken(refreshToken);

        UserDto userDto = UserDto.fromEntity(userEntity);

        String reissuedAccessToken = issueAccessToken(userDto);


        return new TokenDto(reissuedAccessToken, redisToken, userDto);
    }



    @Override
    @Transactional(readOnly = true)
    public CheckResponse logoutUser(String email, String token) {

        log.info("[유저 로그아웃 시작] : {}", email);

        userSessionRevocationService.revokeAll(email, token);


        log.info("[유저 로그아웃 완료] : {}", email);

        return CheckResponse.of(true, "로그아웃 완료되었습니다.");
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

    private String issueAccessToken(UserDto userDto) {

        return tokenProvider.createAccessToken(userDto.getEmail(), userDto.getName(), userDto.getUserType());

    }

    private String issueRefreshToken(UserDto userDto) {
        return tokenProvider.createRefreshToken(userDto.getEmail());
    }


    private UserEntity getUser(String email) {
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

}
