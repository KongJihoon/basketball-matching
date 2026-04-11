package com.example.basketballmatching.gameUsers.service.impl;

import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameQueryRepository;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.GameStatus;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameUsers.dto.CurrentGameListDto;
import com.example.basketballmatching.gameUsers.dto.GameUserLevelDto;
import com.example.basketballmatching.gameUsers.dto.LastGameListDto;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class GameUserServiceImpl implements GameUserService {

    private final ParticipantGameRepository participantGameRepository;

    private final UserRepository userRepository;

    private final GameRepository gameRepository;
    private final GameQueryRepository gameQueryRepository;
    private final RedisService redisService;

    /**
     * 경기 참가 신청
     */
    @Override
    @Transactional
    public CommonResponse<ApplyGameUserDto> applyGame(Long gameId, Long userId) {
        log.info("[경기 참가 신청 시작] gameId : {} userId : {}", gameId, userId);


        UserEntity userEntity = getUser(userId);

        GameEntity gameEntity = getGameWithLock(gameId);

        String data = redisService.getData("blackList:" + userEntity.getEmail());

        if (data != null) {
            throw new CustomException(BLACKLIST_USER);
        }

        if (gameEntity.getGameStatus().equals(GameStatus.CLOSED)) {
            throw new CustomException(CLOSED_GAME);
        }

        ParticipantGameEntity participantGameEntity = participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElse(null);

        validateParticipantInfo(userEntity, gameEntity, participantGameEntity);

        if (participantGameEntity == null) {

            participantGameEntity = ParticipantGameEntity.createApply(gameEntity, userEntity);


            participantGameRepository.save(participantGameEntity);


            if (gameEntity.getParticipantCount() >= gameEntity.getHeadCount()) {
                gameEntity.setStatue(GameStatus.CLOSED);
            }


        } else if (participantGameEntity.getParticipantGameStatus().equals(CANCEL)) {

            participantGameEntity.reApply();
        }

        ApplyGameUserDto participantDto = ApplyGameUserDto.fromEntity(participantGameEntity);


        log.info("[경기 참가 신청 완료] gameId : {}, participantId : {}", gameId, participantGameEntity.getParticipantGameId());

        return CommonResponse.of("경기 신청이 완료되었습니다.", participantDto);
    }


    /**
     * 경기 참가 취소
     */
    @Override
    @Transactional
    public CheckResponse cancelGame(Long userId, Long gameId) {

        GameEntity gameEntity = getGameWithLock(gameId);

        ParticipantGameEntity participantGameEntity = getParticipantGame(userId, gameId);


        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(gameEntity.getStartDateTime().minusMinutes(30))) {
            throw new CustomException(NOT_ALLOWED_CANCEL);
        }

        if (participantGameEntity.getParticipantGameStatus().equals(CANCEL)) {
            throw new CustomException(ALREADY_CANCELED_USER);
        }

        if (participantGameEntity.getParticipantGameStatus().equals(KICKOUT)) {
            throw new CustomException(ALREADY_KICKOUT_USER);
        }


        if (!participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
            throw new CustomException(NOT_ACCEPT_USER);
        }


        participantGameEntity.cancel(now);





        return CheckResponse.of(true, "경기 취소가 완료되었습니다.");
    }


    /**
     * 현재 예정 경기 조회
     */
    @Override
    @Transactional(readOnly = true)
    public CommonResponse<List<CurrentGameListDto>> getMyCurrentGameList(Long userId, Pageable pageable) {

        UserEntity userEntity = getUser(userId);


        List<CurrentGameListDto> currentGameList = gameQueryRepository.getCurrentGameList(userEntity.getUserId(), pageable);

        return CommonResponse.of("현재 예정된 게임 조회가 완료되었습니다.", currentGameList);
    }

    /**
     * 지난 경기 조회
     */
    @Override
    @Transactional(readOnly = true)
    public CommonResponse<List<LastGameListDto>> getMyLastGameList(Long userId, Pageable pageable) {

        UserEntity userEntity = getUser(userId);


        List<LastGameListDto> lastGameList = gameQueryRepository.getLastGameList(userEntity.getUserId(), pageable);

        return CommonResponse.of("지난 게임 조회가 완료되었습니다.", lastGameList);
    }


    @Override
    @Transactional(readOnly = true)
    public CommonResponse<GameUserLevelDto> getMyGameUserLevel(Long userId) {

        UserEntity userEntity = getUser(userId);

        GameUserLevelDto gameUserLevelDto = GameUserLevelDto.fromEntity(userEntity);


        return CommonResponse.of("유저 랭크 조회를 완료하였습니다.", gameUserLevelDto);
    }


    private UserEntity getUser(Long userId) {
        return userRepository.findByUserIdAndDeletedDateTimeIsNull(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private ParticipantGameEntity getParticipantGame(Long userId, Long gameId) {
        return participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameId, userId)
                .orElseThrow(() -> new CustomException(PARTICIPANT_NOT_FOUND));
    }


    private GameEntity getGameWithLock(Long gameId) {
        return gameRepository.findByGameIdWithLock(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));
    }


    private void validateParticipantInfo(UserEntity userEntity, GameEntity gameEntity, ParticipantGameEntity participantGameEntity) {

        LocalDateTime now = LocalDateTime.now();

        if (Objects.equals(userEntity.getUserId(), gameEntity.getUserEntity().getUserId())) {
            throw new CustomException(NOT_APPLY_GAME_CREATOR);
        }

        if (participantGameEntity != null) {

            if (participantGameEntity.getParticipantGameStatus().equals(KICKOUT)) {
                throw new CustomException(NOT_APPLY_KICKOUT_USER);
            }

            if (participantGameEntity.getParticipantGameStatus().equals(ACCEPT)) {
                throw new CustomException(ALREADY_ACCEPT_USER);
            }

            if (participantGameEntity.getParticipantGameStatus().equals(APPLY)) {
                throw new CustomException(ALREADY_APPLY_GAME_USER);
            }

        }


        if (gameEntity.getParticipantCount() >= gameEntity.getHeadCount()) {
            throw new CustomException(FULL_HEADCOUNT_GAME);
        }

        if (now.isAfter(gameEntity.getStartDateTime().minusMinutes(30))) {
            throw new CustomException(NOT_ALLOWED_TO_JOIN);
        }

        if (gameEntity.getMatchGenderType().equals(MatchGenderType.FEMALE_ONLY) &&
                userEntity.getGenderType().equals(GenderType.MALE)) {
            throw new CustomException(ONLY_FEMALE_GAME);
        }


        if (gameEntity.getMatchGenderType().equals(MatchGenderType.MALE_ONLY) &&
                userEntity.getGenderType().equals(GenderType.FEMALE)) {
            throw new CustomException(ONLY_MALE_GAME);
        }


    }
}
