package com.example.basketballmatching.auth.oauth2.dto;

import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;

public record OAuthAccountDecision(
        OAuthFlowType flowType,
        String email
) {

    public static OAuthAccountDecision login(String email) {
        return new OAuthAccountDecision(
                OAuthFlowType.LOGIN,
                email
        );
    }

    public static OAuthAccountDecision signup(String email) {

        return new OAuthAccountDecision(
                OAuthFlowType.SIGNUP,
                email
        );
    }

}
