package com.example.basketballmatching.game.type;

import com.example.basketballmatching.global.exception.CustomException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.example.basketballmatching.global.exception.ErrorCode.INVALID_HEADCOUNT;

@Getter
@AllArgsConstructor
public enum MatchFormat {


    FIVE_ON_FIVE(10),
    THREE_ON_THREE(6);

    private static final int MAXIMUM_HEAD_COUNT = 100;

    private final int minimumHeadCount;


    public void validateHeadCount(Integer headCount) {

        if (headCount == null || headCount < minimumHeadCount || headCount > MAXIMUM_HEAD_COUNT) {

            throw new CustomException(INVALID_HEADCOUNT);

        }

    }

}
