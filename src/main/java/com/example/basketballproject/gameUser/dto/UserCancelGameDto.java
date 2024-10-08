package com.example.basketballproject.gameUser.dto;

import com.example.basketballproject.gameUser.type.ParticipantStatus;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCancelGameDto {

    private Long userId;

    private Long gameId;

    private String gameAddress;

    private ParticipantStatus status;

    private LocalDateTime canceledDateTime;

    public static UserCancelGameDto cancelGame(ParticipantGameDto participantGameDto) {

        return UserCancelGameDto.builder()
                .userId(participantGameDto.getUserEntity().getUserId())
                .gameId(participantGameDto.getGameEntity().getGameId())
                .gameAddress(participantGameDto.getGameEntity().getAddress())
                .status(participantGameDto.getStatus())
                .canceledDateTime(participantGameDto.getCanceledDateTime())
                .build();
    }

}
