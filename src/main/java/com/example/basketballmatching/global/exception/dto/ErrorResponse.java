package com.example.basketballmatching.global.exception.dto;


import com.example.basketballmatching.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private int statusCode;
    private String errorCode;
    private String errorMessage;

    public static ErrorResponse of(ErrorCode errorCode) {

        return ErrorResponse.builder()
                .statusCode(errorCode.getStatusCode())
                .errorCode(errorCode.name())
                .errorMessage(errorCode.getErrorMessage())
                .build();

    }

}
