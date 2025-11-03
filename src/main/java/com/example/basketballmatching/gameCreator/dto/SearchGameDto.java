package com.example.basketballmatching.gameCreator.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.type.GameStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
@Builder
@AllArgsConstructor
@Getter
public class SearchGameDto {

        private Long gameId;

        private String title;

        private String address;

        private LocalDateTime startDateTime;

        private MatchGenderType matchGenderType;

        private MatchFormat matchFormat;

        private GameStatus gameStatus;

        public static SearchGameDto fromEntity(GameEntity gameEntity) {

            return SearchGameDto.builder()
                    .gameId(gameEntity.getGameId())
                    .title(gameEntity.getTitle())
                    .address(gameEntity.getAddress())
                    .startDateTime(gameEntity.getStartDateTime())
                    .matchGenderType(gameEntity.getMatchGenderType())
                    .matchFormat(gameEntity.getMatchFormat())
                    .gameStatus(gameEntity.getGameStatus())
                    .build();

        }





}
