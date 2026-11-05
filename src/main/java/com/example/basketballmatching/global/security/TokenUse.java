package com.example.basketballmatching.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenUse {

    ACCESS("access"),
    REFRESH("refresh");

    private final String claimValue;

}
