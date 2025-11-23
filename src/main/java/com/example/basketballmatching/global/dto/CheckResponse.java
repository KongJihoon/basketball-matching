package com.example.basketballmatching.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class CheckResponse {

    private final boolean success;

    @Schema(name = "확인 메시지", example = "회원가입에 성공하였습니다.")
    private final String message;


}
