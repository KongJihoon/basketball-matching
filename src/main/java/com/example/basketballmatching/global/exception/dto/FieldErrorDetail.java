package com.example.basketballmatching.global.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class FieldErrorDetail {

    @Schema(description = "검증 실패한 필드명", example = "email")
    private final String field;

    @Schema(description = "검증 실패 코드", example =  "NotBlank")
    private final String code;

    @Schema(description = "검증 실패 메시지", example = "이메일은 필수 입력값입니다.")
    private final String message;
}
