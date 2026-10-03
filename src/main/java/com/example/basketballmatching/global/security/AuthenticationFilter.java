package com.example.basketballmatching.global.security;


import com.example.basketballmatching.auth.service.AuthTokenStore;
import com.example.basketballmatching.blacklist.service.BlackListStore;
import com.example.basketballmatching.global.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_PREFIX = "Bearer ";


    private final TokenProvider tokenProvider;

    private final AuthTokenStore authTokenStore;

    private final BlackListStore blackListStore;

    private final SecurityErrorResponseWriter errorResponseWriter;

    /**
     * 요청마다 Authorization 헤더의 AccessToken을 확인하고,
     * 유효한 토큰이면 Spring Security 인증 정보를 반환한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = resolvedAccessToken(request);

        /*
         * 토큰이 없으면 인증 객체를 생성하지 않고 다음 필터로 진행한다.
         * 공개 API는 통과하고, 권한이 있는 API는 이후 인증 인가 과정에서 차단한다.
         *
         * 이미 인증된 요청이면 기존 인증 정보를 덮어쓰지 않는다.
         */
        if (accessToken == null || isAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }



        try {

            authenticate(accessToken);

        } catch (CustomException e) {
            /*
             * Security Filter는 Controller보다 먼저 실행되므로
             * 인증 예외를 HTTP 응답에 직접 작성한다.
             */
            log.warn("인증 실패: code={}", e.getErrorCode());
            errorResponseWriter.write(response, e.getErrorCode());
            return;
        } catch (Exception e) {
            log.error("INTERNAL SERVER ERROR 발생", e);
            errorResponseWriter.write(response, INTERNAL_SERVER_ERROR);
            return;
        }

        // 인증 정보를 SecurityContext에 저장한 후 다음 필터로 진행한다.
        filterChain.doFilter(
                request,
                response
        );

    }

    /**
     * AccessToken을 검증하고 현재 사용자에 대한
     * Spring Security Authentication 객체를 SecurityContext에 저장한다.
     */
    private void authenticate(String accessToken) {

        // JWT의 서명 구조 및 만료 여부를 검증한다.
        tokenProvider.validateToken(accessToken);


        // JWT subject에 저장된 이메일을 추출한다.
        String email = tokenProvider.getEmailFromToken(accessToken);

        /*
         * JWT가 유효하더라도 로그아웃, 블랙리스트 상태일 수 있기 땜누에
         * 서버가 관리하는 현재 세션 상태를 추가로 확인한다.
         */
        validateSession(email, accessToken);

        /*
         * 이메일로 현재 활성 사용자의 권한을 조회하고
         * Spring Security가 사용할 Authentication 객체를 생성한다.
         */
        Authentication authentication = tokenProvider.getAuthentication(accessToken);

        // 이후 인가 과정에서 사용할 수 있도록 현재 요청의 SecurityContext에 Authentication을 저장한다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * JWT 유효성과 별개로 서버에서 폐기하거나
     * 제재한 사용자의 요청을 차단한다.
     */
    private void validateSession(String email, String accessToken) {

        // 로그아웃 이후 Redis 블랙리스트에 등록된 AccessToken의 접근을 차단한다.
        if (authTokenStore.isAccessTokenRevoked(accessToken)) {
            throw new CustomException(LOGOUT_USER);
        }

        // 현재 블랙리스트 제재 기간이 남아있는 사용자의 요청을 차단.
        if (blackListStore.isBlacklisted(email)) {
            throw new CustomException(BLACKLIST_USER);
        }
    }

    /**
     * Authorization 헤더에서 Bearer AccessToken을 추출한다.
     * 헤더가 없거나 Bearer 형식이 아니라면 null 반환.
     */
    private String resolvedAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorization) || !authorization.startsWith(TOKEN_PREFIX)) {
            return null;
        }

        String accessToken = authorization.substring(TOKEN_PREFIX.length()).trim();


        return StringUtils.hasText(accessToken) ? accessToken : null;
    }

    /**
     * 현재 요청에 이미 SecurityContext에 인증 정보가 존재하는지 확인한ㄴ다.
     */
    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

}
