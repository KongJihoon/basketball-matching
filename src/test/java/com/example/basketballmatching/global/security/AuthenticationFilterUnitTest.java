package com.example.basketballmatching.global.security;

import com.example.basketballmatching.auth.service.AuthTokenStore;
import com.example.basketballmatching.blacklist.service.BlackListStore;
import com.example.basketballmatching.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterUnitTest {

    private static final String ACCESS_TOKEN =
            "access-token";

    private static final String EMAIL =
            "test@test.com";

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthTokenStore authTokenStore;

    @Mock
    private BlackListStore blackListStore;

    @Mock
    private SecurityErrorResponseWriter errorResponseWriter;

    @Mock
    private Authentication authentication;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 존재하지 않을 시 Fiter 실행")
    void doFilter_withoutAuthorizationHeader() throws Exception {
        // given


        // when
        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(filterChain).doFilter(request, response);

        verifyNoInteractions(tokenProvider, authTokenStore, blackListStore, errorResponseWriter);

    }

    @Test
    @DisplayName("AccessToken 검증 성공 시 인증 정보 저장")
    void doFilter_withValidAccessToken() throws Exception{
        // given

        setAuthorization();

        when(tokenProvider.getEmailFromToken(ACCESS_TOKEN))
                .thenReturn(EMAIL);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(false);

        when(blackListStore.isBlacklisted(EMAIL))
                .thenReturn(false);

        when(tokenProvider.getAuthentication(ACCESS_TOKEN))
                .thenReturn(authentication);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(tokenProvider).validateToken(ACCESS_TOKEN);

        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);

        verifyNoInteractions(errorResponseWriter);

    }

    @Test
    @DisplayName("로그아웃 처리된 유저 접근 시 401 예외 발생")
    void doFilter_withRevokedAccessToken() throws Exception{
        // given

        setAuthorization();

        when(tokenProvider.getEmailFromToken(ACCESS_TOKEN))
                .thenReturn(EMAIL);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(true);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(errorResponseWriter).write(response, LOGOUT_USER);

        verifyNoInteractions(blackListStore);
        verify(filterChain, never()).doFilter(request, response);

    }

    @Test
    @DisplayName("블랙리스트 사용자 접근 시 403 예외 발생")
    void doFilter_withBlacklistUser() throws Exception{
        // given
        setAuthorization();

        when(tokenProvider.getEmailFromToken(ACCESS_TOKEN))
                .thenReturn(EMAIL);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(false);

        when(blackListStore.isBlacklisted(EMAIL))
                .thenReturn(true);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(errorResponseWriter).write(response, BLACKLIST_USER);

        verify(filterChain, never()).doFilter(request, response);

    }

    @Test
    @DisplayName("유효하지 않은 토큰 접근 시 인증실패 예외 발생")
    void doFilter_withInvalidAccessToken() throws Exception{
        // given

        setAuthorization();

        doThrow(
                new CustomException(INVALID_TOKEN)
        ).when(tokenProvider).validateToken(ACCESS_TOKEN);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(errorResponseWriter).write(response, INVALID_TOKEN);
        verifyNoInteractions(authTokenStore, blackListStore);

        verify(filterChain, never()).doFilter(request, response);

    }


    @Test
    @DisplayName("인증 이후 발생한 예외 처리는 Filter가 처리하지 않는다.")
    void doFilter_NotCatchFilter() throws Exception{
        // given

        setAuthorization();

        when(tokenProvider.getEmailFromToken(ACCESS_TOKEN))
                .thenReturn(EMAIL);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(false);

        when(blackListStore.isBlacklisted(EMAIL))
                .thenReturn(false);

        when(tokenProvider.getAuthentication(ACCESS_TOKEN))
                .thenReturn(authentication);

        doThrow(
                new ServletException("downstream error")
        ).when(filterChain).doFilter(request, response);

        // when

        // then

        assertThrows(ServletException.class, () -> authenticationFilter.doFilter(request, response, filterChain));

        verifyNoInteractions(errorResponseWriter);

    }

    private void setAuthorization() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

}
