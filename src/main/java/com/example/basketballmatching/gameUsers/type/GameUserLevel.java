package com.example.basketballmatching.gameUsers.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GameUserLevel {

    NONE("경기 부족"),
    BEGINNER("초보자"),
    AMATEUR("아마추어"),
    SEMI_PRO("세미프로"),
    PRO("프로");

    private final String description;

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


}
