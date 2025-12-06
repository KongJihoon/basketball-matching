package com.example.basketballmatching.user.service;

import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.user.dto.ChangePasswordDto;
import com.example.basketballmatching.user.dto.EditUserDto;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;

public interface UserService {


    CommonResponse<SignUpDto.Response> signUp(SignUpDto.Request request);


    CheckResponse checkEmail(String email);
    CheckResponse checkNickname(String nickname);
    CommonResponse<UserDto> getUserInfo(Long userId);



    CommonResponse<UserDto> editUserInfo(Long userId, EditUserDto editUserDto);

    CheckResponse verifyPasswordCode(String email, String code);


    CheckResponse resetPassword(String email, String newPassword, String checkNewPassword);

    CheckResponse changePassword(Long userId, ChangePasswordDto request);

    CheckResponse deleteUser(Long userId, String token);
}
