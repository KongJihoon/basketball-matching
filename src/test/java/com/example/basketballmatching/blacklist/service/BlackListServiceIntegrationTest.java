package com.example.basketballmatching.blacklist.service;

import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import com.example.basketballmatching.blacklist.dto.response.BlackListResponse;
import com.example.basketballmatching.blacklist.dto.response.CreateBlackListResponse;
import com.example.basketballmatching.blacklist.repository.BlackListRepository;
import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.game.dto.BlackListGameResultDto;
import com.example.basketballmatching.game.repository.GameRepository;
import com.example.basketballmatching.game.service.BlackListGameService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.report.domain.ReportEntity;
import com.example.basketballmatching.report.repository.ReportRepository;
import com.example.basketballmatching.report.type.ReportType;
import com.example.basketballmatching.support.IntegrationTest;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.repository.UserRepository;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.example.basketballmatching.blacklist.type.BlackListStatus.ACTIVE;
import static com.example.basketballmatching.blacklist.type.BlackListStatus.EXPIRED;
import static com.example.basketballmatching.game.type.CityName.SEOUL;
import static com.example.basketballmatching.game.type.FieldStatus.INDOOR;
import static com.example.basketballmatching.game.type.MatchFormat.THREE_ON_THREE;
import static com.example.basketballmatching.game.type.MatchGenderType.MIXED;
import static com.example.basketballmatching.global.exception.ErrorCode.BLACKLIST_REPORT_ALREADY_USED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@IntegrationTest
@Transactional
@DisplayName("BlackListService 통합 테스트")
class BlackListServiceIntegrationTest {

    @Autowired
    private BlackListService blackListService;

    @Autowired
    private BlackListStore blackListStore;

    @Autowired
    private BlackListRepository blackListRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    /**
     * 경기 정리 로직은 BlackListGameService의 책임이다.
     * 이 테스트에서는 블랙리스트 등록과 경기 정리 호출의 연결만 검증한다.
     */

    @MockBean
    private BlackListGameService blackListGameService;

    private Long adminId;
    private Long targetId;
    private Long reportId;

    private String targetEmail;

    @BeforeEach
    void setUp() {

        UserEntity creator = saveUser(
                "creator@test.com",
                "경기생성자",
                "010-1111-1111",
                UserType.USER
        );

        UserEntity reporter = saveUser(
                "reporter@test.com",
                "신고자",
                "010-2222-2222",
                UserType.USER
        );

        UserEntity target = saveUser(
                "target@test.com",
                "신고대상",
                "010-3333-3333",
                UserType.USER
        );

        UserEntity admin = saveUser(
                "admin@test.com",
                "관리자",
                "010-4444-4444",
                UserType.ADMIN
        );

        LocalDateTime now = LocalDateTime.now()
                .withSecond(0)
                .withNano(0);

        LocalDateTime startDateTime = now.minusDays(2);
        LocalDateTime endDateTime = startDateTime.plusHours(2);
        LocalDateTime createdAt = startDateTime.minusDays(1);

        GameEntity game = GameEntity.create(
                "블랙리스트 통합 테스트 경기",
                "종료된 경기입니다.",
                6,
                INDOOR,
                THREE_ON_THREE,
                MIXED,
                startDateTime,
                endDateTime,
                "잠실 농구장",
                "서울특별시 송파구 올림픽로 25",
                SEOUL,
                37.515,
                127.073,
                creator,
                createdAt
        );

        gameRepository.save(game);

        ReportEntity report = ReportEntity.create(
                reporter, target, game, ReportType.POOR_SPORTSMANSHIP, "경기 중 비매너 행위를 반복했습니다.", now.minusHours(2));

        report.approve(admin, "신고 내용을 확인하여 승인합니다.", now.minusHours(1));

        reportRepository.save(report);

        adminId = admin.getUserId();
        targetId = target.getUserId();
        targetEmail = target.getEmail();
        reportId = report.getReportId();

        flushAndClear();


    }

