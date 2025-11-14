package com.example.basketballmatching.gameUsers.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameUsers.entity.LevelEntity;
import com.example.basketballmatching.user.entity.UserEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EvaluatePlayerDto {

    @NotNull
    private Long receiverId;

    @NotNull
    @Min(1)
    @Max(5)
    private int score;

    public static LevelEntity toEntity( UserEntity evaluator, UserEntity receiver, GameEntity gameEntity, int score) {


        return LevelEntity.builder()
                .evaluator(evaluator)
                .receiver(receiver)
                .gameEntity(gameEntity)
                .score(score)
                .build();
    }


}
