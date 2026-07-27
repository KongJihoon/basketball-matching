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

    @Transactional
    public OAuthAccountDecision resolveKakaoAccount(Long kakaoUserId, String email) {

        validateKakaoUserId(kakaoUserId);

        String providerUserId = String.valueOf(kakaoUserId);

        Optional<OAuthAccountEntity> oAuthAccount = oAuthAccountRepository.findWithUserByProviderAndProviderUserId(KAKAO, providerUserId);

        if (oAuthAccount.isPresent()) {
            UserEntity user = validateActiveUser(oAuthAccount.get().getUserEntity());

            return OAuthAccountDecision.login(user.getEmail());
        }

        validateEmail(email);

        Optional<UserEntity> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            return OAuthAccountDecision.signup(email);
        }

        UserEntity user = validateLinkableUser(existingUser.get());

        OAuthAccountEntity newOAuthAccount = OAuthAccountEntity.create(
                user, KAKAO, providerUserId
        );

        oAuthAccountRepository.save(newOAuthAccount);

        return OAuthAccountDecision.login(user.getEmail());

    }

    @Transactional
    public String completeSignUp(OAuthTicketPayload payload, OAuthSignUpRequest request) {

        validateSignUpPayload(payload);
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



    private UserEntity validateLinkableUser(UserEntity userEntity) {
        validateActiveUser(userEntity);

        if (userEntity.getLoginProvider().equals(LOCAL)) {
            throw new CustomException(OAUTH_ACCOUNT_LINK_REQUIRED);
        }

        return userEntity;
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
