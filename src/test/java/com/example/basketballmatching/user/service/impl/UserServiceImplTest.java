package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    @Test
    @DisplayName("회원가입 테스트")
    void signUpTest() {
        // given

        SignUpDto.Request request = SignUpDto.Request.builder()
                .email("test@test.com")
                .password("Test@123")
                .checkPassword("Test@123")
                .name("test")
                .phone("010-1111-1111")
                .birth(LocalDate.now())
                .position("GUARD")
                .build();
        // when

        SignUpDto.Response response = userService.signUp(request);

        UserEntity userEntity = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        // then

        assertEquals(response.getEmail(), userEntity.getEmail());

    }


}