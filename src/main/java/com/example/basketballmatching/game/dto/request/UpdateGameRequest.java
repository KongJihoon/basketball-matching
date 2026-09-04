package com.example.basketballmatching.game.dto.request;

import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateGameRequest(
        @Schema(description = "경기 제목", example = "서울 xx체육관 3대3 인원 모집")
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백을 입력할 수 없습니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        String title,

        @Schema(description = "경기 상세 내용", example = "3대3 인원 모집합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "경기 내용은 공백을 입력할 수 없습니다.")
        @Size(max = 255, message = "경기 내용은 255자 이하로 입력해주세요.")
        String content,

        @Schema(description = "경기 모집 정원", example = "10", minimum = "6", maximum = "100")
        @Min(value = 6, message = "경기 정원은 최소 6명 이상이어야 합니다.")
        @Max(value = 100, message = "경기 정원은 최대 100명 이하여야 합니다.")
        Integer headCount,

        @Schema(description = "경기 형식", example = "THREE_ON_THREE")
        MatchFormat matchFormat,

        @Schema(description = "경기 성별", example = "MALE_ONLY")
        MatchGenderType matchGenderType,
        @Schema(description = "변경할 경기 시작 시각", example = "2026-08-10T19:00:00")
        LocalDateTime startDateTime,

        @Schema(description = "변경할 경기 종료 시각", example = "2026-08-10T21:00:00")
        LocalDateTime endDateTime
) {


    public boolean hasAnyChange() {
        return title != null
                || content != null
                || headCount != null
                || matchFormat != null
                || matchGenderType != null
                || startDateTime != null
                || endDateTime != null;
    }

    public boolean hasScheduleInput() {
        return startDateTime != null
                || endDateTime != null;
    }

    public boolean hasIncompleteSchedule() {
        return (startDateTime == null)
                != (endDateTime == null);
    }
}
