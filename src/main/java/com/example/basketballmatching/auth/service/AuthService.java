package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.blacklist.service.BlackListStore;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.security.TokenProvider;
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



    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;


    private final AuthTokenStore authTokenStore;

    private final UserSessionRevocationService userSessionRevocationService;

    private final BlackListStore blackListStore;


    /**
     * 로컬 회원의 이메일과 비밀번호를 검증하고
     * 서비스 이용에 필요한 AccessToken과 RefreshToken 발급
     *
     * DB 조회는 읽기 전용 트랜잭션으로 수행
     * 발급한 RefreshToken은 별도 저장소인 Redis에 저장한다.
     */
    @Transactional(readOnly = true)
    public AuthTokenResponse login(String email, String password) {

        log.info("[유저 로그인 시작]: {}", email);

        // 탈퇴하지 않은 활성 사용자를 이메일로 조회한다.
        UserEntity user = getActiveUser(email);


        // 카카오 계정이 로컬 계정 로그인에 접근하면 차단한다.
        validateLocalLogin(user);

        /*
         * 입력받은 평문 비밀번호와 DB의 해시값을 비교한다.
         * 비밀번호를 복호화 하지 않고 PasswordEncoder.matches()를 사용.
         */
        validatePassword(password, user);

        // 현재 제재 중인 사용자의 로그인을 차단한다.
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


    /**
     * 유효한 Refresh Token을 이용하여 새로운 AccessToken 발급
     *
     * Redis에 저장된 RefreshToken과 요청 토큰의 일치여부를 검사하고
     * 현재 사용자 상태를 검증한 뒤에 새로운 AccessToken만 발급한다.
     */
    @Transactional(readOnly = true)
    public AuthTokenResponse reissue(String refreshToken) {

        // 요청 토큰의 서명과 만료 여부를 검증한다.
        // Redis의 저장된 값과 비교하기 전에 만료된 RefreshToken의 접근을 제한한다.
        tokenProvider.validateRefreshToken(refreshToken);

        String email = tokenProvider.getEmailFromToken(refreshToken);

        log.info("[토큰 재발급 시작]: {}", email);


        String savedRefreshToken = authTokenStore.getRefreshToken(email);

        // 요청 토큰과 Redis의 저장된 RefreshToken의 일치 여부를 검사한다.
        validateReissue(refreshToken, savedRefreshToken);

        // 재발급 유저의 탈퇴 가능성으로 활성 유저인지 검증
        UserEntity user = getActiveUser(email);

        // 유저가 블랙리스트인지 검사
        validateNotBlacklisted(email);


        // 기존 RefreshToken은 유지하고 새로운 AccessToken을 발급하고 반환한다.
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

    /**
     * 사용자가 입력한 평문 비밀번호와
     * DB에 저장된 단방향 해시값을 비교한다.
     */
    private void validatePassword(String password, UserEntity user) {
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
    }

    /**
     * 인증이 완료된 사용자에게 AccessToken과 RefreshToken 발급
     * RefreshToken은 재발급 시 서버에서 검증할 수 있도록 Redis에 저장한다.
     */
    private AuthTokenResponse issueTokens(UserEntity user) {

        String accessToken = issueAccessToken(user);

        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        /*
         * RefreshToken을 이메일 기준으로 Redis에 저장한다.
         * Redis TTL은 RefreshToken JWT 유효시간과 동일하게 설정한다.
         */
        authTokenStore.saveRefreshToken(user.getEmail(), refreshToken, tokenProvider.getRefreshTokenExpirationMillis());

        return AuthTokenResponse.of(accessToken, refreshToken, user);
    }

    /**
     * Access Token에 인증과 인가에 필요한 사용자 정보를 담아 발급한다.
     */
    private String issueAccessToken(UserEntity user) {

        return tokenProvider.createAccessToken(user.getEmail(), user.getName(), user.getUserType());

    }



    private UserEntity getActiveUser(String email) {
        return userRepository.findByEmailAndDeletedDateTimeIsNull(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    /**
     * 요청받은 RefreshToken이 서버에서 관리하는
     * 현재 유효한 RefreshToken과 일치하는지 확인한다.
     */
    private void validateReissue(String refreshToken, String savedRefreshToken) {
        if (savedRefreshToken == null) {
            throw new CustomException(NOT_FOUND_TOKEN);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(INVALID_TOKEN);
        }

    }

    /**
     * 현재 제재 중인 사용자의 로그인을 차단한다.
     */
    private void validateNotBlacklisted(String email) {

        if (blackListStore.isBlacklisted(email)) {
            throw new CustomException(BLACKLIST_USER);
        }
    }

}
