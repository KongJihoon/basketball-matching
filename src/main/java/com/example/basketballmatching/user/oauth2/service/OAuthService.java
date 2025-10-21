package com.example.basketballmatching.user.oauth2.service;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.oauth2.dto.KakaoDto;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OAuthService {

    TokenDto kakaoLogin(String code) throws JsonProcessingException;

    String responseUrl();
}
