package com.example.basketballmatching.blacklist.service;

import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import com.example.basketballmatching.blacklist.dto.response.BlackListResponse;
import com.example.basketballmatching.blacklist.dto.response.CreateBlackListResponse;
import com.example.basketballmatching.blacklist.event.UserBlacklistedEvent;
import com.example.basketballmatching.blacklist.repository.BlackListRepository;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.dto.GameCancelNotificationDto;
import com.example.basketballmatching.game.service.BlackListGameService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.example.basketballmatching.blacklist.type.BlackListStatus.*;
import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static com.example.basketballmatching.report.type.ReportType.*;
import static com.example.basketballmatching.user.type.UserType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlackListService 단위 테스트")
class BlackListServiceUnitTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long REPORTER_ID = 2L;
    private static final Long TARGET_ID = 3L;

    private static final Long GAME_ID = 10L;
    private static final Long REPORT_ID = 20L;
    private static final Long BLACKLIST_ID = 30L;
    private static final Long RECEIVER_ID = 4L;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 27, 12, 0);

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BlackListRepository blackListRepository;

    @Mock
    private BlackListGameService blackListGameService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BlackListService blackListService;

    private UserEntity admin;
    private UserEntity reporter;
    private UserEntity target;
    private GameEntity game;


    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(NOW.atZone(ZONE_ID).toInstant(), ZONE_ID);

        blackListService = new BlackListService(
                userRepository, reportRepository, blackListRepository, blackListGameService, eventPublisher, clock
        );

        admin = createUser(ADMIN_ID, "admin@test.com", "관리자", ADMIN);

        reporter = createUser(REPORTER_ID, "reporter@test.com", "신고자", USER);

        target = createUser(TARGET_ID, "target@test.com", "신고 대상", USER);

        game = createGame();
    }

    @Nested
    @DisplayName("블랙리스트 제재 등록")
    class CreateBlackList {

        @Test
        @DisplayName("승인된 신고를 근거로 7일 제재를 등록하고 이벤트를 발행한다.")
        void createBlackList_success() {
            // given

            ReportEntity approvedReport = createApprovedReport();

            GameCancelNotificationDto notice = new GameCancelNotificationDto(RECEIVER_ID, game.getTitle());

            BlackListGameResultDto gameResult = new BlackListGameResultDto(List.of(notice));


            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(reportRepository.findById(REPORT_ID))
                    .thenReturn(Optional.of(approvedReport));

            when(blackListRepository.existsByReportEntity_ReportId(REPORT_ID))
                    .thenReturn(false);

            when(blackListRepository.existsByUserEntity_UserIdAndExpiresAtAfter(
                    TARGET_ID, NOW
            )).thenReturn(false);

            when(blackListRepository.save(any(BlackListEntity.class)))
                    .thenAnswer(invocation -> {
                        BlackListEntity blackList = invocation.getArgument(0);

                        ReflectionTestUtils.setField(blackList,"blackListId", BLACKLIST_ID);
                        return blackList;
                    });

            when(blackListGameService.cleanup(TARGET_ID, NOW))
                    .thenReturn(gameResult);

            // when

            CreateBlackListResponse response = blackListService.createBlackList(ADMIN_ID, REPORT_ID);

            // then

            assertEquals(BLACKLIST_ID, response.blackListId());

            assertEquals(REPORT_ID, response.reportId());

            assertEquals(TARGET_ID, response.targetUserId());

            assertEquals(NOW.plusDays(7), response.expiresAt());

            assertEquals(ACTIVE, response.status());

            ArgumentCaptor<BlackListEntity> blackListCaptor = ArgumentCaptor.forClass(BlackListEntity.class);

            verify(blackListRepository).save(blackListCaptor.capture());

            BlackListEntity savedBlackList = blackListCaptor.getValue();

            assertEquals(approvedReport, savedBlackList.getReportEntity());

            assertEquals(target, savedBlackList.getUserEntity());

            assertEquals(admin, savedBlackList.getBannedBy());

            ArgumentCaptor<UserBlacklistedEvent> eventCaptor = ArgumentCaptor.forClass(UserBlacklistedEvent.class);

            verify(eventPublisher).publishEvent(eventCaptor.capture());

            UserBlacklistedEvent event = eventCaptor.getValue();

            assertEquals(TARGET_ID, event.userId());

            assertEquals(target.getEmail(), event.email());

            assertEquals(NOW, event.bannedAt());

            assertEquals(NOW.plusDays(7), event.expiresAt());

            assertEquals(1, event.notices().size());

            assertEquals(RECEIVER_ID, event.notices().get(0).receiverId());

        }
        @Test
        @DisplayName("승인되지 않은 신고는 제재에 사용할 수 없다.")
        void createBlackList_fail_reportNotApproved() {
            // given

            ReportEntity pendingReport = createPendingReport();

            when(userRepository.findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(reportRepository.findById(REPORT_ID))
                    .thenReturn(Optional.of(pendingReport));

            // when

            CustomException exception = assertThrows(CustomException.class, () -> blackListService.createBlackList(ADMIN_ID, REPORT_ID));

            // then

            assertEquals(REPORT_NOT_APPROVED, exception.getErrorCode());

            verifyNoInteractions(blackListGameService, eventPublisher);

        }

        @Test
        @DisplayName("이미 제재에 사용한 신고를 재사용할 수 없다.")
        void createBlackList_fail_reportAlreadyUsed() {
            // given

            ReportEntity approvedReport = createApprovedReport();

            when(userRepository
                    .findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(reportRepository.findById(REPORT_ID))
                    .thenReturn(Optional.of(approvedReport));

            when(blackListRepository
                    .existsByReportEntity_ReportId(REPORT_ID))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> blackListService.createBlackList(ADMIN_ID, REPORT_ID));

            // then

            assertEquals(BLACKLIST_REPORT_ALREADY_USED, exception.getErrorCode());

            verify(blackListRepository, never()).save(any());

            verifyNoInteractions(blackListGameService, eventPublisher);

        }

        @Test
        @DisplayName("이미 활성 제재 중인 사용자를 다시 제재할 수 없다.")
        void createBlackList_fail_alreadyBlacklisted() {
            // given

            ReportEntity approvedReport = createApprovedReport();

            when(userRepository
                    .findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(reportRepository.findById(REPORT_ID))
                    .thenReturn(Optional.of(approvedReport));

            when(blackListRepository
                    .existsByReportEntity_ReportId(REPORT_ID))
                    .thenReturn(false);

            when(blackListRepository
                    .existsByUserEntity_UserIdAndExpiresAtAfter(
                            TARGET_ID,
                            NOW
                    ))
                    .thenReturn(true);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> blackListService.createBlackList(ADMIN_ID, REPORT_ID));

            // then

            assertEquals(ALREADY_BLACK_USER, exception.getErrorCode());

            verify(blackListRepository, never()).save(any());

            verifyNoInteractions(blackListGameService, eventPublisher);

        }

    }

    @Nested
    @DisplayName(("블랙리스트 목록 조회"))
    class GetBlackLists{

        @Test
        @DisplayName("활성 제재 목록을 조회한다")
        void getBlackLists_success_active() {
            // given
            PageRequest pageable =
                    PageRequest.of(0, 20);

            BlackListEntity activeBlackList =
                    createBlackListEntity(
                            NOW.minusDays(1),
                            NOW.plusDays(6)
                    );

            Page<BlackListEntity> page =
                    new PageImpl<>(
                            List.of(activeBlackList),
                            pageable,
                            1
                    );

            when(userRepository
                    .findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(blackListRepository
                    .findAllByExpiresAtAfterOrderByBannedDateTimeDesc(
                            NOW,
                            pageable
                    ))
                    .thenReturn(page);

            // when
            Page<BlackListResponse> response =
                    blackListService.getBlackLists(
                            ADMIN_ID,
                            ACTIVE,
                            pageable
                    );

            // then
            assertEquals(
                    1,
                    response.getTotalElements()
            );

            BlackListResponse content =
                    response.getContent().get(0);

            assertEquals(
                    BLACKLIST_ID,
                    content.blackListId()
            );

            assertEquals(
                    TARGET_ID,
                    content.targetUserId()
            );

            assertEquals(
                    ACTIVE,
                    content.status()
            );

            verify(blackListRepository, never())
                    .findAllByExpiresAtLessThanEqualOrderByBannedDateTimeDesc(
                            any(),
                            any()
                    );
        }

        @Test
        @DisplayName("만료된 제재 목록을 조회한다")
        void getBlackLists_success_expired() {
            // given
            PageRequest pageable =
                    PageRequest.of(0, 20);

            BlackListEntity expiredBlackList =
                    createBlackListEntity(
                            NOW.minusDays(10),
                            NOW.minusDays(3)
                    );

            Page<BlackListEntity> page =
                    new PageImpl<>(
                            List.of(expiredBlackList),
                            pageable,
                            1
                    );

            when(userRepository
                    .findByUserIdAndDeletedDateTimeIsNull(ADMIN_ID))
                    .thenReturn(Optional.of(admin));

            when(blackListRepository
                    .findAllByExpiresAtLessThanEqualOrderByBannedDateTimeDesc(
                            NOW,
                            pageable
                    ))
                    .thenReturn(page);

            // when
            Page<BlackListResponse> response =
                    blackListService.getBlackLists(
                            ADMIN_ID,
                            EXPIRED,
                            pageable
                    );

            // then
            assertEquals(
                    1,
                    response.getTotalElements()
            );

            BlackListResponse content =
                    response.getContent().get(0);

            assertEquals(
                    BLACKLIST_ID,
                    content.blackListId()
            );

            assertEquals(
                    EXPIRED,
                    content.status()
            );

            verify(blackListRepository, never())
                    .findAllByExpiresAtAfterOrderByBannedDateTimeDesc(
                            any(),
                            any()
                    );
        }

    }



    private BlackListEntity createBlackListEntity(
            LocalDateTime bannedAt,
            LocalDateTime expiresAt
    ) {
        ReportEntity approvedReport =
                createApprovedReport();

        BlackListEntity blackList =
                BlackListEntity.create(
                        approvedReport,
                        admin,
                        bannedAt,
                        expiresAt
                );

        ReflectionTestUtils.setField(
                blackList,
                "blackListId",
                BLACKLIST_ID
        );

        return blackList;
    }

    private ReportEntity createApprovedReport() {

        ReportEntity report = createPendingReport();


        report.approve(admin, "신고 내용을 확인하여 승인합니다.", NOW.minusHours(1));

        return report;

    }

    private ReportEntity createPendingReport() {
        ReportEntity report = ReportEntity.create(
                reporter, target, game, POOR_SPORTSMANSHIP, "경기 중 반복적인 비매너 행위", NOW.minusDays(1)
        );

        ReflectionTestUtils.setField(report, "reportId", REPORT_ID);

        return report;
    }

    private GameEntity createGame() {
        LocalDateTime startDateTime =
                NOW.plusDays(2);

        GameEntity createdGame =
                GameEntity.create(
                        "블랙리스트 테스트 경기",
                        "블랙리스트 정책 검증용 경기입니다.",
                        6,
                        INDOOR,
                        THREE_ON_THREE,
                        MIXED,
                        startDateTime,
                        startDateTime.plusHours(2),
                        "테스트 농구장",
                        "서울특별시 송파구",
                        SEOUL,
                        37.5,
                        127.0,
                        reporter,
                        NOW
                );

        ReflectionTestUtils.setField(createdGame, "gameId", GAME_ID);


        return createdGame;
    }

    private UserEntity createUser(Long userId, String email, String nickname, UserType userType) {


        return UserEntity.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name(nickname)
                .birth(LocalDate.of(1997, 1, 1))
                .phone("010-1234-5678")
                .address("서울특별시")
                .position(Position.GUARD)
                .userType(userType)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }

}