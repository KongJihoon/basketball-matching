package com.example.basketballmatching.auth.service.impl;

import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.security.TokenProvider;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.UserType;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.NOT_FOUND_TOKEN;
import static com.example.basketballmatching.global.exception.ErrorCode.PASSWORD_NOT_MATCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RedisService redisService;


    @InjectMocks
    private AuthServiceImpl authService;

    UserEntity userEntity;

    @BeforeEach
    public void setUp() {
        userEntity = UserEntity.builder()
                .userId(1L)
                .email("test@test.com")
                .name("test")
                .password("ENCODED")
                .userType(UserType.USER)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();
    }





    @Test
    @DisplayName("유저 로그인 테스트")
    void loginUserTest() {
        // given

        String email = "test@test.com";

        String password = "test@123";

        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .email("test@test.com")
                .name("test")
                .password("ENCODED")
                .userType(UserType.USER)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();



        when(userRepository.findByEmailAndDeletedDateTimeIsNull(eq(email))).thenReturn(Optional.of(userEntity));

        when(passwordEncoder.matches(eq(password), eq(userEntity.getPassword()))).thenReturn(true);

        when(redisService.getData("blackList:" + email)).thenReturn(null);

        when(tokenProvider.createAccessToken(eq(email), eq(userEntity.getName()), eq(userEntity.getUserType()))).thenReturn("accessToken");

        when(tokenProvider.createRefreshToken(eq(email))).thenReturn("refreshToken");


        // when



        TokenDto tokenDto = authService.loginUser(email, password);

        // then

        assertNotNull(tokenDto);
        assertEquals("accessToken", tokenDto.getAccessToken());
        assertEquals("refreshToken", tokenDto.getRefreshToken());

        assertNotNull(tokenDto.getUserDto());
        assertEquals(email, tokenDto.getUserDto().getEmail());

        verify(userRepository).findByEmailAndDeletedDateTimeIsNull(eq(email));
        verify(passwordEncoder).matches(eq(password), eq("ENCODED"));
        verify(redisService).getData(eq("blackList:" + email));

        verify(tokenProvider).createAccessToken(eq(email), eq("test"), eq(UserType.USER));
        verify(tokenProvider).createRefreshToken(eq(email));

        verifyNoMoreInteractions(userRepository, passwordEncoder, tokenProvider,redisService);


    }


    @Test
    @DisplayName("유저 로그인 실패 테스트 - PASSWORD_NOT_MATCH")
    void loginUserFailTest_PASSWORD_NOT_MATCH() {
        // given
        String email = "test@test.com";

        String password = "test@123";

        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .email("test@test.com")
                .name("test")
                .password("ENCODED")
                .userType(UserType.USER)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();


        when(userRepository.findByEmailAndDeletedDateTimeIsNull(email)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches(password, userEntity.getPassword())).thenReturn(false);

        // when

        CustomException exception = assertThrows(CustomException.class, () -> authService.loginUser(email, password));

        // then

        assertEquals(PASSWORD_NOT_MATCH, exception.getErrorCode());

        verify(redisService, never()).getData(any());
        verify(tokenProvider, never()).createAccessToken(any(), any(), any());
        verify(tokenProvider, never()).createRefreshToken(any());



    }

    @Test
    @DisplayName("토큰 재발급 테스트")
    void reissueTest() {
        // given

        String email = "test@test.com";

        String accessToken = "accessToken";

        String refreshToken = "refreshToken";

        when(redisService.getData("refreshToken:" + userEntity.getEmail())).thenReturn(refreshToken);

        Claims claims = mock(Claims.class);

        when(tokenProvider.parseToken("refreshToken")).thenReturn(claims);

        when(claims.getSubject()).thenReturn(email);

        when(userRepository.findByEmailAndDeletedDateTimeIsNull(email)).thenReturn(Optional.of(userEntity));

        when(tokenProvider.createAccessToken(email, userEntity.getName(), userEntity.getUserType())).thenReturn(accessToken);


        // when

        TokenDto tokenDto = authService.reissue(email, refreshToken);


        // then

        assertNotNull(tokenDto);
        assertEquals(accessToken, tokenDto.getAccessToken());
        assertEquals(refreshToken, tokenDto.getRefreshToken());

        assertEquals(email, tokenDto.getUserDto().getEmail());

        verify(redisService).getData("refreshToken:" + email);
        verify(tokenProvider).parseToken(refreshToken);
        verify(userRepository).findByEmailAndDeletedDateTimeIsNull(email);
        verify(tokenProvider).createAccessToken(any(), any(), any());

        verifyNoMoreInteractions(redisService, userRepository);


    }

    @Test
    @DisplayName("토큰 재발급 실패 테스트 - NOT_FOUND_TOKEN")
    void reissueFailTest_NOT_FOUND_TOKEN() {
        // given
        String email = "test@test.com";



        String refreshToken = "refreshToken";

        when(redisService.getData(eq("refreshToken:" + email))).thenReturn(null);


        // when

        CustomException exception = assertThrows(CustomException.class, () -> authService.reissue(email, refreshToken));

        // then

        assertEquals(NOT_FOUND_TOKEN, exception.getErrorCode());


        verify(redisService).getData(eq("refreshToken:" + email));
        verify(tokenProvider, never()).parseToken(any());
        verify(userRepository, never()).findByEmailAndDeletedDateTimeIsNull(any());
        verify(tokenProvider, never()).createAccessToken(any(), any(), any());

    }


    @Test
    @DisplayName("유저 로그아웃 테스트")
    void logoutUserTest() {
        // given

        String email = "test@test.com";
        String accessToken = "accessToken";
        long remainTime = 1000L;

        when(tokenProvider.getRemainingTime(eq(accessToken))).thenReturn(remainTime);


        // when

        CheckResponse checkResponse = authService.logoutUser(email, accessToken);

        // then

        assertTrue(checkResponse.isSuccess());
        assertEquals("로그아웃 완료되었습니다.", checkResponse.getMessage());

        verify(redisService).setDataExpireMillis(eq("logout:access:" + accessToken),
                eq("LOGOUT"), eq(remainTime));

        verify(redisService).deleteData(eq("refreshToken:" + email));

    }

    @Test
    @DisplayName("유저 로그아웃 실패 테스트 - NOT_FOUND_TOKEN")
    void logoutUserFailTest_NOT_FOUND_TOKEN() {
        // given
        String email = "test@test.com";


        // when

        CustomException exception = assertThrows(CustomException.class, () -> authService.logoutUser(email, null));

        // then

        assertEquals(NOT_FOUND_TOKEN, exception.getErrorCode());
        verifyNoInteractions(redisService, tokenProvider);

    }



}