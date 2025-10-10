package com.example.basketballmatching.global.security;


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

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthentificationFilter  extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolvedTokenRequest(request);

        try {

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (tokenProvider.validateToken(token)) {

                Authentication authentication = tokenProvider.getAuthentication(token);

                log.info("인증 객체 principal: {}", authentication.getPrincipal());

                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }


                filterChain.doFilter(request, response);

            }

        } catch (CustomException e) {
            log.warn("JWT 검증 실패: {}", e.getErrorCode().getErrorMessage());
            setErrorResponse(response, e.getErrorCode());
            return;

        } catch (Exception e) {
            log.error("INTERNAL SERVER ERROR 발생: {}", e.getMessage());
            setErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request, response);


    }

    private String resolvedTokenRequest(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);

        if (!ObjectUtils.isEmpty(token) && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length()).trim();
        }


        return null;
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException{
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
