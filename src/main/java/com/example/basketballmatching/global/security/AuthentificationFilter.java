package com.example.basketballmatching.global.security;


import com.example.basketballmatching.auth.service.AuthTokenStore;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthentificationFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";


    private final TokenProvider tokenProvider;

    private final RedisService redisService;

    private final AuthTokenStore authTokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolvedTokenRequest(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }


        try {


            tokenProvider.validateToken(token);

            String email = tokenProvider.getEmailFromToken(token);


            String blackList = redisService.getData("blackList:" + email);

            if (authTokenStore.isAccessTokenRevoked(token)) {
                log.warn("[로그아웃 유저 접근]: {}", email);

                setErrorResponse(response, LOGOUT_USER);
                return;
            }

            if (blackList != null) {
                log.warn("[블랙리스트 유저 접근]: {}", email);

                setErrorResponse(response, BLACKLIST_USER);
                return;
            }


            Authentication authentication = tokenProvider.getAuthentication(token);


            if (authentication != null) {
                log.info("인증 객체 principal: {}", authentication.getPrincipal());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }


            filterChain.doFilter(request, response);


        } catch (CustomException e) {
            log.warn("JWT 검증 실패: {}", e.getErrorCode().getErrorMessage());
            setErrorResponse(response, e.getErrorCode());
            return;
        } catch (Exception e) {
            log.error("INTERNAL SERVER ERROR 발생: {}", e.getMessage());
            setErrorResponse(response, INTERNAL_SERVER_ERROR);
            return;
        }


    }

    private String resolvedTokenRequest(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);

        if (!ObjectUtils.isEmpty(token) && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length()).trim();
        }


        return null;
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(errorCode.getStatusCode());

        ObjectMapper objectMapper = new ObjectMapper();

        String errorMessage = objectMapper.writeValueAsString(
                Map.of("statusCode", errorCode.getStatusCode(),
                        "errorCode", errorCode.name(), "errorMessage", errorCode.getErrorMessage()
                )
        );

        response.getWriter().write(errorMessage);
    }
}
