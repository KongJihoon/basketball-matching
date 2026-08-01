package com.example.basketballmatching.game.dto;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.type.GameStatus;
import com.example.basketballmatching.game.type.MatchFormat;
import com.example.basketballmatching.game.type.MatchGenderType;
import com.example.basketballmatching.game.type.GameUserLevel;
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

        private GameUserLevel gameUserLevel;

        public static SearchGameDto fromEntity(GameEntity gameEntity) {

            return SearchGameDto.builder()
                    .gameId(gameEntity.getGameId())
                    .title(gameEntity.getTitle())
                    .address(gameEntity.getAddress())
                    .startDateTime(gameEntity.getStartDateTime())
                    .matchGenderType(gameEntity.getMatchGenderType())
                    .matchFormat(gameEntity.getMatchFormat())
                    .gameStatus(gameEntity.getGameStatus())
                    .gameUserLevel(gameEntity.getGameUserLevel())
                    .build();

        }





}
