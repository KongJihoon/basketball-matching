package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.global.dto.CheckResponse;

public interface AuthService {

    TokenDto loginUser(String email, String password);

    TokenDto reissue(String email, String refreshToken);

    CheckResponse logoutUser(String email, String token);



}
