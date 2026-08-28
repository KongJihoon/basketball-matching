package com.example.basketballmatching.notifications.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisTopic {

    GAME_CREATED("game.created");

    private final String value;

}
