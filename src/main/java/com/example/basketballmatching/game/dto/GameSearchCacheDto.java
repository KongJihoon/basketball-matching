package com.example.basketballmatching.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameSearchCacheDto<T> implements Serializable {
    private List<T> content;
    private long totalElement;
}
