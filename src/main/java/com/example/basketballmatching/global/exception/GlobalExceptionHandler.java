package com.example.basketballmatching.global.exception;

import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {


        log.warn("CustomException 발생 : {}", e.getErrorCode().getHttpStatus());


        return new ResponseEntity<>(ErrorResponse.of(e.getErrorCode()), e.getErrorCode().getHttpStatus());

    }

}
