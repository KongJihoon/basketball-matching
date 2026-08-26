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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = resolvedAccessToken(request);

        if (accessToken == null || isAlreadyAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }



        try {

            authenticate(accessToken);

        } catch (CustomException e) {
            log.warn("인증 실패: code={}", e.getErrorCode());
            errorResponseWriter.write(response, e.getErrorCode());
            return;
        } catch (Exception e) {
            log.error("INTERNAL SERVER ERROR 발생", e);
            errorResponseWriter.write(response, INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(
                request,
                response
        );

    }

    private void authenticate(String accessToken) {
        tokenProvider.validateToken(accessToken);

        String email = tokenProvider.getEmailFromToken(accessToken);

        validateSession(email, accessToken);

        Authentication authentication = tokenProvider.getAuthentication(accessToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void validateSession(String email, String accessToken) {
        if (authTokenStore.isAccessTokenRevoked(accessToken)) {
            throw new CustomException(LOGOUT_USER);
        }

        if (blackListStore.isBlacklisted(email)) {
            throw new CustomException(BLACKLIST_USER);
        }
    }

    private String resolvedAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorization) || !authorization.startsWith(TOKEN_PREFIX)) {
            return null;
        }

        String accessToken = authorization.substring(TOKEN_PREFIX.length()).trim();


        return StringUtils.hasText(accessToken) ? accessToken : null;
    }

    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

}
