package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.UserType;
import com.nimbusds.jose.JWEHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @InjectMocks
    private GameServiceImpl gameService;

    @Test
    @DisplayName("경기 생성 테스트")
    void createGameTest() {
        // given

        Long userId = 1L;

        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .emailAuth(true)
                .userType(UserType.USER)
                .build();

        CreateGameDto.Request request = CreateGameDto.Request.builder()
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
                .build();





        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        when(gameRepository.existsBySamePlaceAtSameTime(any(), any(), any(), any())).thenReturn(false);

        ArgumentCaptor<GameEntity> captor = ArgumentCaptor.forClass(GameEntity.class);

        when(gameRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ParticipantGameEntity> captor1 = ArgumentCaptor.forClass(ParticipantGameEntity.class);

        when(participantGameRepository.save(captor1.capture())).thenAnswer(inv -> inv.getArgument(0));

        // when

        CommonResponse<CreateGameDto.Response> commonResponse = gameService.createGame(userId, request);


        // then

        assertNotNull(commonResponse.getData());
        assertEquals("경기 생성이 완료되었습니다.", commonResponse.getMessage());


        GameEntity saved = captor.getValue();

        ParticipantGameEntity savedParticipant = captor1.getValue();

        verify(userRepository).findById(userId);
        verify(gameRepository).existsBySamePlaceAtSameTime(any(), any(), any(), any());
        verify(gameRepository).save(saved);
        verify(participantGameRepository).save(savedParticipant);

    }


}