package com.example.basketballmatching.auth.service.impl;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.LoginProvider;
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


    @Override
    @Transactional
    public TokenDto loginUser(String email, String password) {

        log.info("[유저 로그인 시작]: {}", email);

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (userEntity.getLoginProvider().equals(KAKAO)) {
            throw new CustomException(PROVIDER_NOT_MATCH);
        }

        if (!passwordEncoder.matches(password, userEntity.getPassword()) || password == null) {
                throw new CustomException(PASSWORD_NOT_MATCH);
        }




        UserDto userDto = UserDto.fromEntity(userEntity);


        String accessToken = tokenProvider.createAccessToken(userDto.getEmail(), userDto.getName(), userDto.getUserType());
        log.info("accessToken 생성 완료.");

        String refreshToken = tokenProvider.createRefreshToken(userDto.getEmail());
        log.info("refreshToken 생성 완료");

        log.info("[유저 로그인 완료] email : {}", userDto.getEmail());

        return new TokenDto(accessToken, refreshToken, userDto);
    }

    @Override
    @Transactional
    public TokenDto kakaoLogin(String email) {

        log.info("[카카오 로그인 검증 시작] email : {}", email);

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (userEntity.getLoginProvider().equals(LOCAL)) {
            throw new CustomException(PROVIDER_NOT_MATCH);
        }

        UserDto userDto = UserDto.fromEntity(userEntity);

        String accessToken = tokenProvider.createAccessToken(userDto.getEmail(), userDto.getName(), userDto.getUserType());

        String refreshToken = tokenProvider.createRefreshToken(userDto.getEmail());


        log.info("[카카오 로그인 완료] email : {}", userDto.getEmail());

        return new TokenDto(accessToken, refreshToken, userDto);
    }

    @Override
    public TokenDto reissue(String email, String refreshToken) {

        log.info("[토큰 재발급 시작]: {}", email);

        String redisToken = redisService.getData("refreshToken:" + email);


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

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        // refreshToken 검증
        tokenProvider.validateRefreshToken(refreshToken);

        UserDto userDto = UserDto.fromEntity(userEntity);

        String reissuedAccessToken = tokenProvider.createAccessToken(userDto.getEmail(), userDto.getName(), userDto.getUserType());


        return new TokenDto(reissuedAccessToken, redisToken, userDto);
    }

    @Override
    public CheckResponse logoutUser(String email, String token) {

        log.info("[유저 로그아웃 시작] : {}", email);

        if (token == null) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }

        redisService.setDataExpireMillis("logout:access:" + token, "LOGOUT",tokenProvider.getRemainingTime(token));

        redisService.deleteData("refreshToken:" + email);


        log.info("[유저 로그아웃 완료] : {}", email);

        return CheckResponse.of(true, "로그아웃 완료되었습니다.");
    }


}
