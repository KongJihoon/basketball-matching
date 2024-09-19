package com.example.basketballproject.auth.service;

import com.example.basketballproject.auth.dto.SignUpDto;
import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.repository.UserRepository;
import com.example.basketballproject.user.type.GenderType;
import com.example.basketballproject.user.type.Position;
import com.example.basketballproject.user.type.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.example.basketballproject.global.exception.ErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;


    public SignUpDto signUp(SignUpDto request) {

        checkForDuplicateUser(request);

        UserEntity user = userRepository.save(
                UserEntity.builder()
                        .loginId(request.getLoginId())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .email(request.getEmail())
                        .name(request.getName())
                        .nickname(request.getNickname())
                        .birth(request.getBirth())
                        .userType(UserType.USER)
                        .genderType(GenderType.valueOf(request.getGenderType()))
                        .position(Position.valueOf(request.getPosition()))
                        .createdDateTime(LocalDateTime.now())
                        .build()
        );

        return SignUpDto.toEntity(user);
    }

    private void checkForDuplicateUser(SignUpDto request) {
        if (userRepository.existsByEmailAndDeletedDateTimeNull(request.getEmail())) {
            throw new CustomException(ALREADY_EXIST_USER);
        }

        if (userRepository.existsByNicknameAndDeletedDateTimeNull(request.getNickname())) {
            throw new CustomException(ALREADY_EXIST_NICKNAME);
        }

        if (userRepository.existsByLoginIdAndDeletedDateTimeNull(request.getLoginId())) {
            throw new CustomException(ALREADY_EXIST_LOGINID);
        }
    }

}
