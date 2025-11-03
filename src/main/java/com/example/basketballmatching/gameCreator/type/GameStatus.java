package com.example.basketballmatching.gameCreator.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GameStatus {

    RECRUITING("모집 중"),
    CLOSED("마감");


    private final String description;



}
