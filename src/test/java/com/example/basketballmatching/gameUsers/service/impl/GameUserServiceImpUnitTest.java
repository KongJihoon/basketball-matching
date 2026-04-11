package com.example.basketballmatching.gameUsers.service.impl;


import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameUserServiceImpUnitTest {


    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameUserServiceImpl gameUserService;

    private UserEntity creator;


    private UserEntity participant;

    private GameEntity gameEntity;

    @BeforeEach
    public void setUp() {
        creator = UserEntity.builder()
                .userId(1L)
                .emailAuth(true)
                .userType(UserType.USER)
                .build();

        participant = UserEntity.builder()
                .userId(2L)
                .emailAuth(true)
                .genderType(GenderType.MALE)
                .userType(UserType.USER)
                .build();

        gameEntity = GameEntity.builder()
                .gameId(1L)
                .title("주말 3대3 같이 하실 분")
                .content("초보~중수 환영 / 즐겜 / 노쇼 금지")
                .headCount(6) // 3vs3 최소 6
                .fieldStatus(FieldStatus.OUTDOOR) // 예시 (실제 enum에 맞게)
                .matchGenderType(MatchGenderType.MIXED) // 예시
                .startDateTime(LocalDateTime.of(2025, 12, 20, 19, 0))
                .endDateTime(LocalDateTime.of(2025, 12, 20, 20, 30))
                .placeName("잠실종합운동장 농구장")
                .address("서울특별시 송파구 올림픽로 25") // CityName.getCityName(address)에서 뽑을 주소
                .latitude(37.515)   // 예시
                .longitude(127.073) // 예시
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .userEntity(creator)
                .build();
    }

    @Test
    @DisplayName("경기 참가 테스트")
    void applyGameTest() {
        // given

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(participant.getUserId()))
                .thenReturn(Optional.of(participant));

        when(gameRepository.findByGameIdWithLock(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participant.getUserId()))
                .thenReturn(Optional.empty());

        when(participantGameRepository.save(any(ParticipantGameEntity.class))).thenAnswer(inv -> inv.getArgument(0));




        // when

        CommonResponse<ApplyGameUserDto> commonResponse = gameUserService.applyGame(gameEntity.getGameId(), participant.getUserId());

        // then

        ArgumentCaptor<ParticipantGameEntity> participantCaptor = ArgumentCaptor.forClass(ParticipantGameEntity.class);



        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(participant.getUserId());
        verify(gameRepository).findByGameIdWithLock(gameEntity.getGameId());
        verify(participantGameRepository).findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participant.getUserId());
        verify(participantGameRepository).save(participantCaptor.capture());

        ParticipantGameEntity savedParticipant = participantCaptor.getValue();

        assertEquals("경기 신청이 완료되었습니다.", commonResponse.getMessage());
        assertEquals(participant.getUserId(), commonResponse.getData().getUserId());
        assertEquals(gameEntity.getGameId(), commonResponse.getData().getGameId());

        assertEquals(gameEntity.getGameId(), savedParticipant.getGameEntity().getGameId());
        assertEquals(participant.getUserId(), savedParticipant.getUserEntity().getUserId());

        verifyNoMoreInteractions(userRepository, gameRepository, participantGameRepository);

    }

    @Test
    @DisplayName("경기 참가 실패 테스트 - 경기 시작 시간 30분전 참가 신청 불가")
    void applyGameFailTest_NOT_ALLOWED_TO_JOIN() {
        // given

        LocalDateTime now = LocalDateTime.now();

        gameEntity.setStartDateTime(now.plusMinutes(30));
        gameEntity.setEndDateTime(now.plusMinutes(70));

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(participant.getUserId()))
                .thenReturn(Optional.of(participant));

        when(gameRepository.findByGameIdWithLock(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participant.getUserId()))
                .thenReturn(Optional.empty());


        // when


        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.applyGame(gameEntity.getGameId(), participant.getUserId()));


        // then

        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(participant.getUserId());
        verify(gameRepository).findByGameIdWithLock(gameEntity.getGameId());
        verify(participantGameRepository).findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participant.getUserId());
        verify(participantGameRepository, never()).save(any());

        assertEquals(ErrorCode.NOT_ALLOWED_TO_JOIN, exception.getErrorCode());

        verifyNoMoreInteractions(userRepository, gameRepository, participantGameRepository);

    }

    @Test
    @DisplayName("경기 참가 실패 테스트 - 경기 개설자 참가 신청 시 참가 신청 불가")
    void applyGameFailTest_NOT_APPLY_GAME_CREATOR() {
        // given
        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(creator.getUserId()))
                .thenReturn(Optional.of(creator));

        when(gameRepository.findByGameIdWithLock(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), creator.getUserId()))
                .thenReturn(Optional.empty());


        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.applyGame(gameEntity.getGameId(), creator.getUserId()));

        // then

        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(creator.getUserId());
        verify(gameRepository).findByGameIdWithLock(gameEntity.getGameId());
        verify(participantGameRepository).findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), creator.getUserId());
        verify(participantGameRepository, never()).save(any());

        assertEquals(ErrorCode.NOT_APPLY_GAME_CREATOR, exception.getErrorCode());

        verifyNoMoreInteractions(userRepository, participantGameRepository, gameRepository);

    }

    @Test
    @DisplayName("경기 참가 취소 테스트")
    void cancelGame() {
        // given

        ParticipantGameEntity applyUser = ParticipantGameEntity.createApply(gameEntity, participant);

        applyUser.accept(LocalDateTime.now());

        when(gameRepository.findByGameIdWithLock(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(
                gameEntity.getGameId(), participant.getUserId()
        )).thenReturn(Optional.of(applyUser));


        // when

        CheckResponse checkResponse = gameUserService.cancelGame(participant.getUserId(), gameEntity.getGameId());



        // then

        assertTrue(checkResponse.isSuccess());
        assertEquals("경기 취소가 완료되었습니다.", checkResponse.getMessage());

        assertEquals(ParticipantGameStatus.CANCEL, applyUser.getParticipantGameStatus());

        verify(gameRepository).findByGameIdWithLock(gameEntity.getGameId());
        verify(participantGameRepository).findByGameEntity_GameIdAndUserEntity_UserId(
                gameEntity.getGameId(), participant.getUserId()
        );
        verifyNoMoreInteractions(gameRepository, participantGameRepository);


    }

    @Test
    @DisplayName("경기 취소 실패 테스트 - 이미 취소한 유저 취소 불가")
    void cancelGameFailTest_ALREADY_CANCELED_USER() {
        // given
        ParticipantGameEntity applyUser = ParticipantGameEntity.createApply(gameEntity, participant);

        applyUser.cancel( LocalDateTime.now());

        when(gameRepository.findByGameIdWithLock(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(participantGameRepository.findByGameEntity_GameIdAndUserEntity_UserId(
                gameEntity.getGameId(), participant.getUserId()
        )).thenReturn(Optional.of(applyUser));


        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameUserService.cancelGame(participant.getUserId(), gameEntity.getGameId()));

        // then

        verify(gameRepository).findByGameIdWithLock(gameEntity.getGameId());
        verify(participantGameRepository).findByGameEntity_GameIdAndUserEntity_UserId(gameEntity.getGameId(), participant.getUserId());

        assertEquals(ErrorCode.ALREADY_CANCELED_USER, exception.getErrorCode());

        verifyNoMoreInteractions(gameRepository, participantGameRepository);


    }



}