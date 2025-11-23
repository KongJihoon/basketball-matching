package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.type.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class GameDto {

    private Long gameId;

    private String title;

    private String content;

    private int headCount;

    private int participantCount;

    private FieldStatus fieldStatus;

    private MatchFormat matchFormat;

    private GameStatus gameStatus;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedDateTime;

    private String placeName;

    private String address;

    private Double latitude;

    private Double longitude;

    private CityName cityName;

    private MatchGenderType matchGenderType;

    private Long creatorId;

    private String creatorNickname;



    public static GameDto fromEntity(GameEntity gameEntity) {

        return GameDto.builder()
                .gameId(gameEntity.getGameId())
                .title(gameEntity.getTitle())
                .content(gameEntity.getContent())
                .headCount(gameEntity.getHeadCount())
                .participantCount(gameEntity.getParticipantCount())
                .fieldStatus(gameEntity.getFieldStatus())
                .matchFormat(gameEntity.getMatchFormat())
                .gameStatus(gameEntity.getGameStatus())
                .startDateTime(gameEntity.getStartDateTime())
                .endDateTime(gameEntity.getEndDateTime())
                .createdAt(gameEntity.getCreatedAt())
                .updatedAt(gameEntity.getUpdatedAt())
                .deletedDateTime(gameEntity.getDeletedDateTime())
                .placeName(gameEntity.getPlaceName())
                .address(gameEntity.getAddress())
                .latitude(gameEntity.getLatitude())
                .longitude(gameEntity.getLongitude())
                .cityName(gameEntity.getCityName())
                .matchGenderType(gameEntity.getMatchGenderType())
                .creatorId(gameEntity.getUserEntity().getUserId())
                .creatorNickname(gameEntity.getUserEntity().getNickname())
                .build();

    }
}
