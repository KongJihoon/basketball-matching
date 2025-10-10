package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.auth.dto.TokenDto;

public interface AuthService {

    TokenDto loginUser(String email, String password);

}
