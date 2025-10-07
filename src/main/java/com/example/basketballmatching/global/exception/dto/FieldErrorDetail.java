package com.example.basketballmatching.global.exception.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class FieldErrorDetail {

    private final String field;
    private final String code;
    private final String message;
}
