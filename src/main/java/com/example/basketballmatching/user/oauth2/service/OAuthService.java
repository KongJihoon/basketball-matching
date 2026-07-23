package com.example.basketballmatching.user.oauth2.service;

import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OAuthService {

    AuthTokenResponse kakaoLogin(String code) throws JsonProcessingException;

    String responseUrl();
}
