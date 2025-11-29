package com.example.basketballmatching.gameUsers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameAvgScoreDto {

    private final Long gameId;

    private final Double avgScore;

}
