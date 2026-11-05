package com.example.basketballmatching.global.security;

import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.exception.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {

        ErrorCode errorCode;

        if (exception instanceof JwtAuthenticationException jwtException) {
            errorCode = jwtException.getErrorCode();
        } else if (exception instanceof InsufficientAuthenticationException) {
            errorCode = ErrorCode.NOT_FOUND_TOKEN;
        } else {
            errorCode = ErrorCode.INVALID_TOKEN;
        }

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        errorResponseWriter.write(response, errorCode);

    }
}
