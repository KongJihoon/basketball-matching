package com.example.basketballmatching.auth.oauth2.dto;

import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.auth.oauth2.type.OAuthProvider;

public record OAuthTicketPayload(
        OAuthFlowType flowType,
        OAuthProvider provider,
        String providerUserId,
        String email
) {

    /**
     * 기존 OAuth 회원 로그인에 사용할 Payload 생성
     *
     * 로그인에는 사용자 이메일만 필요하므로 Provider 사용자 ID는 포함하지 않는다.
     */
    public static OAuthTicketPayload login(String email) {
        return new OAuthTicketPayload(
                OAuthFlowType.LOGIN,
                OAuthProvider.KAKAO,
                null,
                email
        );
    }

    /**
     * 신규 OAuth 회원가입에 사용할 Payload 생성
     *
     * 회원가입 완료 시 사용자의 OAuth 계정 연결 정보를 저장해야 하므로
     * Provider와 ProviderId를 함께 보관
     */
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
