package com.example.basketballproject.gameCreator.dto;

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
public class DeleteGameDto {


    private String title;

    private String content;

    private Long headCount;

    private FieldState fieldState;

    private Gender gender;

    private String address;

    private CityName cityName;

    private MatchFormat matchFormat;

    private LocalDateTime startDateTime;

    private LocalDateTime createdDateTime;

    private LocalDateTime deletedDateTime;


    public static DeleteGameDto toDto(GameEntity gameEntity) {


        return DeleteGameDto.builder()
                .title(gameEntity.getTitle())
                .content(gameEntity.getContent())
                .headCount(gameEntity.getHeadCount())
                .fieldState(gameEntity.getFieldState())
                .gender(gameEntity.getGender())
                .address(gameEntity.getAddress())
                .cityName(gameEntity.getCityName())
                .matchFormat(gameEntity.getMatchFormat())
                .startDateTime(gameEntity.getStartDateTime())
                .createdDateTime(gameEntity.getCreatedDateTime())
                .deletedDateTime(gameEntity.getDeletedDateTime())
                .build();

    }

}
