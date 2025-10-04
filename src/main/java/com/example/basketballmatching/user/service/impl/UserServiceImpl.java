package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.ALREADY_EXIST_EMAIL;
import static com.example.basketballmatching.global.exception.ErrorCode.PASSWORD_NOT_MATCH;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;


    @Override
    @Transactional
    public SignUpDto.Response signUp(SignUpDto.Request request) {

        boolean exists = userRepository.existsByEmail(request.getEmail());

        if (exists) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        if (!request.getPassword().equals(request.getCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        UserEntity userEntity = SignUpDto.Request.toEntity(request);

        userRepository.save(userEntity);

        return SignUpDto.Response.fromDto(UserDto.fromEntity(userEntity));
    }
}
