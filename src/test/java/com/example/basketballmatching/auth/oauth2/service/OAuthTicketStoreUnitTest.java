package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketPayload;
import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.global.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.basketballmatching.auth.oauth2.type.OAuthProvider.KAKAO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthTicketStore 단위 테스트")
class OAuthTicketStoreUnitTest {

    private static final String EMAIL = "test@example.com";

    private static final String TICKET = "test-oauth-ticket";

    private static final String TICKET_KEY = "oauth:ticket:" + TICKET;

    private static final String PROVIDER_USER_ID = "123456789";

    private static final long TICKET_EXPIRE_MINUTES = 10L;

    @Mock
    private RedisService redisService;

    private ObjectMapper objectMapper;

    private OAuthTicketStore oAuthTicketStore;


    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();

        oAuthTicketStore = new OAuthTicketStore(
                redisService, objectMapper
        );
    }


    @Nested
    @DisplayName("TIcket 발급")
    class Issue {

        @Test
        @DisplayName("SIGNUP Payload를 Redis에 저장하고 Ticket 발급")
        void issue_success() throws Exception{
            // given

            OAuthTicketPayload payload = OAuthTicketPayload.signup(KAKAO, PROVIDER_USER_ID, EMAIL);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);


            // when

            String ticket = oAuthTicketStore.issue(payload);

            // then

            verify(redisService).setDataExpireMinutes(
                    keyCaptor.capture(),
                    valueCaptor.capture(),
                    eq(TICKET_EXPIRE_MINUTES)
            );

            OAuthTicketPayload storedPayload = objectMapper.readValue(valueCaptor.getValue(), OAuthTicketPayload.class);


            assertAll(
                    () -> assertNotNull(ticket),
                    () -> assertEquals(32, ticket.length()),
                    () -> assertEquals("oauth:ticket:" + ticket, keyCaptor.getValue()),
                    () -> assertEquals(payload, storedPayload)
            );
        }

    }

    @Nested
    @DisplayName("Ticket 소비")
    class Consume {

        @Test
        @DisplayName("유효한 SIGNUP TICKET을 한 번 소비하고 Payload 반환")
        void consume_success() throws Exception {
            // given

            OAuthTicketPayload payload = OAuthTicketPayload.signup(
                    KAKAO,
                    PROVIDER_USER_ID,
                    EMAIL
            );

            String serializedPayload = objectMapper.writeValueAsString(payload);

            when(redisService.getAndDeleteData(TICKET_KEY))
                    .thenReturn(serializedPayload);

            // when

            OAuthTicketPayload result = oAuthTicketStore.consume(TICKET, OAuthFlowType.SIGNUP);

            // then

            assertEquals(payload, result);

            verify(redisService).getAndDeleteData(TICKET_KEY);

        }

        @Test
        @DisplayName("만료되거나 이미 사용한 Ticket이면 예외 발생")
        void consume_fail_ticketNotFound() {
            // given

            when(redisService.getAndDeleteData(TICKET_KEY))
                    .thenReturn(null);
            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthTicketStore.consume(TICKET, OAuthFlowType.SIGNUP));

            // then

            assertEquals(ErrorCode.OAUTH_TICKET_INVALID, exception.getErrorCode());

        }

        @Test
        @DisplayName("Ticket의 요청 분기가 다르면 예외 발생")
        void consume_fail_flowTypeMismatch() throws Exception {
            // given

            OAuthTicketPayload payload = OAuthTicketPayload.signup(
                    KAKAO, PROVIDER_USER_ID, EMAIL
            );

            String serializedPayload = objectMapper.writeValueAsString(payload);

            when(redisService.getAndDeleteData(TICKET_KEY))
                    .thenReturn(serializedPayload);

            // when

            CustomException exception = assertThrows(CustomException.class, () -> oAuthTicketStore.consume(TICKET, OAuthFlowType.LOGIN));

            // then

            assertEquals(ErrorCode.OAUTH_TICKET_INVALID, exception.getErrorCode());

        }
    }

}