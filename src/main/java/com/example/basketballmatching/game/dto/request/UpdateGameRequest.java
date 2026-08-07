package com.example.basketballmatching.game.dto.request;

import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateGameRequest(
        @Schema(description = "경기 제목", example = "서울 xx체육관 3대3 인원 모집")
        String title,

        @Schema(description = "경기 상세 내용", example = "3대3 인원 모집합니다.")
        String content,

        @Schema(description = "경기 인원 수", example = "6")
        Integer headCount,

        @Schema(description = "경기 형식", example = "THREE_ON_THREE")
        MatchFormat matchFormat,

        @Schema(description = "경기 성별", example = "MALE_ONLY")
        MatchGenderType matchGenderType
) {
}
