package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.dto.UserDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;


    @Override
    @Transactional
    public ApiResponse<SignUpDto.Response> signUp(SignUpDto.Request request) {

        log.info("유저 회원가입 시작 : {}", request.getEmail());

        boolean existsByEmail = userRepository.existsByEmail(request.getEmail());

        boolean existsByNickname = userRepository.existsByNickname(request.getNickname());

        validationByUser(request, existsByEmail, existsByNickname);

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        UserEntity userEntity = SignUpDto.Request.toEntity(request);

        userRepository.save(userEntity);

        log.info("유저 회원가입 완료");

        return ApiResponse.of("회원가입에 성공하였습니다.", SignUpDto.Response.fromDto(UserDto.fromEntity(userEntity)));
    }

    @Override
    public CheckResponse checkEmail(String email) {

        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        return CheckResponse.of(true, "사용가능한 이메일입니다.");
    }




    private static void validationByUser(SignUpDto.Request request, boolean existsByEmail, boolean existsByNickname) {
        if (existsByEmail) {
            throw new CustomException(ALREADY_EXIST_EMAIL);
        }

        if (existsByNickname) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }

        if (!request.getPassword().equals(request.getCheckPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }
    }
}
