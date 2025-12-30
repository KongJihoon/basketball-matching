package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.AcceptGameUserListDto;
import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.ParticipantGameService;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.cache.event.GameSearchCacheBumpEvent;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.notifications.service.NotificationService;
import com.example.basketballmatching.notifications.type.NotificationType;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.ACCEPT;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantGameServiceImpl implements ParticipantGameService {

    private final ParticipantGameRepository participantGameRepository;

    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    private final NotificationService notificationService;

    private final ApplicationEventPublisher eventPublisher;



    /**
     * 경기 참가 신청자 조회
     */
    @Override
    @Transactional(readOnly = true)
    public CommonResponse<List<ApplyGameUserListDto>> getApplyParticipantList(Long gameId, Long userId, Pageable pageable) {

        log.info("[경기 참가 신청자 조회 시작] gameId : {}, userId : {}", gameId, userId);

        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        // 경기 개설자인지 조회
        validateGameCreator(gameEntity, userEntity);

        // 지원자 목록 조회 (엔티티 기준)
        Page<ParticipantGameEntity> pages = getParticipantGameList(pageable, gameEntity, APPLY);


        // DTO 변환
        List<ApplyGameUserListDto> participantGameList = pages.stream().map(ApplyGameUserListDto::fromEntity).toList();

        log.info("[경기 참가 신청자 조회 완료] gameId : {}, userId : {}", gameId, userId);

        return CommonResponse.of("경기 신청자 조회가 완료되었습니다.", participantGameList);
    }




    /**
     * 경기 참가 수락자 조회
     */
    @Override
    @Transactional(readOnly = true)
    public CommonResponse<List<AcceptGameUserListDto>> getAcceptParticipantList(Long gameId, Long userId, Pageable pageable) {

        log.info("[경기 참가 수락자 조회 시작] gameId : {}, userId : {}", gameId, userId);


        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        // 경기 개설자인지 조회
        validateGameCreator(gameEntity, userEntity);

        // 지원자 목록 조회 (엔티티 기준)
        Page<ParticipantGameEntity> pages = getParticipantGameList(pageable, gameEntity, ACCEPT);


        // DTO 변환
        List<AcceptGameUserListDto> participantGameList = pages.stream().map(AcceptGameUserListDto::fromEntity).toList();

        log.info("[경기 참가 수락자 조회 완료] gameId : {}, userId : {}", gameId, userId);


        return CommonResponse.of("경기 참가자 조회가 완료되었습니다.", participantGameList);
    }



    /**
     * 경기 수락
     */
    @Override
    @Transactional
    public CheckResponse acceptGameUser(Long participantUserId, Long userId, Long gameId) {

        log.info("[참가자 경기 수락 시작] participantId : {}, gameId : {}", participantUserId, gameId);

        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        // 경기 개설자인지 조회
        validateGameCreator(gameEntity, userEntity);



        if (gameEntity.getParticipantCount() >= gameEntity.getHeadCount()) {
            throw new CustomException(FULL_HEADCOUNT_GAME);
        }

        LocalDateTime now = validateStartDateTime(gameEntity);




        ParticipantGameEntity participantGameEntity = getParticipantGame(gameEntity, participantUserId);

        // 경기 참가자 상태 유효성 검사
        validateGameStatusInAcceptAndReject(participantGameEntity.getParticipantGameStatus());

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ACCEPT, now);


        updateGameUserLevel(gameEntity);

        notificationService.send(NotificationType.ACCEPT_GAME, participantGameEntity.getUserEntity(),
                participantGameEntity.getGameEntity().getTitle() + "에 참가가 수락되었습니다.");




        log.info("[참가자 경기 수락 완료] participantId : {}, gameId : {}", participantUserId, gameId);

        return CheckResponse.of(true, "경기 수락이 완료되었습니다.");
    }





    /**
     * 경기 거절
     */
    @Override
    @Transactional
    public CheckResponse rejectGameUser(Long participantUserId, Long userId, Long gameId) {

        log.info("[경기 참가자 거절 시작] participantId : {}, gameId : {}", participantUserId, gameId);

        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }


        ParticipantGameEntity participantGameEntity = getParticipantGame(gameEntity, participantUserId);

        // 경기 참가자 상태 유효성 검사
        validateGameStatusInAcceptAndReject(participantGameEntity.getParticipantGameStatus());


        // 경기 생성자는 거절할 수 없음.
        if (participantGameEntity.getUserEntity().getUserId().equals(gameEntity.getUserEntity().getUserId())) {
            throw new CustomException(NOT_REJECT_CREATOR);
        }

        LocalDateTime now = validateStartDateTime(gameEntity);


        participantGameEntity.setParticipantGameStatusAndRejectDateTime(REJECT, now);



        notificationService.send(NotificationType.REJECT_GAME, participantGameEntity.getUserEntity(), participantGameEntity.getGameEntity().getTitle() + "에 참가가 거절되었습니다.");



        log.info("[경기 참가자 거절 완료] participantId : {}, gameId : {}", participantUserId, gameId);

        return CheckResponse.of(true, "경기 거절을 완료하였습니다.");
    }




    /**
     * 경기 강퇴
     */
    @Override
    @Transactional
    public CheckResponse kickOutGameUser(Long participantUserId, Long userId, Long gameId) {

        log.info("[경기 강퇴 시작] : participantId : {}, gameId : {}", participantUserId, gameId);

        GameEntity gameEntity = getGame(gameId);

        UserEntity userEntity = getUser(userId);

        validateGameCreator(gameEntity, userEntity);


        ParticipantGameEntity participantGameEntity = getParticipantGame(gameEntity, participantUserId);

        if (Objects.equals(participantGameEntity.getUserEntity().getUserId(), gameEntity.getUserEntity().getUserId())) {
            throw new CustomException(NOT_KICKOUT_CREATOR);
        }

        // 경기 참가자 상태 유효성 검사
        validateGameStatusInKickOut(participantGameEntity.getParticipantGameStatus());


        LocalDateTime now = validateStartDateTime(gameEntity);

        participantGameEntity.setParticipantGameStatusAndKickoutDateTime(KICKOUT, now);


        gameRepository.save(participantGameEntity.getGameEntity());

        updateGameUserLevel(gameEntity);

        notificationService.send(NotificationType.KICKED_OUT, participantGameEntity.getUserEntity(), participantGameEntity.getGameEntity().getTitle() + "에서 강퇴당하였습니다.");




        log.info("[경기 강퇴 완료] participantId : {}, gameId : {}", participantUserId, gameId);

        return CheckResponse.of(true, "참가자 강퇴를 완료하였습니다.");


    }



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
        List<ParticipantGameEntity> participantGameEntityList = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(List.of(ACCEPT, APPLY), gameId);


        // 조회 유저 상태 DELETE로 변경
        participantGameEntityList.forEach(participantGame ->
                participantGame.setParticipantGameStatusAndDeletedDateTime(DELETE, now));

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


        eventPublisher.publishEvent(new GameSearchCacheBumpEvent());

        log.info("[경기 삭제 완료] userId : {}, gameId : {}", userId, gameId);


        return CheckResponse.of(true, "경기 삭제가 완료되었습니다.");
    }

    public void validateGameStatusInAcceptAndReject(ParticipantGameStatus status) {

        switch (status) {

            case ACCEPT -> throw new CustomException(ALREADY_ACCEPT_USER);
            case REJECT -> throw new CustomException(ALREADY_REJECT_USER);

        }

    }

    private void validateGameStatusInKickOut(ParticipantGameStatus status) {

        switch (status) {
            case APPLY -> throw new CustomException(NOT_ACCEPT_USER);
            case KICKOUT -> throw new CustomException(ALREADY_KICKOUT_USER);
        }
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

    private Page<ParticipantGameEntity> getParticipantGameList(Pageable pageable, GameEntity gameEntity, ParticipantGameStatus participantGameStatus) {
        return participantGameRepository.
                findByParticipantGameStatusAndGameEntity_GameId(participantGameStatus, gameEntity.getGameId(), pageable);
    }

    private ParticipantGameEntity getParticipantGame(GameEntity gameEntity, Long participantUserId) {
        return participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participantUserId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));
    }

    private static LocalDateTime validateStartDateTime(GameEntity gameEntity) {
        LocalDateTime now = LocalDateTime.now();
        if (gameEntity.getStartDateTime().isBefore(now)) {
            throw new CustomException(ALREADY_START_GAME);
        }
        return now;
    }

    public void updateGameUserLevel(GameEntity gameEntity) {

        List<ParticipantGameEntity> acceptedParticipants = participantGameRepository.findByGameEntity_GameIdAndParticipantGameStatus(gameEntity.getGameId(), ACCEPT);


        double average = acceptedParticipants.stream()
                .map(participantGameEntity -> participantGameEntity.getUserEntity().getGameUserLevel())
                .mapToDouble(GameUserLevel::getValue)
                .average()
                .orElse(0.0);

        GameUserLevel gameUserLevel = GameUserLevel.fromUserLevelAverage(average);


        gameEntity.setGameUserLevel(gameUserLevel);

    }

}