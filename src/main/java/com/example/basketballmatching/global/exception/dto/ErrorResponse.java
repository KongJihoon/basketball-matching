package com.example.basketballmatching.global.exception.dto;


import com.example.basketballmatching.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private int statusCode;
    private String errorCode;
    private String description;

    public static ErrorResponse of(ErrorCode errorCode) {

        return ErrorResponse.builder()
                .statusCode(errorCode.getStatusCode())
                .errorCode(errorCode.name())
                .description(errorCode.getDescription())
                .build();

    }

}
