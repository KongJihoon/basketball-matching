package com.example.basketballmatching.game.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameCreatedEventDto {

    private Long gameId;

    private Long createdUserId;

    private String title;

    public static GameCreatedEventDto of(Long gameId, Long createdUserId, String title) {
        return new GameCreatedEventDto(gameId, createdUserId, title);
    }

}
