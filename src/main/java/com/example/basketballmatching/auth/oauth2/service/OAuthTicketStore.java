package com.example.basketballmatching.auth.oauth2.service;

import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketPayload;
import com.example.basketballmatching.auth.oauth2.type.OAuthFlowType;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.example.basketballmatching.global.exception.ErrorCode.INTERNAL_SERVER_ERROR;
import static com.example.basketballmatching.global.exception.ErrorCode.OAUTH_TICKET_INVALID;

@Component
@RequiredArgsConstructor
public class OAuthTicketStore {

    private static final String TICKET_PREFIX =
            "oauth:ticket:";

    private static final long
            TICKET_EXPIRE_MINUTES = 10L;

    private final RedisService redisService;

    private final ObjectMapper objectMapper;

    /**
     * OAuth Payload를 Redis에 저장하고
     * 클라이언트에 전달할 일회용 Ticket을 발급한다.
     */
    public String issue(OAuthTicketPayload payload) {

        // UUID 랜덤 문자열 생성
        String ticket = createTicket();

        /*
         * 여러 필드로 구성된 Payload를 Redis에 저장할 수 있게 JSON 문자열로 직렬화 한다.
         */
        String serializedPayload = serialize(payload);

        /*
         * ticket을 Redis 키로 사용하고, 직렬화한 문자열을 value로 저장하고
         * 10분이 지나면 사용하지 않는 Ticket은 자동 만료된다.
         */
        redisService.setDataExpireMinutes(ticketKey(ticket), serializedPayload, TICKET_EXPIRE_MINUTES);


        return ticket;
    }

    /**
     *  Ticket을 한 번만 사용하고 저장된 OAuth Payload를 반환한다.
     *
     *  Redis GETDEL 방식으로 조회한뒤 데이터를 원자적으로 제거 -> 티켓 재사용 방지
     */
    public OAuthTicketPayload consume(String ticket, OAuthFlowType expectedFlowType) {

        if (ticket == null || ticket.isBlank()) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

        /*
         * 티켓을 Redis를 조회하는 동시에 제거한다.
         * 성공 여부와 관계없이 한 번 조회된 티켓의 재사용 방지.
         */
        String serializedPayload = redisService.getAndDeleteData(ticketKey(ticket));

        if (serializedPayload == null || serializedPayload.isBlank()) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

        // Redis의 저장된 JSON 문자열을 다시 OAuth Payload로 역직렬화한다.
        OAuthTicketPayload payload = deserialize(serializedPayload);

        /*
         * LOGIN API에서는 Login Ticket
         * SIGNUP API에서는 Signup Ticket
         * 위 과정을 위해 저장된 flowType과 비교 검증
         */
        if (payload.flowType() != expectedFlowType) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }


        return payload;
    }

    /**
     * Redis에 저장된 JSON 문자열을 OAuth Payload로 역직렬화
     */
    private OAuthTicketPayload deserialize(String serializedPayload) {

        try {

            return objectMapper.readValue(serializedPayload, OAuthTicketPayload.class);

        } catch (JsonProcessingException exception) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

    }

    /**
     * OAuth Payload를 Redis value값으로 저장하기 위해
     * JSON 문자열로 직렬화한다.
     */
    private String serialize(OAuthTicketPayload payload) {

        try {

            return objectMapper
                    .writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new CustomException(INTERNAL_SERVER_ERROR);
        }

    }

    private String ticketKey(String ticket) {
        return TICKET_PREFIX + ticket;
    }



    private String createTicket() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
