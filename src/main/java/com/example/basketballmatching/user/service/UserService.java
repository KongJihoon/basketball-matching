package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;

public interface UserService {


    ApiResponse<SignUpDto.Response> signUp(SignUpDto.Request request);


    CheckResponse checkEmail(String email);
    CheckResponse checkNickname(String nickname);
    ApiResponse<UserDto> getUserInfo(Long userId);

    ApiResponse<UserDto> editUserInfo(Long userId, EditUserDto editUserDto);

    CheckResponse verifyPasswordCode(String email, String code);


    CheckResponse resetPassword(String email, String newPassword, String checkNewPassword);

    CheckResponse changePassword(Long userId, ChangePasswordDto request);

    CheckResponse deleteUser(Long userId, String token);
}
