package com.example.basketballmatching.auth.oauth2.dto;

import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;

public record OAuthTicketPayload(
        OAuthFlowType flowType,
        OAuthProvider provider,
        String providerUserId,
        String email
) {

    public static OAuthTicketPayload login(String email) {
        return new OAuthTicketPayload(
                OAuthFlowType.LOGIN,
                OAuthProvider.KAKAO,
                null,
                email
        );
    }

    public static OAuthTicketPayload signup(
            OAuthProvider provider,
            String providerUserId,
            String email
    ) {
        return new OAuthTicketPayload(
                OAuthFlowType.SIGNUP,
                provider,
                providerUserId,
                email
        );
    }

}
