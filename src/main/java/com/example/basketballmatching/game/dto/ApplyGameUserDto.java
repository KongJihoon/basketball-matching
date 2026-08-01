package com.example.basketballmatching.game.dto;


import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyGameUserDto {


    private Long userId;

    private Long gameId;

    private String gameAddress;

    private ParticipantGameStatus participantGameStatus;

    private LocalDateTime createdDateTime;

    public static ApplyGameUserDto fromEntity(ParticipantGameEntity participantGameEntity) {
        return ApplyGameUserDto.builder()
                .userId(participantGameEntity.getUserEntity().getUserId())
                .gameId(participantGameEntity.getGameEntity().getGameId())
                .participantGameStatus(participantGameEntity.getParticipantGameStatus())
                .gameAddress(participantGameEntity.getGameEntity().getAddress())
                .createdDateTime(participantGameEntity.getCreatedAt())
                .build();
    }


}
