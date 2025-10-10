package com.example.basketballmatching.auth.dto;


import com.example.basketballmatching.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenDto {

    private String accessToken;

    private String refreshToken;

    private UserDto userDto;

}
