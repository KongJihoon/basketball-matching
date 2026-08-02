package com.example.basketballmatching.game.type;

import com.example.basketballmatching.global.exception.CustomException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.example.basketballmatching.global.exception.ErrorCode.INVALID_HEADCOUNT;

@Getter
@AllArgsConstructor
public enum MatchFormat {


    FIVE_ON_FIVE(10, 20),
    THREE_ON_THREE(6, 9);

    private final int minimumHeadCount;

    private final int maximumHeadCount;

    public void validateHeadCount(Integer headCount) {

        if (headCount == null || headCount < minimumHeadCount || headCount > maximumHeadCount) {

            throw new CustomException(INVALID_HEADCOUNT);

        }

    }

}
