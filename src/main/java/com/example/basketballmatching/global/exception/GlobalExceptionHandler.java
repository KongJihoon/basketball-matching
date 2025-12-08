package com.example.basketballmatching.global.exception;

import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.exception.dto.FieldErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, ErrorCode> CODE_MAP = Map.of(
            "NotBlank", INVALID_INPUT,
            "NotNull", INVALID_INPUT,
            "Pattern", INVALID_PATTERN,
            "Email", INVALID_PATTERN,
            "Past", PAST_BIRTHDAY,
            "Future", FUTURE_DATE
    );


    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {


        log.warn("CustomException: code={}, status={}, message={}",
                e.getErrorCode().name(),
                e.getErrorCode().getHttpStatus(),
                e.getMessage());


        return new ResponseEntity<>(ErrorResponse.of(e.getErrorCode()), e.getErrorCode().getHttpStatus());

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        List<FieldErrorDetail> details = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> FieldErrorDetail.of(
                        error.getField(),
                        Optional.ofNullable(error.getCode()).orElse("NotBlank"),
                        error.getDefaultMessage()
                )).toList();

        String firstCode = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getCode)
                .filter(code -> code != null && !code.isBlank())
                .orElse("NotBlank");

        ErrorCode errorCode = map(firstCode);

        log.warn("Validation failed: repCode={}, detailsSize={}", firstCode, details.size());


        return new ResponseEntity<>(ErrorResponse.of(errorCode, details), errorCode.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {

        log.error("[Unhandled exception]", e);

        return new ResponseEntity<>(
                ErrorResponse.of(INTERNAL_SERVER_ERROR),
                INTERNAL_SERVER_ERROR.getHttpStatus()
        );

    }


    private static ErrorCode map(String validationCode) {
        return CODE_MAP.getOrDefault(validationCode, INVALID_INPUT);
    }

}
