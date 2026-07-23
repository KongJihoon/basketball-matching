package com.example.basketballmatching.user.service;

import com.example.basketballmatching.gameCreator.dto.GameCancelNotificationDto;
import com.example.basketballmatching.gameCreator.dto.UserWithdrawalGameResultDto;
import com.example.basketballmatching.gameCreator.service.UserWithdrawalGameService;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.event.UserWithdrawnEvent;
import com.example.basketballmatching.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserWithdrawalService 단위 테스트")
class UserWithdrawalServiceTest {

    private static final Long USER_ID = 1L;

    private static final String EMAIL = "text@example.com";

    private static final String ACCESS_TOKEN =
            "access-token";

    private static final ZoneId TEST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-10T03:00:00Z"),
            TEST_ZONE_ID);

    private static final LocalDateTime WITHDRAWN_AT = LocalDateTime.of(2026, 1, 10, 12, 0);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserWithdrawalGameService userWithdrawalGameService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private  UserWithdrawalService userWithdrawalService;

    @BeforeEach
    void setUp() {
        userWithdrawalService =
                new UserWithdrawalService(
                        userRepository,
                        userWithdrawalGameService,
                        eventPublisher,
                        FIXED_CLOCK
                );
    }

    @Test
    @DisplayName("회원 탈퇴 후 이벤트 발행")
    void withdraw_success() {
        // given

        UserEntity user = createUser();

        List<GameCancelNotificationDto> notices = List.of(
                new GameCancelNotificationDto(
                        2L, "토요일 농구 경기"
                ),
                new GameCancelNotificationDto(
                        3L, "일요일 농구 경기"
                )
        );

        UserWithdrawalGameResultDto gameResult = new UserWithdrawalGameResultDto(notices);

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                .thenReturn(Optional.of(user));

        when(userWithdrawalGameService.cleanup(USER_ID, WITHDRAWN_AT))
                .thenReturn(gameResult);

        ArgumentCaptor<UserWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);


        // when

        userWithdrawalService.withdraw(USER_ID, ACCESS_TOKEN);

        // then

        assertEquals(WITHDRAWN_AT, user.getDeletedDateTime());

        verify(userRepository).findByUserIdAndDeletedDateTimeIsNull(USER_ID);

        verify(userWithdrawalGameService).cleanup(USER_ID, WITHDRAWN_AT);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserWithdrawnEvent publishEvent = eventCaptor.getValue();

        assertEquals(USER_ID, publishEvent.userId());

        assertEquals(EMAIL, publishEvent.email());

        assertEquals(ACCESS_TOKEN, publishEvent.accessToken());

        assertEquals(notices, publishEvent.notices());

        /*
         * JPA Dirty Checking을 사용하므로
         * UserRepository.save()를 호출하지 않는다.
         */
        verify(userRepository, never()).save(any(UserEntity.class));

    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    @DisplayName("ACCESS TOKEN이 존재하지 않거나 공백이면 예외 발생")
    void withdraw_fail_accessTokenIsMissing(String accessToken) {
        // given

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userWithdrawalService.withdraw(USER_ID, accessToken));

        // then

        assertEquals(NOT_FOUND_TOKEN, exception.getErrorCode());

        verifyNoInteractions(
                userRepository, userWithdrawalGameService, eventPublisher
        );
    }

    @Test
    @DisplayName("존재하지 않는 회원 예외 발생")
    void withdraw_fail_userNotFound() {
        // given

        when(userRepository.findByUserIdAndDeletedDateTimeIsNull(USER_ID))
                .thenReturn(Optional.empty());

        // when

        CustomException exception = assertThrows(CustomException.class, () -> userWithdrawalService.withdraw(USER_ID, ACCESS_TOKEN));

        // then

        assertEquals(USER_NOT_FOUND, exception.getErrorCode());

        verifyNoInteractions(userWithdrawalGameService, eventPublisher);

    }


    private UserEntity createUser() {

        return UserEntity.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .build();
    }


}