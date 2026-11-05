package com.example.basketballmatching.global.security;

import com.example.basketballmatching.auth.service.AuthTokenStore;
import com.example.basketballmatching.blacklist.service.BlackListStore;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.JwtAuthenticationException;
import com.example.basketballmatching.global.exception.SecurityAccessDeniedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private RestAccessDeniedHandler accessDeniedHandler;

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
    @DisplayName("Authorization 헤더가 존재하지 않을 시 Filter 실행")
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

        Claims claims = Jwts.claims().setSubject(EMAIL);


        when(tokenProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(false);

        when(blackListStore.isBlacklisted(EMAIL))
                .thenReturn(false);

        when(tokenProvider.getAuthentication(claims))
                .thenReturn(authentication);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then


        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain).doFilter(request, response);

        verifyNoInteractions(errorResponseWriter);

    }

    @Test
    @DisplayName("로그아웃 토큰은 LOGOUT_USER 인증 오류로 전달한다")
    void doFilter_withRevokedAccessToken() throws Exception{
        // given

        setAuthorization();

        Claims claims = Jwts.claims().setSubject(EMAIL);


        when(tokenProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(true);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(authenticationEntryPoint).commence(
                same(request),
                same(response),
                argThat(exception -> exception instanceof JwtAuthenticationException jwt && jwt.getErrorCode() == LOGOUT_USER)
        );

        verifyNoInteractions(blackListStore, accessDeniedHandler, errorResponseWriter);

        verify(tokenProvider, never()).getAuthentication(any(Claims.class));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("제재 사용자는 BLACKLIST_USER 접근 거절 오류로 전달한다")
    void doFilter_withBlacklistUser() throws Exception{
        // given
        setAuthorization();

        Claims claims = Jwts.claims().setSubject(EMAIL);

        when(tokenProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(false);

        when(blackListStore.isBlacklisted(EMAIL))
                .thenReturn(true);

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(accessDeniedHandler).handle(
                same(request),
                same(response),
                argThat(exception -> exception instanceof SecurityAccessDeniedException denied
                        && denied.getErrorCode() == BLACKLIST_USER
                )
        );

        verifyNoInteractions(authenticationEntryPoint, errorResponseWriter);

        verify(tokenProvider, never()).getAuthentication(any(Claims.class));

        verify(filterChain, never()).doFilter(request, response);

        assertNull(SecurityContextHolder.getContext().getAuthentication());


    }

    @Test
    @DisplayName("잘못된 토큰은 INVALID_TOKEN 인증 오류로 전달한다")
    void doFilter_withInvalidAccessToken() throws Exception{
        // given

        setAuthorization();


        when(tokenProvider.validateAccessToken(ACCESS_TOKEN))
                .thenThrow(new CustomException(INVALID_TOKEN));

        // when

        authenticationFilter.doFilter(request, response, filterChain);

        // then

        verify(authenticationEntryPoint).commence(
                same(request),
                same(response),
                argThat(exception -> exception instanceof JwtAuthenticationException jwt
                    && jwt.getErrorCode() == INVALID_TOKEN
                )
        );

        verifyNoInteractions(authTokenStore, blackListStore, accessDeniedHandler, errorResponseWriter);

        verify(tokenProvider, never()).getAuthentication(any(Claims.class));

        verify(filterChain, never()).doFilter(request, response);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

    }


    @Test
    @DisplayName("인증 이후 발생한 예외 처리는 Filter가 처리하지 않는다.")
    void doFilter_NotCatchFilter() throws Exception{
        // given

        setAuthorization();
        Claims claims = Jwts.claims().setSubject(EMAIL);


        when(tokenProvider.validateAccessToken(ACCESS_TOKEN))
                .thenReturn(claims);

        when(authTokenStore.isAccessTokenRevoked(ACCESS_TOKEN))
                .thenReturn(false);

        when(blackListStore.isBlacklisted(EMAIL))
                .thenReturn(false);

        when(tokenProvider.getAuthentication(claims))
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
