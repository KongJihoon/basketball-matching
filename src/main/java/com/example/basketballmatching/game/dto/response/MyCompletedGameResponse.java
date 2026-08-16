package com.example.basketballmatching.game.dto.response;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.type.CityName;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;

public record MyCompletedGameResponse(
        @Schema(description = "경기 ID", example = "1")
        Long gameId,

        @Schema(description = "경기 제목", example = "잠실 주말 농구")
        String title,

        @Schema(description = "경기 장소", example = "잠실종합운동장 농구장")
        String placeName,

        @Schema(description = "경기 주소", example = "서울특별시 송파구 올림픽로 25")
        String address,

        @Schema(description = "경기 지역", example = "SEOUL")
        CityName cityName,

        @Schema(description = "경기 형식", example = "THREE_ON_THREE")
        MatchFormat matchFormat,

        @Schema(description = "참가 성별 조건", example = "MIXED")
        MatchGenderType matchGenderType,

        @Schema(description = "경기 시작 시각", example = "2026-08-10T15:00:00")
        LocalDateTime startDateTime,

        @Schema(description = "경기 종료 시각", example = "2026-08-10T17:00:00")
        LocalDateTime endDateTime,

        @Schema(description = "경기 정원", example = "6")
        int headCount,

        @Schema(description = "최종 참가 인원", example = "6")
        int participantCount,

        @Schema(description = "경기 생성자 여부", example = "false")
        boolean creator
) {

    public static MyCompletedGameResponse fromEntity(ParticipantGameEntity participation, Long requesterId) {
        GameEntity game = participation.getGameEntity();

        return new MyCompletedGameResponse(
                game.getGameId(),
                game.getTitle(),
                game.getPlaceName(),
                game.getAddress(),
                game.getCityName(),
                game.getMatchFormat(),
                game.getMatchGenderType(),
                game.getStartDateTime(),
                game.getEndDateTime(),
                game.getHeadCount(),
                game.getParticipantCount(),
                Objects.equals(game.getUserEntity().getUserId(), requesterId)
        );

    }
}
