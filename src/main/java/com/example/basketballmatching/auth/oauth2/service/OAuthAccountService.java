package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.dto.OAuthAccountDecision;
import com.example.basketballmatching.auth.oauth2.dto.OAuthSignUpRequest;
import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketPayload;
import com.example.basketballmatching.auth.oauth2.repository.OAuthAccountRepository;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.LoginProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static com.example.basketballmatching.auth.oauth2.type.OAuthProvider.KAKAO;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static com.example.basketballmatching.user.type.LoginProvider.LOCAL;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final OAuthAccountRepository oAuthAccountRepository;

    private final UserRepository userRepository;

    /**
     * 카카오 사용자 ID와 이메일을 이용해
     * 기존 회원 로그인 또는 신규 회원가입 흐름을 결정하는 분기
     *
     * 이미 카카오 계정이 연결되어있다면 로그인으로 처리하고
     * 연결 정보가 없다면 동일한 이메일의 사용자 존재 여부를 확인한다.
     */
    @Transactional(readOnly = true)
    public OAuthAccountDecision resolveKakaoAccount(Long kakaoUserId, String email) {


        // 카카오 사용자를 식별할 Provider 사용자 ID가 존재하는지 검증
        validateKakaoUserId(kakaoUserId);


        String providerUserId = String.valueOf(kakaoUserId);

        /*
         * Provider 종류와 Provider 사용자 ID로 이미 서비스에 연결된 OAuth계정을 조회한다.
         * 연관된 UserEntity도 함께 사용되므로 Fetch Join으로 조회
         */
        Optional<OAuthAccountEntity> oAuthAccount = oAuthAccountRepository.findWithUserByProviderAndProviderUserId(KAKAO, providerUserId);

        // 이미 카카오 계정이 연결돼 있다면 탈퇴 여부 확인 후 로그인 흐름 반환
        if (oAuthAccount.isPresent()) {
            UserEntity user = validateActiveUser(oAuthAccount.get().getUserEntity());

            return OAuthAccountDecision.login(user.getEmail());
        }

        // 기존 카카오 계정 정보가 없으므로 신규 가입 또는 기존 사용자 연결에 사용할 이메일을 검증
        validateEmail(email);

        // 카카오에서 받은 이메일과 동일한 사용자가 있는 지 조회
        Optional<UserEntity> existingUser = userRepository.findByEmail(email);

        // 동일한 사용자가 있다면 계정 연결 요구 예외 발생
        if (existingUser.isPresent()) {
            validateActiveUser(existingUser.get());

            throw new CustomException(OAUTH_ACCOUNT_LINK_REQUIRED);
        }



        return OAuthAccountDecision.signup(email);

    }

    /**
     * OAuth Ticket의 인증 정보와 사용자 추가 정보로
     * 신규 사용자 및 OAuth 계정 연결 정보를 생성한다.
     *
     * 사용자 저장과 OAuth 계정 생성은 하나의 트랜잭션에서 처리한다.
     */
    @Transactional
    public String completeSignUp(OAuthTicketPayload payload, OAuthSignUpRequest request) {

        // 회원가입에 필요한 OAuth 정보가 Payload에 저장되어 있는지 검증
        validateSignUpPayload(payload);

        // OAuth 계정, 이메일, 닉네임이 기존 데이터와 중복되지 않는지 검증
        validateSignUpAvailability(payload, request.nickname());


        LoginProvider loginProvider = convertLoginProvider(payload.provider());

        UserEntity user = UserEntity.createOAuth(
                payload.email(),
                request.nickname(),
                request.name(),
                request.birth(),
                request.phone(),
                request.address(),
                request.position(),
                request.genderType(),
                loginProvider
        );

        UserEntity savedUser = userRepository.save(user);

        OAuthAccountEntity oAuthAccount = OAuthAccountEntity.create(user, payload.provider(), payload.providerUserId());

        oAuthAccountRepository.save(oAuthAccount);

        return savedUser.getEmail();


    }

    private void validateSignUpPayload(OAuthTicketPayload payload) {
        if (payload.provider() == null || !StringUtils.hasText(payload.providerUserId())
        || !StringUtils.hasText(payload.email())) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

    }

    private void validateSignUpAvailability(OAuthTicketPayload payload, String nickname) {
        if (oAuthAccountRepository.existsByProviderAndProviderUserId(payload.provider(), payload.providerUserId())) {
            throw new CustomException(OAUTH_ACCOUNT_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(payload.email())) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }
    }

    private LoginProvider convertLoginProvider(OAuthProvider provider) {
        return switch (provider) {
            case KAKAO -> LoginProvider.KAKAO;
        };
    }




    private UserEntity validateActiveUser(UserEntity userEntity) {

        if (userEntity.getDeletedDateTime() != null) {
            throw new CustomException(OAUTH_WITHDRAWN_USER);
        }

        return userEntity;

    }



    private void validateKakaoUserId(Long kakaoUserId) {
        if (kakaoUserId == null) {
            throw new CustomException(OAUTH_PROVIDER_ID_NOT_FOUND);
        }
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new CustomException(OAUTH_EMAIL_NOT_FOUND);
        }
    }

}
