package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.AcceptGameUserListDto;
import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.service.ParticipantGameService;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantGameServiceImpl implements ParticipantGameService {

    private final ParticipantGameRepository participantGameRepository;

    private final GameRepository gameRepository;

    private final UserRepository userRepository;


    /**
     * 경기 참가 신청자 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ApplyGameUserListDto>> getApplyParticipantList(Long gameId, Long userId, Pageable pageable) {

        log.info("[경기 참가 신청자 조회 시작] gameId : {}, userId : {}", gameId, userId);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            log.error("[CustomException 발생] errorCode : {}", NOT_GAME_CREATOR);
            throw new CustomException(NOT_GAME_CREATOR);
        }

        // 지원자 목록 조회 (엔티티 기준)
        Page<ParticipantGameEntity> pages = getParticipantGameList(pageable, gameEntity, APPLY);


        // DTO 변환
        List<ApplyGameUserListDto> participantGameList = pages.stream().map(ApplyGameUserListDto::fromEntity).toList();

        log.info("[경기 참가 신청자 조회 완료] gameId : {}, userId : {}", gameId, userId);

        return ApiResponse.of("경기 신청자 조회가 완료되었습니다.", participantGameList);
    }


    /**
     * 경기 참가 수락자 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<AcceptGameUserListDto>> getAcceptParticipantList(Long gameId, Long userId, Pageable pageable) {

        log.info("[경기 참가 수락자 조회 시작] gameId : {}, userId : {}", gameId, userId);


        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            log.error("[CustomException 발생] errorCode : {}", NOT_GAME_CREATOR);
            throw new CustomException(NOT_GAME_CREATOR);
        }

        // 지원자 목록 조회 (엔티티 기준)
        Page<ParticipantGameEntity> pages = getParticipantGameList(pageable, gameEntity, ACCEPT);


        // DTO 변환
        List<AcceptGameUserListDto> participantGameList = pages.stream().map(AcceptGameUserListDto::fromEntity).toList();

        log.info("[경기 참가 수락자 조회 완료] gameId : {}, userId : {}", gameId, userId);


        return ApiResponse.of("경기 참가자 조회가 완료되었습니다.", participantGameList);
    }

    private Page<ParticipantGameEntity> getParticipantGameList(Pageable pageable, GameEntity gameEntity, ParticipantGameStatus participantGameStatus) {
        Page<ParticipantGameEntity> pages = participantGameRepository.
                findByParticipantGameStatusAndGameEntity_GameId(participantGameStatus, gameEntity.getGameId(), pageable);
        return pages;
    }

    /**
     * 경기 수락
     */
    @Override
    @Transactional
    public CheckResponse acceptGameUser(Long participantId, Long userId, Long gameId) {

        log.info("[참가자 경기 수락 시작] participantId : {}, gameId : {}", participantId, gameId);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            log.error("[CustomException 발생] errorCode : {}", NOT_GAME_CREATOR);
            throw new CustomException(NOT_GAME_CREATOR);
        }

        boolean exists = participantGameRepository.existsByParticipantGameIdAndGameEntity_GameId(participantId, gameId);

        if (!exists) {
            throw new CustomException(NOT_APPLY_USER);
        }
        LocalDateTime now = LocalDateTime.now();
        if (gameEntity.getStartDateTime().isBefore(now)) {
            throw new CustomException(ALREADY_START_GAME);
        }

        if (gameEntity.getParticipantCount() >= gameEntity.getHeadCount()) {
            throw new CustomException(FULL_HEADCOUNT_GAME);
        }




        ParticipantGameEntity participantGameEntity = participantGameRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        if (participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
            throw new CustomException(ALREADY_ACCEPT_USER);
        }

        participantGameEntity.setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus.ACCEPT, now);

        participantGameRepository.save(participantGameEntity);



        log.info("[참가자 경기 수락 완료] participantId : {}, gameId : {}", participantId, gameId);

        return CheckResponse.of(true, "경기 수락이 완료되었습니다.");
    }


    /**
     * 경기 거절
     */
    @Override
    @Transactional
    public CheckResponse rejectGameUser(Long participantId, Long userId, Long gameId) {

        log.info("[경기 참가자 거절 시작] participantId : {}, gameId : {}", participantId, gameId);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }


        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndParticipantGameId(gameId, participantId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));


        // 경기 생성자는 거절할 수 없음.
        if (participantGameEntity.getUserEntity().getUserId().equals(gameEntity.getUserEntity().getUserId())) {
            throw new CustomException(NOT_REJECT_CREATOR);
        }

        if (participantGameEntity.getParticipantGameStatus().equals(REJECT)) {
            throw new CustomException(ALREADY_REJECT_USER);
        }

        if (participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
            throw new CustomException(ALREADY_ACCEPT_USER);
        }

        if (gameEntity.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new CustomException(ALREADY_START_GAME);
        }

        participantGameEntity.setParticipantGameStatusAndRejectDateTime(REJECT, LocalDateTime.now());

        participantGameRepository.save(participantGameEntity);

        log.info("[경기 참가자 거절 완료] participantId : {}, gameId : {}", participantId, gameId);

        return CheckResponse.of(true, "경기 거절을 완료하였습니다.");
    }


    /**
     * 경기 강퇴
     */
    @Override
    @Transactional
    public CheckResponse kickOutGameUser(Long participantId, Long userId, Long gameId) {

        log.info("[경기 강퇴 시작] : participantId : {}, gameId : {}", participantId, gameId);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }


        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndParticipantGameId(gameId, participantId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));

        if (Objects.equals(participantGameEntity.getUserEntity().getUserId(), gameEntity.getUserEntity().getUserId())) {
            throw new CustomException(NOT_KICKOUT_CREATOR);
        }

        if (participantGameEntity.getParticipantGameStatus().equals(KICKOUT)) {
            throw new CustomException(ALREADY_KICKOUT_USER);
        }

        if (!participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
            throw new CustomException(NOT_ACCEPT_USER);
        }

        LocalDateTime now = LocalDateTime.now();

        if (gameEntity.getStartDateTime().isBefore(now)) {
            throw new CustomException(ALREADY_START_GAME);
        }

        participantGameEntity.setParticipantGameStatusAndKickoutDateTime(KICKOUT, now);
        participantGameRepository.save(participantGameEntity);

        gameEntity.decreaseParticipantCount();
        gameRepository.save(gameEntity);

        log.info("[경기 강퇴 완료] participantId : {}, gameId : {}", participantId, gameId);

        return CheckResponse.of(true, "참가자 강퇴를 완료하였습니다.");


    }

    /**
     * 경기 삭제
     */
    @Override
    @Transactional
    public CheckResponse deleteGame(Long userId, Long gameId) {

        log.info("[경기 삭제 시작] userId : {}, gameId : {}", userId, gameId);

        GameEntity gameEntity = gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        if (!Objects.equals(gameEntity.getUserEntity().getUserId(), userEntity.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(gameEntity.getStartDateTime().minusMinutes(30))) {
            throw new CustomException(NOT_DELETE_GAME);
        }

        List<ParticipantGameEntity> participantGameEntityList = participantGameRepository.findByParticipantGameStatusInAndGameEntity_GameId(List.of(ACCEPT, APPLY), gameId);


        participantGameEntityList.forEach(participantGame ->
                participantGame.setParticipantGameStatusAndDeletedDateTime(DELETE, now));

        participantGameRepository.saveAll(participantGameEntityList);

        gameEntity.setDeletedDateTime(now);

        gameRepository.save(gameEntity);

        log.info("[경기 삭제 완료] userId : {}, gameId : {}", userId, gameId);


        return CheckResponse.of(true, "경기 삭제가 완료되었습니다.");
    }


}
