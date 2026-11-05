package com.example.basketballmatching.global.security;

import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.exception.SecurityAccessDeniedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {

        ErrorCode errorCode = exception instanceof SecurityAccessDeniedException denied
                ? denied.getErrorCode()
                : ErrorCode.ACCESS_DENIED;


        errorResponseWriter.write(response, errorCode);
    }
}
