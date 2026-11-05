package com.example.basketballmatching.global.exception;

import lombok.Getter;
import org.springframework.security.access.AccessDeniedException;

@Getter
public class SecurityAccessDeniedException extends AccessDeniedException {

    private final ErrorCode errorCode;

    public SecurityAccessDeniedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getErrorMessage(), cause);
        this.errorCode = errorCode;
    }

}
