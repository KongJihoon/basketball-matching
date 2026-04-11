package com.example.basketballmatching.blackList.service.impl;

import com.example.basketballmatching.blackList.entity.BlackListEntity;
import com.example.basketballmatching.blackList.repository.BlackListRepository;
import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.service.RedisService;
import com.example.basketballmatching.report.dto.CreateReportDto;
import com.example.basketballmatching.report.entity.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.type.ReportType;
import com.example.basketballmatching.user.entity.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.basketballmatching.gameCreator.type.ParticipantGameStatus.APPLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlackListServiceImpUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BlackListRepository blackListRepository;

    @Mock
    private ParticipantGameRepository participantGameRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private BlackListServiceImpl blackListService;


    UserEntity admin;

    UserEntity blackList;

    GameEntity gameEntity;

    ReportEntity reportEntity;

    @BeforeEach
    void setup() {

        admin = UserEntity.builder()
                .email("creator@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("name")
                .nickname("name")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소")
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.ADMIN)
                .build();

        admin.setEmailAuth();


        blackList = UserEntity.builder()
                .email("participant@example.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("participant")
                .nickname("participant")
                .birth(LocalDate.of(1997,7,24))
                .address("테스트용주소")
                .phone("010-1111-1111")
                .position(Position.GUARD)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .userType(UserType.USER)
                .build();

        blackList.setEmailAuth();


        CreateGameDto.Request request = CreateGameDto.Request.builder()
                .title("테스트 게임")
                .content("테스트 게임 본문")
                .headCount(9)
                .fieldStatus(FieldStatus.OUTDOOR)
                .matchGenderType(MatchGenderType.MALE_ONLY)
                .startDateTime(LocalDateTime.now().plusHours(2L))
                .endDateTime(LocalDateTime.now().plusHours(3L))
                .placeName("테스트 게임 장소")
                .address("인천광역시 테스트 게임 주소")
                .matchFormat(MatchFormat.THREE_ON_THREE)
                .build();

        gameEntity = GameEntity.create(request.getTitle(), request.getContent(), request.getHeadCount(), request.getFieldStatus(), request.getMatchFormat(), request.getMatchGenderType(), request.getStartDateTime()
        , request.getEndDateTime(), request.getPlaceName(), request.getAddress(), CityName.getCityName(request.getAddress()), request.getLatitude(), request.getLongitude(), admin);

        gameEntity.setGameUserLevel(GameUserLevel.AMATEUR);


        CreateReportDto createDto = CreateReportDto.builder()
                .reportType(ReportType.ABUSIVE_LANGUAGE)
                .content("비신사적 플레이")
                .build();

        reportEntity = ReportEntity.create(admin, blackList, gameEntity, createDto);

    }

    @Test
    @DisplayName("블랙리스트 등록 테스트")
    void createBlackListTest() {
        // given

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(admin.getUserId())).thenReturn(Optional.of(admin));

        when(reportRepository.findById(reportEntity.getReportId())).thenReturn(Optional.of(reportEntity));


        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(reportEntity.getTargetUser().getUserId())).thenReturn(Optional.of(blackList));

        when(redisService.getData("blackList:" + blackList.getEmail())).thenReturn(null);

        when(blackListRepository.existsByUserEntity_UserId(blackList.getUserId())).thenReturn(false);

        when(participantGameRepository.findByUserEntity_UserIdAndParticipantGameStatusIn(blackList.getUserId(), List.of(ParticipantGameStatus.ACCEPT, APPLY))).thenReturn(anyList());
        // when

        CheckResponse checkResponse = blackListService.createBlackListUser(admin.getUserId(), reportEntity.getReportId());


        // then

        assertEquals("신고 유저 블랙리스트 등록에 성공하였습니다.", checkResponse.getMessage());
        assertTrue(checkResponse.isSuccess());

        ArgumentCaptor<BlackListEntity> blackListCaptor = ArgumentCaptor.forClass(BlackListEntity.class);
        ArgumentCaptor<ReportEntity> reportCaptor = ArgumentCaptor.forClass(ReportEntity.class);

        verify(redisService).getData("blackList:" + blackList.getEmail());
        verify(reportRepository).findById(reportEntity.getReportId());
        verify(blackListRepository).existsByUserEntity_UserId(blackList.getUserId());
        verify(participantGameRepository).findByUserEntity_UserIdAndParticipantGameStatusIn(blackList.getUserId(), List.of(ParticipantGameStatus.ACCEPT, APPLY));
        verify(blackListRepository).save(blackListCaptor.capture());
        verify(redisService).setDataExpireDays(any(), any(), any());
        verify(participantGameRepository).saveAll(anyList());
        verify(reportRepository).save(reportCaptor.capture());

        BlackListEntity captorValue = blackListCaptor.getValue();
        ReportEntity reportCaptorValue = reportCaptor.getValue();


        assertTrue(reportCaptorValue.isBanned());
        assertEquals(blackList.getUserId(), captorValue.getUserEntity().getUserId());


        verifyNoMoreInteractions(userRepository, reportRepository,participantGameRepository, blackListRepository);

    }




}