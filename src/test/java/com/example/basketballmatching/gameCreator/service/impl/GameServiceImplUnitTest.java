package com.example.basketballmatching.gameCreator.service.impl;

import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.dto.GameCreatedEventDto;
import com.example.basketballmatching.gameCreator.dto.GameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.entity.ParticipantGameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.lock.RedissonLockExecutor;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedissonLockExecutor lockExecutor;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private GameServiceImpl gameService;


    private UserEntity userEntity;



    private GameEntity gameEntity;

    @BeforeEach
    public void setUp() {
        userEntity = UserEntity.builder()
                .userId(1L)
                .emailAuth(true)
                .userType(UserType.USER)
                .build();

        gameEntity = GameEntity.builder()
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
                .userEntity(userEntity)
                .build();

    }

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


        when(gameRepository.save(any(GameEntity.class))).thenAnswer(inv -> inv.getArgument(0));


        when(participantGameRepository.save(any(ParticipantGameEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // when

        CommonResponse<CreateGameDto.Response> commonResponse = gameService.createGame(userId, request);



        // then

        assertNotNull(commonResponse.getData());
        assertEquals("경기 생성이 완료되었습니다.", commonResponse.getMessage());

        ArgumentCaptor<GameEntity> gameCaptor = ArgumentCaptor.forClass(GameEntity.class);

        ArgumentCaptor<ParticipantGameEntity> participantCaptor = ArgumentCaptor.forClass(ParticipantGameEntity.class);


        verify(userRepository).findById(userId);
        verify(gameRepository).existsBySamePlaceAtSameTime(any(), any(), any(), any());
        verify(gameRepository).save(gameCaptor.capture());
        verify(participantGameRepository).save(participantCaptor.capture());

        GameEntity savedGame = gameCaptor.getValue();
        ParticipantGameEntity savedParticipant = participantCaptor.getValue();

        assertEquals(request.getTitle(), savedGame.getTitle());
        assertEquals(request.getAddress(), savedGame.getAddress());
        assertEquals(userEntity, savedGame.getUserEntity());

        assertEquals(savedGame, savedParticipant.getGameEntity());
        assertEquals(userEntity, savedParticipant.getUserEntity());

    }

    @Test
    @DisplayName("경기 생성 실패 테스트 - 경기 시작시간 30분 전 신청")
    void createGameFailTest_INVALID_GAME_TIME() {
        // given

        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("주말 3대3 같이 하실 분")
                .content("초보~중수 환영 / 즐겜 / 노쇼 금지")
                .headCount(6) // 3vs3 최소 6
                .fieldStatus(FieldStatus.OUTDOOR) // 예시 (실제 enum에 맞게)
                .matchGenderType(MatchGenderType.MIXED) // 예시
                .startDateTime(LocalDateTime.of(2024, 12, 20, 19, 0))
                .endDateTime(LocalDateTime.of(2024, 12, 20, 20, 30))
                .placeName("잠실종합운동장 농구장")
                .address("서울특별시 송파구 올림픽로 25") // CityName.getCityName(address)에서 뽑을 주소
                .latitude(37.515)   // 예시
                .longitude(127.073) // 예시
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userEntity.getUserId()))
                .thenReturn(Optional.of(userEntity));




        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameService.createGame(userEntity.getUserId(), request));

        // then

        assertEquals(ErrorCode.INVALID_GAME_TIME, exception.getErrorCode());

        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(userEntity.getUserId());

        verifyNoInteractions(gameRepository, participantGameRepository);
    }

    @Test
    @DisplayName("경기 생성 성공 시 이벤트 발행 테스트")
    void createGame_publishEvent() {

        Long userId = 1L;

        // given
        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("주말 3대3 같이 하실 분")
                .content("초보~중수 환영 / 즐겜 / 노쇼 금지")
                .headCount(6) // 3vs3 최소 6
                .fieldStatus(FieldStatus.OUTDOOR) // 예시 (실제 enum에 맞게)
                .matchGenderType(MatchGenderType.MIXED) // 예시
                .startDateTime(LocalDateTime.of(2026, 12, 20, 19, 0))
                .endDateTime(LocalDateTime.of(2026, 12, 20, 20, 30))
                .placeName("잠실종합운동장 농구장")
                .address("서울특별시 송파구 올림픽로 25") // CityName.getCityName(address)에서 뽑을 주소
                .latitude(37.515)   // 예시
                .longitude(127.073) // 예시
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userId))
                .thenReturn(Optional.of(userEntity));

        when(gameRepository.existsBySamePlaceAtSameTime(any(), any(), any(), any()))
                .thenReturn(false);

        when(gameRepository.save(any(GameEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        when(participantGameRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(lockExecutor.executeWithLock(anyString(), anyLong(), anyLong(), any()))
                .thenAnswer(inv -> {
                    Callable<?> actions = inv.getArgument(3);
                    return actions.call();
                });

        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> {
                    TransactionCallback<?> cb = inv.getArgument(0);
                    return cb.doInTransaction(mock(TransactionStatus.class));
                });


        // when

        gameService.createGame(userId, request);


        // then

        verify(eventPublisher, times(1)).publishEvent(any(GameCreatedEventDto.class));

    }


    @Test
    @DisplayName("경기 상세 조회 테스트")
    void detailGameTest() {
        // given

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameEntity.getGameId())).thenReturn(Optional.of(gameEntity));

        // when

        CommonResponse<GameDto> commonResponse = gameService.detailGame(gameEntity.getGameId());

        // then

        assertEquals("경기 상세조회에 성공하였습니다.", commonResponse.getMessage());
        assertEquals(gameEntity.getGameId(), commonResponse.getData().getGameId());
    }

    @Test
    @DisplayName("경기 수정 테스트")
    void editGameTest() {
        // given


        EditGameDto request = EditGameDto.builder()
                .title("수정 테스트 제목")
                .content("수정 테스트 내용")
                .headCount(9)
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userEntity.getUserId()))
                .thenReturn(Optional.of(userEntity));



        // when

        CommonResponse<GameDto> commonResponse = gameService.editGame(request, gameEntity.getGameId(), userEntity.getUserId());


        // then

        assertEquals("경기 수정이 완료되었습니다.", commonResponse.getMessage());
        assertEquals(gameEntity.getGameId(), commonResponse.getData().getGameId());

        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(userEntity.getUserId());
        verify(gameRepository).findByGameIdAndDeletedDateTimeIsNull(gameEntity.getGameId());

        verifyNoMoreInteractions(gameRepository, userRepository);
    }

    @Test
    @DisplayName("경기 수정 실패 테스트 - 경기 형식 최대 인원수 초과")
    void editGameFailTest_INVALID_HEADCOUNT() {
        // given

        EditGameDto request = EditGameDto.builder()
                .title("수정 테스트 제목")
                .content("수정 테스트 내용")
                .headCount(12)
                .build();


        when(gameRepository.findByGameIdAndDeletedDateTimeIsNull(gameEntity.getGameId()))
                .thenReturn(Optional.of(gameEntity));

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(userEntity.getUserId()))
                .thenReturn(Optional.of(userEntity));

        // when

        CustomException exception = assertThrows(CustomException.class, () -> gameService.editGame(request, gameEntity.getGameId(), userEntity.getUserId()));

        // then

        assertEquals(ErrorCode.INVALID_HEADCOUNT, exception.getErrorCode());

        verify(gameRepository).findByGameIdAndDeletedDateTimeIsNull(gameEntity.getGameId());
        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(userEntity.getUserId());

    }



}