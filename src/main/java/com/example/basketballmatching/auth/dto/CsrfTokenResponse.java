package com.example.basketballmatching.auth.dto;

public record CsrfTokenResponse(
        String headerName,
        String token

) {
}
