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

    public String issue(OAuthTicketPayload payload) {

        String ticket = createTicket();

        String serializedPayload = serialize(payload);

        redisService.setDataExpireMinutes(ticketKey(ticket), serializedPayload, TICKET_EXPIRE_MINUTES);


        return ticket;
    }

    public OAuthTicketPayload consume(String ticket, OAuthFlowType expectedFlowType) {

        if (ticket == null || ticket.isBlank()) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

        String serializedPayload = redisService.getAndDeleteData(ticketKey(ticket));

        if (serializedPayload == null || serializedPayload.isBlank()) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

        OAuthTicketPayload payload = deserialize(serializedPayload);

        if (payload.flowType() != expectedFlowType) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }


        return payload;
    }

    private OAuthTicketPayload deserialize(String serializedPayload) {

        try {

            return objectMapper.readValue(serializedPayload, OAuthTicketPayload.class);

        } catch (JsonProcessingException exception) {
            throw new CustomException(OAUTH_TICKET_INVALID);
        }

    }

    private String ticketKey(String ticket) {
        return TICKET_PREFIX + ticket;
    }

    private String serialize(OAuthTicketPayload payload) {

        try {

            return objectMapper
                    .writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new CustomException(INTERNAL_SERVER_ERROR);
        }

    }

    private String createTicket() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
