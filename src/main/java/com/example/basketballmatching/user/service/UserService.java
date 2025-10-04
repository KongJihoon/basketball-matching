package com.example.basketballmatching.user.service;

import com.example.basketballmatching.user.dto.SignUpDto;

public interface UserService {


    SignUpDto.Response signUp(SignUpDto.Request request);

}
