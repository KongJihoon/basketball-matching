package com.example.basketballproject.gameUser.dto;


import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameCreator.type.CityName;
import com.example.basketballproject.gameCreator.type.FieldState;
import com.example.basketballproject.gameCreator.type.Gender;
import com.example.basketballproject.gameCreator.type.MatchFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameSearchDto {

    private Long gameId;

    private String title;

    private String content;

    private Long headCount;

    private FieldState fieldState;

    private Gender gender;

    private LocalDateTime startDateTime;

    private String address;

    private Double latitude;

    private Double longitude;

    private CityName cityName;

    private MatchFormat matchFormat;

    public static GameSearchDto of(GameEntity gameEntity) {

        return GameSearchDto.builder()
                .gameId(gameEntity.getGameId())
                .title(gameEntity.getTitle())
                .content(gameEntity.getContent())
                .headCount(gameEntity.getHeadCount())
                .fieldState(gameEntity.getFieldState())
                .gender(gameEntity.getGender())
                .startDateTime(gameEntity.getStartDateTime())
                .latitude(gameEntity.getLatitude())
                .longitude(gameEntity.getLongitude())
                .cityName(gameEntity.getCityName())
                .matchFormat(gameEntity.getMatchFormat())
                .build();

    }


}
