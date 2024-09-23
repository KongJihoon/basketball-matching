package com.example.basketballproject.auth.service;

import com.example.basketballproject.auth.dto.SignInDto;
import com.example.basketballproject.auth.dto.SignUpDto;
import com.example.basketballproject.auth.dto.TokenDto;
import com.example.basketballproject.auth.security.TokenProvider;
import com.example.basketballproject.global.exception.CustomException;
import com.example.basketballproject.user.dto.UserDto;
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

    private final TokenProvider tokenProvider;


    public UserDto signUp(SignUpDto.Request request) {

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

        return UserDto.fromEntity(user);
    }


    public UserDto loginUser(SignInDto.Request request) {

        UserEntity userEntity = userRepository.findByLoginIdAndDeletedDateTimeNull(request.getLoginId())
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            throw new CustomException(PASSWORD_NOT_MATCH);
        }

        if (!userEntity.isEmailAuth()) {
            throw new CustomException(CONFIRM_EMAIL_AUTH);
        }

        return UserDto.fromEntity(userEntity);

    }

    public TokenDto getToken(UserDto userDto) {

        String accessToken = tokenProvider.createAccessToken(userDto.getLoginId(), userDto.getEmail(), UserType.valueOf(userDto.getUserType()));

        String refreshToken = tokenProvider.createRefreshToken(userDto.getLoginId());

        return new TokenDto(userDto.getLoginId(), accessToken, refreshToken);
    }


    private void checkForDuplicateUser(SignUpDto.Request request) {
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
