package com.example.basketballmatching.game.service.impl;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.service.ParticipantGameService;
import com.example.basketballmatching.game.type.GameUserLevel;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.notifications.service.NotificationService;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.game.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantGameServiceImpl implements ParticipantGameService {

    private final ParticipantGameRepository participantGameRepository;

    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    private final NotificationService notificationService;





    /**
     * 경기 삭제
     */
    @Override
    @Transactional
    public CheckResponse deleteGame(Long userId, Long gameId) {

        log.info("[경기 삭제 시작] userId : {}, gameId : {}", userId, gameId);

        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        validateGameCreator(gameEntity, userEntity);

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(gameEntity.getStartDateTime().minusMinutes(30))) {
            throw new CustomException(NOT_DELETE_GAME);
        }


        // ACCEPT/ APPLY 유저 리스트
        List<ParticipantGameEntity> participantGameEntityList = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(List.of(ACCEPT), gameId);


        // 조회 유저 상태 DELETE로 변경
        participantGameEntityList.forEach(participantGame ->
                participantGame.delete(now));

        // 삭제 알림 전송
        participantGameEntityList
                .stream()
                .filter(participantGameEntity -> !Objects.equals(participantGameEntity.getUserEntity().getUserId(), userEntity.getUserId()))
                .forEach(

                        participantGame ->
                                notificationService.send(NotificationType.DELETE_GAME, participantGame.getUserEntity(), participantGame.getGameEntity().getTitle() + "의 게임이 삭제되었습니다.")

                );

        participantGameRepository.saveAll(participantGameEntityList);

        gameEntity.setDeletedDateTime(now);

        gameRepository.save(gameEntity);


        log.info("[경기 삭제 완료] userId : {}, gameId : {}", userId, gameId);


        return CheckResponse.of(true, "경기 삭제가 완료되었습니다.");
    }







    private void validateGameCreator(GameEntity gameEntity, UserEntity userEntity) {
        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            log.error("[CustomException 발생] errorCode : {}", NOT_GAME_CREATOR);
            throw new CustomException(NOT_GAME_CREATOR);
        }
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private GameEntity getGame(Long gameId) {
        return gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));
    }


}