package com.example.basketballproject.gameUser.dto;


import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameUser.entity.ParticipantEntity;
import com.example.basketballproject.gameUser.type.ParticipantStatus;
import com.example.basketballproject.user.entity.UserEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParticipantGameDto {


    private Long participantId;

    private ParticipantStatus status;

    private LocalDateTime createdDateTime;

    private LocalDateTime acceptDateTime;

    private LocalDateTime canceledDateTime;

    private GameEntity gameEntity;

    private UserEntity userEntity;

    public static ParticipantGameDto fromEntity(ParticipantEntity participantEntity) {

        return ParticipantGameDto.builder()
                .participantId(participantEntity.getParticipantId())
                .status(participantEntity.getStatus())
                .createdDateTime(participantEntity.getCreatedDateTime())
                .acceptDateTime(participantEntity.getAcceptDateTime())
                .canceledDateTime(participantEntity.getCanceledDateTime())
                .gameEntity(participantEntity.getGameEntity())
                .userEntity(participantEntity.getUserEntity())
                .build();
    }

}
