package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.dto.OAuthAccountDecision;
import com.example.basketballmatching.auth.oauth2.repository.OAuthAccountRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
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
