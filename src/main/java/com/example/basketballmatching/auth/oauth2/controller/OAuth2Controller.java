package com.example.basketballmatching.auth.oauth2.controller;


import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.auth.oauth2.dto.OAuthAuthorizationResult;
import com.example.basketballmatching.auth.oauth2.dto.OAuthCallbackResponse;
import com.example.basketballmatching.auth.oauth2.dto.OAuthSignUpRequest;
import com.example.basketballmatching.auth.oauth2.dto.OAuthTicketRequest;
import com.example.basketballmatching.auth.oauth2.service.OAuthService;
import com.example.basketballmatching.auth.oauth2.support.OAuthStateCookieManager;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/oauth2")
@Tag(name = "OAUTH2")
public class OAuth2Controller {

    private final OAuthService oAuthService;

    private final OAuthStateCookieManager oAuthStateCookieManager;

    @Operation(summary = "카카오 로그인 요청 (리다이렉트)")
    @GetMapping("/kakao/authorization")
    public void authorizationKakao(HttpServletResponse response) throws IOException {

        OAuthAuthorizationResult result = oAuthService.createKakaoAuthorization();

        oAuthStateCookieManager.add(
                response, result.state()
        );

        response.sendRedirect(result.authorizationUrl());
    }

    @Operation(summary = "카카오 로그인 Callback")
    @ApiResponse(responseCode = "200", description = "카카오 인증 및 OAuth Ticket 발급 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "카카오 API 연동 실패",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/kakao/callback")
    public ResponseEntity<CommonResponse<OAuthCallbackResponse>> kakaoCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String cookieState = oAuthStateCookieManager.read(request);


        try {
            OAuthCallbackResponse callbackResponse = oAuthService.kakaoCallback(code, state, cookieState);

            return ResponseEntity.ok(
                    CommonResponse.of("카카오 인증에 성공하였습니다.", callbackResponse)
            );

        } finally {
            oAuthStateCookieManager.delete(response);
        }

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

    @Operation(summary = "OAuth 추가정보 회원가입")
    @ApiResponse(responseCode = "200", description = "OAuth 회원가입 및 서비스 토큰 발급 성공")
    @ApiResponse(responseCode = "400", description = "회원가입 입력값 오류",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 OAuth Ticket",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "이메일, 닉네임 또는 OAuth 계정 중복",
    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<AuthTokenResponse>> signup(
            @Valid @RequestBody OAuthSignUpRequest request
            ) {

        AuthTokenResponse response = oAuthService.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        CommonResponse.of("OAuth 회원가입이 완료되었습니다.", response)
                );
    }
}
