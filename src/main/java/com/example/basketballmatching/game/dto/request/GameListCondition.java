package com.example.basketballmatching.game.dto.request;

import com.example.basketballmatching.game.type.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record GameListCondition(
        @Schema(name = "date", description = "조회 날짜", example = "2026-08-04")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        @Schema(name = "keyword", description = "검색어", example = "잠실")
        @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
        String keyword,

        @Schema(name = "cityName", description = "지역", example = "SEOUL")
        CityName cityName,

        @Schema(name = "matchFormat", description = "경기 형식", example = "THREE_ON_THREE")
        MatchFormat matchFormat,

        @Schema(name = "fieldStatus", description = "실내 실외 구분", example = "INDOOR")
        FieldStatus fieldStatus,

        @Schema(name = "matchGenderType", description = "경기 성별 조건", example = "MALE_ONLY")
        MatchGenderType matchGenderType,

        @Schema(name = "gameStatus", description = "경기 상태", example = "RECRUITING")
        GameStatus gameStatus,

        @Schema(name = "sortType", description = "정렬 기준", example = "START_TIME_ASC")
        GameSortType sortType

) {
}
