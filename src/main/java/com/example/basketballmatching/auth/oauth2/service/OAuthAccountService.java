package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.domain.OAuthAccountEntity;
import com.example.basketballmatching.auth.oauth2.dto.KakaoDto;
import com.example.basketballmatching.auth.oauth2.repository.OAuthAccountRepository;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.example.basketballmatching.auth.oauth2.type.OAuthProvider.KAKAO;
import static com.example.basketballmatching.user.type.LoginProvider.LOCAL;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final OAuthAccountRepository oAuthAccountRepository;

    private final UserRepository userRepository;

    @Transactional
    public String findOrCreateKakaoUserEmail(Long kakaoUserId, String email, String nickname) {

        validateKakaoUserId(kakaoUserId);

        String providerUserId = String.valueOf(kakaoUserId);

        Optional<OAuthAccountEntity> oAuthAccount = oAuthAccountRepository.findWithUserByProviderAndProviderUserId(KAKAO, providerUserId);

        if (oAuthAccount.isPresent()) {
            UserEntity user = validateActiveUser(oAuthAccount.get().getUserEntity());

            return user.getEmail();
        }

        UserEntity user = linkOrCreateKakaoUser(providerUserId, email, nickname);

        return user.getEmail();

    }

    private UserEntity linkOrCreateKakaoUser(String providerUserId, String email, String nickname) {

        UserEntity user = userRepository.findByEmail(email)
                .map(this::validateLinkableUser)
                .orElseGet(() -> createTemporaryKakaoUser(email, nickname));

        OAuthAccountEntity oAuthAccount = OAuthAccountEntity.create(user, KAKAO, providerUserId);


        oAuthAccountRepository.save(oAuthAccount);

        return user;
    }

    private UserEntity createTemporaryKakaoUser(String email, String nickname) {

        KakaoDto.Request request = KakaoDto.Request.builder()
                .email(email)
                .name(nickname)
                .nickname(nickname)
                .build();

        UserEntity user = KakaoDto.Request.toEntity(request);

        return userRepository.save(user);
    }

    private UserEntity validateLinkableUser(UserEntity userEntity) {
        validateActiveUser(userEntity);

        if (userEntity.getLoginProvider().equals(LOCAL)) {
            throw new CustomException(ErrorCode.OAUTH_ACCOUNT_LINK_REQUIRED);
        }

        return userEntity;
    }

    private UserEntity validateActiveUser(UserEntity userEntity) {

        if (userEntity.getDeletedDateTime() != null) {
            throw new CustomException(ErrorCode.OAUTH_WITHDRAWN_USER);
        }

        return userEntity;

    }


    private void validateKakaoUserId(Long kakaoUserId) {
        if (kakaoUserId == null) {
            throw new CustomException(ErrorCode.OAUTH_PROVIDER_ID_NOT_FOUND);
        }
    }
}
