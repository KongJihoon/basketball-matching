package com.example.basketballproject.global.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private int statusCode;

    private ErrorCode errorCode;

    private String errorMessage;

    private List<String> details;

    public ErrorResponse(ErrorCode errorCode) {
        this.statusCode = errorCode.getStatusCode();
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }

    public ErrorResponse(ErrorCode errorCode, List<String> details) {

        this.statusCode = errorCode.getStatusCode();
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
        this.details = details;

    }

}
