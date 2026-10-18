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

    /**
     * 카카오 OAuth 인증 진입
     *
     * 서버가 발급한 State를 브라우저 Cookie에 저장하고,
     * 동일한 State가 포함된 카카오 인증 URL로 사용자를 리다이렉트한다.
     */
    @Operation(summary = "카카오 로그인 요청 (리다이렉트)")
    @GetMapping("/kakao/authorization")
    public void authorizationKakao(HttpServletResponse response) throws IOException {

        /*
         * 예측하기 어려운 State를 생성해 Redis에 저장하고,
         * 해당 State가 포함된 카카오 인증 URL을 생성한다.
         */
        OAuthAuthorizationResult result = oAuthService.createKakaoAuthorization();

        /*
         * Callback 요청이 OAuth 인증을 시작한 브라우저에서 왔는지
         * 확인할 수 있도록 State를 HttpOnly 쿠키에 저장한다.
         */
        oAuthStateCookieManager.add(
                response, result.state()
        );

        // 사용자를 카카오 로그인 및 동의 화면으로 이동시킨다.
        response.sendRedirect(result.authorizationUrl());
    }


    /**
     * 카카오 인증 완료 후 인가 코드와 State를 전달받는 Callback API
     *
     * Cookie에 저장해 둔 State와 Callback으로 전달된 State를 검증한 뒤
     * 카카오 사용자 정보를 조회하고 OAuth Ticket을 발급한다.
     */
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

        /*
         * OAuth 인증을 시작한 브라우저를 확인하기 위해
         * 인증 요청 단계에서 저장한 State Cookie를 조회한다.
         */
        String cookieState = oAuthStateCookieManager.read(request);


        try {
            /*
             * 인가 코드와 State를 검증하고
             * 카카오 사용자에 해당하는 일회용 OAuth Ticket을 발급한다.
             */
            OAuthCallbackResponse callbackResponse = oAuthService.kakaoCallback(code, state, cookieState);

            return ResponseEntity.ok(
                    CommonResponse.of("카카오 인증에 성공하였습니다.", callbackResponse)
            );

        } finally {

            /*
             * 인증 성공 여부와 관계없이 사용이 끝난 State Cookie를 삭제한다.
             * 실패한 인증 요청의 Cookie가 브라우저에 남는 것을 방지.
             */
            oAuthStateCookieManager.delete(response);
        }

    }

    /**
     * Kakao Callback에서 발급받은 LOGIN Ticket을 사용하여
     * 서비스의 JWT 토큰을 발급받는다.
     */
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

    /**
     * Kakao Callback에서 발급받은 SIGNUP Ticket과
     * 신규 회원의 추가 정보를 입력받아 회원가입을 완료하고 서비스 JWT 토큰을 발급한다.
     */
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
