package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.config.OAuthSecurityProperties;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.service.RedisService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static com.example.basketballmatching.global.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthStateStore 단위 테스트")
class OAuthStateStoreUnitTest {

    private static final String STATE = "oauth-state";

    private static final String STATE_KEY = "oauth:state:" + STATE;

    private static final long EXPIRATION_MILLIS = Duration.ofMinutes(3).toMillis();

    @Mock
    private RedisService redisService;

    private OAuthStateStore oAuthStateStore;

    @BeforeEach
    void setUp() {
        OAuthSecurityProperties properties = new OAuthSecurityProperties(
                Duration.ofMinutes(3),
                false
        );

        oAuthStateStore = new OAuthStateStore(redisService, properties);
    }

    @Nested
    @DisplayName("State 발급")
    class Issue {

        @Test
        @DisplayName("State를 생성하고 Redsi에 저장")
        void issue_success() {
            // given

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);


            // when

            String state = oAuthStateStore.issue();

            // then

            verify(redisService).setDataExpireMillis(
                    keyCaptor.capture(),
                    eq("valid"),
                    eq(EXPIRATION_MILLIS)
            );

            assertAll(
                    () -> assertNotNull(state),
                    () -> assertEquals(43, state.length()),
                    () -> assertTrue(state.matches("[A-Za-z0-9_-]{43}")),
                    () -> assertEquals("oauth:state:" + state, keyCaptor.getValue())
            );

        }

    }

    @Nested
    @DisplayName("State 소비")
    class Consume {

        @Test
        @DisplayName("Callback과 쿠키의 State가 일치 시 소비")
        void consume_success() {
            // given

            when(redisService.getAndDeleteData(STATE_KEY))
                    .thenReturn("valid");

            // when

            assertDoesNotThrow(() -> oAuthStateStore.consume(STATE, STATE));

            // then

            verify(redisService).getAndDeleteData(STATE_KEY);
        }

        @Test
        @DisplayName("Callback과 쿠키의 State가 다르면 예외 발생")
        void consume_fail_stateMismatch() {
            // given

            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthStateStore.consume(STATE, "different-state"));

            // then

            assertEquals(OAUTH_STATE_INVALID, exception.getErrorCode());

            verifyNoInteractions(redisService);

        }

        @Test
        @DisplayName("쿠키 State 존재하지 않을 시 예외 발생")
        void consume_fail_cookieNotExists() {
            // given


            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthStateStore.consume(STATE, null));
            // then

            assertEquals(OAUTH_STATE_INVALID, exception.getErrorCode());

            verifyNoInteractions(redisService);
        }

        @Test
        @DisplayName("만료되거나 이미 사용된 쿠키 State 접근 시 예외발생")
        void consume_fail_expiredOrReused() {
            // given

            when(redisService.getAndDeleteData(STATE_KEY))
                    .thenReturn(null);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthStateStore.consume(STATE, STATE));

            // then

            assertEquals(OAUTH_STATE_INVALID, exception.getErrorCode());

        }
    }

}
