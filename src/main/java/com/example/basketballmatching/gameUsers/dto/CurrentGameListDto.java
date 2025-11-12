package com.example.basketballmatching.gameUsers.dto;

import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.type.CityName;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentGameListDto {

    private Long gameId;

    private String title;

    private String address;

    private CityName cityName;

    private MatchFormat matchFormat;

    private MatchGenderType matchGenderType;

    private LocalDateTime startDateTime;

    private ParticipantGameStatus participantGameStatus;

    public static CurrentGameListDto fromEntity(ParticipantGameEntity participantGameEntity) {

        return CurrentGameListDto.builder()
                .gameId(participantGameEntity.getGameEntity().getGameId())
                .title(participantGameEntity.getGameEntity().getTitle())
                .address(participantGameEntity.getGameEntity().getAddress())
                .cityName(participantGameEntity.getGameEntity().getCityName())
                .matchFormat(participantGameEntity.getGameEntity().getMatchFormat())
                .matchGenderType(participantGameEntity.getGameEntity().getMatchGenderType())
                .startDateTime(participantGameEntity.getGameEntity().getStartDateTime())
                .participantGameStatus(participantGameEntity.getParticipantGameStatus())
                .build();


    }


}
