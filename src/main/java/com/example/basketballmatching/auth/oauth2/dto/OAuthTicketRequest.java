package com.example.basketballmatching.auth.oauth2.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OAuthTicketRequest(
        @Schema(
                description = "OAuth 로그인 완료 후 발급된 일회용 Ticket",
                example = "jL8Y7YpL7bTNP1FzQxR4dFW8BnPzHhVk"
        )
        @NotBlank(message = "OAuth Ticket은 필수입니다.")
        String ticket
) {
}
