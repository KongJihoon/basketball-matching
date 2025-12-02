package com.example.basketballmatching.blackList.service.impl;

import com.example.basketballmatching.blackList.repository.BlackListRepository;
import com.example.basketballmatching.blackList.service.BlackListService;
import com.example.basketballmatching.gameCreator.dto.CreateGameDto;
import com.example.basketballmatching.gameCreator.entity.GameEntity;
import com.example.basketballmatching.gameCreator.repository.GameRepository;
import com.example.basketballmatching.gameCreator.repository.ParticipantGameRepository;
import com.example.basketballmatching.gameCreator.type.FieldStatus;
import com.example.basketballmatching.gameCreator.type.MatchFormat;
import com.example.basketballmatching.gameCreator.type.MatchGenderType;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
@ActiveProfiles("local-test")
class BlackListServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    UserEntity admin;

    UserEntity blackList;

    GameEntity gameEntity;

    ReportEntity reportEntity;
    @Autowired
    private BlackListService blackListService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private BlackListRepository blackListRepository;

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

        userRepository.save(admin);

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

        userRepository.save(blackList);

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

        gameEntity = CreateGameDto.Request.toEntity(request, admin);

        gameRepository.save(gameEntity);

        CreateReportDto createDto = CreateReportDto.builder()
                .reportType(ReportType.ABUSIVE_LANGUAGE)
                .content("비신사적 플레이")
                .build();

        reportEntity = ReportEntity.create(admin, blackList, gameEntity, createDto);

        reportRepository.save(reportEntity);

    }

    @Test
    @DisplayName("블랙리스트 유저 등록 테스트")
    void createBlackListUserTest() {
        // given

        Long adminId = admin.getUserId();

        Long reportId = reportEntity.getReportId();



        // when

        CheckResponse checkResponse = blackListService.createBlackListUser(adminId, reportId);

        boolean exists = blackListRepository.existsByUserEntity_UserId(blackList.getUserId());

        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REPORT));

        String data = redisService.getData("blackList:" + blackList.getEmail());

        Long expiration = redisService.getExpiration("blackList:" + blackList.getEmail());

        // then

        assertEquals("신고 유저 블랙리스트 등록에 성공하였습니다.", checkResponse.getMessage());
        assertNotNull(data);
        assertEquals("BLACKLIST", data);
        assertTrue(exists);
        assertTrue(report.isBanned());
        assertTrue(expiration > 0);

    }




}