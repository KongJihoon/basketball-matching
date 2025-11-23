package com.example.basketballmatching.gameUsers.dto;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameUsers.entity.LevelEntity;
import com.example.basketballmatching.user.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(name = "피평가자 아이디", example = "1")
    @NotNull
    private Long receiverId;

    @Schema(name = "평가 점수", example = "1")
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
