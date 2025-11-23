package com.example.basketballmatching.global.exception.dto;


import com.example.basketballmatching.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {
    @Schema(description = "HTTP 상태 코드", example = "400")
    private int statusCode;

    @Schema(description = "에러코드", example = "USER_NOT_FOUND")
    private String errorCode;

    @Schema(description = "에러메시지", example = "사용자를 찾을 수 없습니다.")
    private String errorMessage;
    private List<FieldErrorDetail> details;

    public static ErrorResponse of(ErrorCode errorCode) {

        return ErrorResponse.builder()
                .statusCode(errorCode.getStatusCode())
                .errorCode(errorCode.name())
                .errorMessage(errorCode.getErrorMessage())
                .build();

    }

    public static ErrorResponse of (ErrorCode errorCode, List<FieldErrorDetail> details) {

        return ErrorResponse.builder()
                .statusCode(errorCode.getStatusCode())
                .errorCode(errorCode.name())
                .errorMessage(errorCode.getErrorMessage())
                .details(details)
                .build();

    }

}
