package com.example.basketballmatching.game.dto;


import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
public class ApplyGameUserListDto {

    private Long participantId;

    private String nickname;

    private GenderType genderType;

    private Position position;

    private LocalDate birth;

    public static ApplyGameUserListDto fromEntity(ParticipantGameEntity participantGameEntity) {

        return ApplyGameUserListDto.builder()
                .participantId(participantGameEntity.getParticipantGameId())
                .nickname(participantGameEntity.getUserEntity().getNickname())
                .genderType(participantGameEntity.getUserEntity().getGenderType())
                .position(participantGameEntity.getUserEntity().getPosition())
                .birth(participantGameEntity.getUserEntity().getBirth())
                .build();

    }

}
