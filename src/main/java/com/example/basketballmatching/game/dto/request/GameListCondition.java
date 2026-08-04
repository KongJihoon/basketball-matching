package com.example.basketballmatching.game.dto.request;

import com.example.basketballmatching.game.type.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record GameListCondition(
        @Schema(name = "date", description = "조회 날짜", example = "2026-08-04")
        @NotNull(message = "조회 날짜를 입력해주세요.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        @Schema(name = "cityName", description = "지역", example = "SEOUL")
        CityName cityName,

        @Schema(name = "matchFormat", description = "경기 형식", example = "THREE_ON_THREE")
        MatchFormat matchFormat,

        @Schema(name = "fieldStatus", description = "실내 실외 구분", example = "INDOOR")
        FieldStatus fieldStatus,

        @Schema(name = "matchGenderType", description = "경기 성별 조건", example = "MALE")
        MatchGenderType matchGenderType,

        @Schema(name = "gameStatus", description = "경기 상태", example = "RECRUITING")
        GameStatus gameStatus

) {
}
