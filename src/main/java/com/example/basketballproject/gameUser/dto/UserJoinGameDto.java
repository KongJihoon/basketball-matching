package com.example.basketballproject.gameUser.dto;

import com.example.basketballproject.gameUser.type.ParticipantStatus;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserJoinGameDto {


    private Long userId;

    private Long gameId;

    private String gameAddress;

    private ParticipantStatus status;

    private LocalDateTime createdDatetime;

    public static UserJoinGameDto from(ParticipantGameDto participantGameDto) {
        return UserJoinGameDto.builder()
                .userId(participantGameDto.getUserEntity().getUserId())
                .gameId(participantGameDto.getGameEntity().getGameId())
                .gameAddress(participantGameDto.getGameEntity().getAddress())
                .status(participantGameDto.getStatus())
                .createdDatetime(participantGameDto.getCreatedDateTime())
                .build();
    }

}