    @Test
    @DisplayName("승인된 신고를 근거로 7일간 블랙리스트 제재 저장")
    void createBlackList_success() {
        // given

        when(blackListGameService.cleanup(eq(targetId), any(LocalDateTime.class)))
                .thenReturn(new BlackListGameResultDto(List.of()));

        // when

        CreateBlackListResponse response = blackListService.createBlackList(adminId, reportId);

        flushAndClear();
        // then

        BlackListEntity blackList = blackListRepository.findById(response.blackListId()).orElseThrow();


        assertEquals(reportId, blackList.getReportEntity().getReportId());
        assertEquals(targetId, blackList.getUserEntity().getUserId());
        assertEquals(adminId, blackList.getBannedBy().getUserId());

        assertEquals(response.bannedAt().plusDays(7), response.expiresAt());

        assertEquals(
                response.expiresAt().truncatedTo(ChronoUnit.SECONDS),
                blackList.getExpiresAt().truncatedTo(ChronoUnit.SECONDS)
        );

        assertEquals(ACTIVE, response.status());

        assertTrue(blackListStore.isBlacklisted(targetEmail));

        verify(blackListGameService).cleanup(eq(targetId), any(LocalDateTime.class));
    }


    @Test
    @DisplayName("이미 제재에 성공한 신고를 다시 사용할 수 없다")
    void createBlackList_fail_reportAlreadyUsed() {
        // given

        when(blackListGameService.cleanup(eq(targetId), any(LocalDateTime.class)))
                .thenReturn(new BlackListGameResultDto(List.of()));

        blackListService.createBlackList(adminId, reportId);

        flushAndClear();
        // when

        CustomException exception = assertThrows(CustomException.class, () -> blackListService.createBlackList(adminId, reportId));

        // then

        assertEquals(BLACKLIST_REPORT_ALREADY_USED, exception.getErrorCode());

        assertEquals(1, blackListRepository.count());

    }

    @Test
    @DisplayName("관리자는 활성 블")
    void getBlackLists_success() {
        // given

        when(blackListGameService.cleanup(eq(targetId), any(LocalDateTime.class)))
                .thenReturn(new BlackListGameResultDto(List.of()));

        CreateBlackListResponse created = blackListService.createBlackList(adminId, reportId);

        flushAndClear();
        // when

        Page<BlackListResponse> response = blackListService.getBlackLists(adminId, ACTIVE, PageRequest.of(0, 20));

        // then

        assertEquals(1, response.getTotalElements());


        BlackListResponse blackList = response.getContent().get(0);

        assertEquals(created.blackListId(), blackList.blackListId());
        assertEquals(reportId, blackList.reportId());
        assertEquals(targetId, blackList.targetUserId());
        assertEquals(targetEmail, blackList.email());
        assertEquals(ACTIVE, blackList.status());
    }

    @Test
    @DisplayName("제재 기간이 지나면 만료 목록으로 조회되고 제재가 자동 해제된다.")
    void getBlackLists_success_expired() {
        // given
        LocalDateTime now = LocalDateTime.now(clock);

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow();

        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow();

        BlackListEntity expiredBlackList = BlackListEntity.create(report, admin, now.minusDays(8), now.minusDays(1));

        blackListRepository.save(expiredBlackList);
        flushAndClear();

        // when

        Page<BlackListResponse> response = blackListService.getBlackLists(adminId, EXPIRED, PageRequest.of(0, 20));


        // then

        assertEquals(1, response.getTotalElements());

        BlackListResponse blackList = response.getContent().get(0);

        assertEquals(expiredBlackList.getBlackListId(), blackList.blackListId());
        assertEquals(EXPIRED, blackList.status());

        assertFalse(blackListStore.isBlacklisted(targetEmail));
    }




    private UserEntity saveUser(
            String email,
            String nickname,
            String phone,
            UserType userType
    ) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .name(nickname)
                .birth(LocalDate.of(1997, 1, 1))
                .phone(phone)
                .address("서울특별시 송파구")
                .position(Position.GUARD)
                .userType(userType)
                .genderType(GenderType.MALE)
                .loginProvider(LoginProvider.LOCAL)
                .emailAuth(true)
                .build();

        return userRepository.save(user);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
