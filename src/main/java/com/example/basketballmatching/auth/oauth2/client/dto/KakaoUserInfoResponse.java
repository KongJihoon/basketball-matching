package com.example.basketballmatching.auth.oauth2.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            @JsonProperty("profile_nickname_needs_agreement")
            Boolean profileNicknameNeedsAgreement,

            Profile profile,

            @JsonProperty("has_email")
            Boolean hasEmail,

            @JsonProperty("email_needs_agreement")
            Boolean emailNeedsAgreement,

            @JsonProperty("is_email_valid")
            Boolean emailValid,

            @JsonProperty("is_email_verified")
            Boolean emailVerified,

            String email
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            @JsonProperty("is_default_nickname")
            Boolean defaultNickname,

            String nickname
    ) {}

}
