package com.example.basketballmatching.auth.oauth2.controller;


import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.oauth2.dto.OAuthCallbackResponse;
import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketRequest;
import com.example.basketballmatching.auth.oauth2.service.OAuthService;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/oauth2")
@Tag(name = "OAUTH2")
public class OAuth2Controller {

    private final OAuthService oAuthService;

    @Operation(summary = "카카오 로그인 요청 (리다이렉트)")
    @GetMapping("/kakao/authorization")
    public void authorizationKakao(HttpServletResponse response) throws IOException {

        response.sendRedirect(oAuthService.createKakaoAuthorizationUrl());

    }

    @Operation(summary = "카카오 로그인 Callback")
    @ApiResponse(responseCode = "200", description = "카카오 인증 및 OAuth Ticket 발급 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "카카오 API 연동 실패",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/kakao/callback")
    public ResponseEntity<CommonResponse<OAuthCallbackResponse>> kakaoCallback(
            @RequestParam(name = "code") String code
    ) {


        OAuthCallbackResponse response = oAuthService.kakaoCallback(code);


        return ResponseEntity.ok(CommonResponse.of("카카오 인증에 성공하였습니다.", response));
    }

    @Operation(summary = "OAuth 로그인 ticket 교환")
    @ApiResponse(responseCode = "200", description = "서비스 토큰 발급 성공")
    @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 토큰",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/token")
    public ResponseEntity<CommonResponse<AuthTokenResponse>> exchangeLoginTicket(
            @Valid @RequestBody OAuthTicketRequest request
            ) {


        AuthTokenResponse response = oAuthService.exchangeLoginTicket(request);

        return ResponseEntity.ok(
                CommonResponse.of("OAuth 로그인에 성공하였습니다.", response)
        );
    }
}
