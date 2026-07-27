package com.example.basketballmatching.auth.oauth2.dto;

import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;

public record OAuthCallbackResponse(
        OAuthFlowType flowType,
        String ticket,
        String email,
        String nickname
) {

    public static OAuthCallbackResponse login(String ticket) {
        return new OAuthCallbackResponse(
                OAuthFlowType.LOGIN,
                ticket,
                null,
                null
        );
    }

    public static OAuthCallbackResponse signup(
            String ticket,
            String email,
            String nickname
    ) {
        return new OAuthCallbackResponse(
                OAuthFlowType.SIGNUP,
                ticket,
                email,
                nickname
        );
    }
}
