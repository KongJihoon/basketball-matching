package com.example.basketballmatching.user.service.impl;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.basketballmatching.global.exception.ErrorCode.ALREADY_EXIST_EMAIL;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {


    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("이메일 중복 확인")
    void checkEmailTest() {
        // given

        String email = "test@test.com";

        when(userRepository.existsByEmail(email)).thenReturn(false);


        // when

        CheckResponse checkResponse = userService.checkEmail(email);


        // then

        assertEquals(true, checkResponse.isSuccess());
        assertEquals("사용가능한 이메일입니다.", checkResponse.getMessage());


    }

    @Test
    @DisplayName("이매일 중복 확인 실패 테스트")
    void checkEmailFailTest_ALREADY_EXIST_EMAIL() {
        // given

        String email = "test@test.com";


        // when
        when(userRepository.existsByEmail(email)).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class, () -> userService.checkEmail(email));

        // then

        assertEquals(ALREADY_EXIST_EMAIL, exception.getErrorCode());

    }

    @Test
    @DisplayName("닉네임 중복 확인 테스트")
    void checkNicknameTest() {
        // given

        String nickname = "test";


        // when

        // then

    }


}