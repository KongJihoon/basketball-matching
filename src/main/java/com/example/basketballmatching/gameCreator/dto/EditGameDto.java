package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.type.MatchFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
@Getter
@AllArgsConstructor
@Builder
public class EditGameDto {

        @Schema(name = "경기 제목", example = "서울 xx체육관에서 3대3 인원 모집")
        private String title;

        @Schema(name = "경기 상세 내용", example = "3대3인원 모집합니다.")
        private String content;

        @Schema(name = "경기 인원 수", example = "6")
        private int headCount;

        @Schema(name = "경기 형식", example = "THREE_ON_THREE")
        private MatchFormat matchFormat;


}
