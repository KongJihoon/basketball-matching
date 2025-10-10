package com.example.basketballmatching.auth.service.impl;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // given: 테스트용 유저 데이터 삽입
        UserEntity user = UserEntity.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("name")
                .nickname("name")
                .birth(LocalDate.of(1997,7,24))
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .userType(UserType.USER)
                .build();

        userRepository.save(user);
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginUser_Success() {
        // given

        TokenDto tokenDto = authService.loginUser("test@example.com", "Test1234!");

        // when

        // then

        assertThat(tokenDto.getAccessToken()).isNotBlank();
        assertThat(tokenDto.getRefreshToken()).isNotBlank();
        assertThat(tokenDto.getUserDto().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("로그인 실패 테스트")
    void loginUser_PasswordNotMatch() {
        // given

        // when

        // then

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.loginUser("test@example.com", "Test@123111!"));


        assertEquals(ErrorCode.PASSWORD_NOT_MATCH, exception.getErrorCode());

    }


}