package com.example.basketballmatching.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class CheckResponse {

    private final boolean success;
    private final String message;


}
