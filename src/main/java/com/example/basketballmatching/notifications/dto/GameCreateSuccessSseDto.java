package com.example.basketballmatching.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameCreateSuccessSseDto {


    private Long receiverUserId;

    private Long gameId;

    private String title;

    private String message;

    private Long createdAtEpochMs;

    public static GameCreateSuccessSseDto of(Long receiverUserId, Long gameId, String title) {


        return new GameCreateSuccessSseDto(
                receiverUserId, gameId, title, "경기 생성이 완료되었습니다", System.currentTimeMillis()
        );
    }

}
