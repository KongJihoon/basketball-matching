package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.user.dto.SignUpDto;

public interface UserService {


    ApiResponse<SignUpDto.Response> signUp(SignUpDto.Request request);


    CheckResponse checkEmail(String email);
    CheckResponse checkNickname(String nickname);

}
