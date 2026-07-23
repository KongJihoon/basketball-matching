package com.example.basketballmatching.gameUsers.dto;

import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.user.domain.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GameUserLevelDto {

    private Long userId;

    private String nickname;

    private GameUserLevel gameUserLevel;

    public static GameUserLevelDto fromEntity(UserEntity userEntity) {
        return GameUserLevelDto.builder()
                .userId(userEntity.getUserId())
                .nickname(userEntity.getNickname())
                .gameUserLevel(userEntity.getGameUserLevel())
                .build();
    }

}
