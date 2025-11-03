package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.type.MatchFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
@Getter
@AllArgsConstructor
@Builder
public class EditGameDto {

        private String title;

        private String content;

        private int headCount;

        private MatchFormat matchFormat;


}
