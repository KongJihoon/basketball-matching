package com.example.basketballmatching.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor(staticName = "of")
public class CommonResponse<T> {

    @Schema(name = "확인 메시지", example = "***에 성공하였습니다.")
    private final String message;
    private final T data;

}
