package com.example.basketballmatching.global.exception;

import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.exception.dto.FieldErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, ErrorCode> CODE_MAP = Map.of(
            "NotBlank", INVALID_INPUT,
            "NotNull", INVALID_INPUT,
            "Pattern", INVALID_PATTERN,
            "Email", INVALID_PATTERN,
            "Past", PAST_BIRTHDAY
    );


    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {


        log.warn("CustomException 발생 : {}", e.getErrorCode().getHttpStatus());


        return new ResponseEntity<>(ErrorResponse.of(e.getErrorCode()), e.getErrorCode().getHttpStatus());

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        log.warn("MethodArgumentNotValidException 발생 : {}", e.getMessage());

        List<FieldErrorDetail> details = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> FieldErrorDetail.of(
                        error.getField(),
                        error.getCode(),
                        error.getDefaultMessage()
                )).toList();

        String firstCode = Optional.ofNullable(e.getBindingResult().getFieldErrors().get(0).getCode())
                .orElse("NotBlank");

        ErrorCode errorCode = map(firstCode);


        return new ResponseEntity<>(ErrorResponse.of(errorCode, details), errorCode.getHttpStatus());
    }

    private static ErrorCode map(String validationCode) {
        return CODE_MAP.getOrDefault(validationCode, INVALID_INPUT);
    }

}
