package com.example.basketballmatching.auth.service;

import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.security.TokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionRevocationService 단위 테스트")
class UserSessionRevocationServiceUnitTest {
    private static final String EMAIL =
            "test@test.com";

    private static final String ACCESS_TOKEN =
            "access-token";

    private static final long REMAINING_TIME =
            300_000L;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthTokenStore authTokenStore;

    @InjectMocks
    private UserSessionRevocationService userSessionRevocationService;

    @Test
    @DisplayName("사용자 세션 폐기 시 AccessToken을 제거하고 RefreshToken 제거")
    void revokeAll_success() {
        // given

        when(tokenProvider.getRemainingTime(ACCESS_TOKEN))
                .thenReturn(REMAINING_TIME);

        // when
        userSessionRevocationService.revokeAll(EMAIL, ACCESS_TOKEN);

        // then

        InOrder inOrder = inOrder(authTokenStore);

        inOrder.verify(authTokenStore)
                .revokeAccessToken(ACCESS_TOKEN, REMAINING_TIME);

        inOrder.verify(authTokenStore)
                .deleteRefreshToken(EMAIL);

    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("AccessToken이 존재하지 않을 시 예외 발생")
    void revokeAll_fail_accessTokenNotFound(String accessToken) {
        // given

        // when
        CustomException exception = assertThrows(CustomException.class, () -> userSessionRevocationService.revokeAll(EMAIL, accessToken));

        // then

        assertEquals(NOT_FOUND_TOKEN, exception.getErrorCode());

        verifyNoInteractions(tokenProvider, authTokenStore);

    }

}
