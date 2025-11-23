package com.example.basketballmatching.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyEmailDto {


    private String email;
    private String code;

}
