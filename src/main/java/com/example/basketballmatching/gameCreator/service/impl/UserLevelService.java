package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameUsers.dto.GameAvgScoreDto;
import com.example.basketballmatching.gameUsers.repository.LevelQueryRepository;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.gameUsers.type.GameUserLevel.NONE;
import static com.example.basketballmatching.global.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserLevelService {


    private final UserRepository userRepository;
    private final GameQueryRepository gameQueryRepository;
    private final LevelQueryRepository levelQueryRepository;

    public void recalculateLevel(Long userId) {

        UserEntity userEntity = userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        List<Long> recent10GamesIds = gameQueryRepository.findRecent10GamesByUser(userEntity);

        if (recent10GamesIds.size() < 10) {
            userEntity.updateLevel(NONE);
            return;
        }

        List<GameAvgScoreDto> avgScoreByGames = levelQueryRepository.findAvgScoreByGames(userEntity.getUserId(), recent10GamesIds);

        if (avgScoreByGames.size() < 5) {
            userEntity.updateLevel(NONE);
            return;
        }

        double average = avgScoreByGames.stream()
                .map(GameAvgScoreDto::getAvgScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        userEntity.updateLevel(GameUserLevel.fromScore(average));

        userRepository.save(userEntity);


    }
}
