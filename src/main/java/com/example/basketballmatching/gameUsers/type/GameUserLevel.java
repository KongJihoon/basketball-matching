package com.example.basketballmatching.gameUsers.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GameUserLevel {

    NONE("경기 부족", 0.0),
    BEGINNER("초보자", 1.0),
    AMATEUR("아마추어", 2.0),
    SEMI_PRO("세미프로", 3.0),
    PRO("프로", 4.0);

    private final String description;

    private final double value;

    public static GameUserLevel fromScore(double score) {

        if (score < 2.5) {
            return BEGINNER;
        }

        if (score < 3.5) {
            return AMATEUR;
        }

        if (score < 4.5) {
            return SEMI_PRO;
        }

        return PRO;

    }

    public static GameUserLevel fromUserLevelAverage(double avg) {

        if (avg < 1.8) {
            return BEGINNER;
        }

        if (avg < 2.8) {
            return AMATEUR;
        }

        if (avg < 3.8) {
            return SEMI_PRO;
        }

        return PRO;
    }


}
