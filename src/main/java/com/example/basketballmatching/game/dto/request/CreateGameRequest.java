package com.example.basketballmatching.game.dto.request;

import com.example.basketballmatching.game.type.FieldStatus;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateGameRequest(

        @Schema(name = "title", example = "경기 제목")
        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @Schema(name = "content", description = "경기 내용")
        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        @Schema(name = "headCount", description = "경기 모집 정원", example = "10", minimum = "6", maximum = "100")
        @NotNull(message = "인원수를 입력해주세요.")
        @Min(value = 6, message = "경기 정원은 최소 6명 이상이어야 합니다.")
        @Max(value = 100, message = "경기 정원은 최대 100명 이하여야 합니다.")
        Integer headCount,

        @Schema(name = "fieldStatus", description = "경기장 상태", example = "INDOOR")
        @NotNull(message = "경기장 상태를 입력해주세요.")
        FieldStatus fieldStatus,

        @Schema(name = "matchFormat", description = "경기 형식",example = "THREE_ON_THREE")
        @NotNull(message = "경기형식을 입력해주세요.")
        MatchFormat matchFormat,
        @Schema(name = "matchGenderType", description = "참가 성별 조건", example = "MIXED")
        @NotNull(message = "참가 성별 조건을 입력해주세요.")
        MatchGenderType matchGenderType,

        @Schema(name = "startDateTime", description = "시작 날짜", example = "2025-11-22T15:00:00:00")
        @NotNull(message = "시작 날짜를 입력해주세요.")
        LocalDateTime startDateTime,

        @Schema(name = "endDateTime", description = "종료 날짜", example = "2025-11-22T17:00:00:00")
        @NotNull(message = "종료 날짜를 입력해주세요.")
        LocalDateTime endDateTime,

        @Schema(name = "placeName", description = "경기 장소명", example = "잠실종합운동장 농구장")
        @NotBlank(message = "경기 장소명을 입력해주세요.")
        String placeName,

        @Schema(name = "address", description = "경기 주소", example = "서울특별시 송파구 올림픽로 25")
        @NotBlank(message = "경기 주소를 입력해주세요.")
        String address,

        @Schema(name = "latitude", description = "위도", example = "37.515")
        Double latitude,

        @Schema(name = "longitude", description = "경도", example = "127.073")
        Double longitude


        ) {
}
