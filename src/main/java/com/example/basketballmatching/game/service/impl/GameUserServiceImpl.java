package com.example.basketballmatching.game.service.impl;

import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.domain.ParticipantGameEntity;
import com.example.basketballmatching.game.repository.query.GameQueryRepository;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.repository.ParticipantGameRepository;
import com.example.basketballmatching.game.type.GameStatus;
import com.example.basketballmatching.game.type.MatchGenderType;
import com.example.basketballmatching.game.dto.ApplyGameUserDto;
import com.example.basketballmatching.game.dto.CurrentGameListDto;
import com.example.basketballmatching.game.dto.GameUserLevelDto;
import com.example.basketballmatching.game.dto.LastGameListDto;
import com.example.basketballmatching.game.service.GameUserService;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.user.domain.UserEntity;
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

import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
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
