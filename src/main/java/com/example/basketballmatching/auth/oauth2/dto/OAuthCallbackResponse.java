package com.example.basketballmatching.auth.oauth2.dto;

import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;

/**
 * 카카오 OAuth 인증 결과를 클라이언트에 전달하는 응답
 *
 * flowType을 통해 기존 회원 로그인과 신규 회원가입 흐름을 구분한다.
 */
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
