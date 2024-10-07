package com.example.basketballproject.auth.security;


import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.global.exception.ErrorCode;
import com.example.basketballproject.user.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static com.example.basketballproject.global.exception.ErrorCode.EXPIRED_TOKEN;

@Slf4j
@Component
public class JwtTokenExtract {

    public UserEntity currentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
        authentication.getPrincipal() == null) {
            throw new CustomException(EXPIRED_TOKEN);
        }

        if (authentication.getPrincipal() instanceof  UserEntity) {
            return (UserEntity) authentication.getPrincipal();
        } else {
            throw new CustomException(EXPIRED_TOKEN);
        }

    }

}
