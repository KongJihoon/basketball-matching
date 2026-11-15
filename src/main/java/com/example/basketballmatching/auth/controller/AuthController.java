package com.example.basketballmatching.auth.controller;


import com.example.basketballmatching.auth.dto.*;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.auth.support.BrowserAuthResponses;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "AUTH")
public class AuthController {




    private final AuthService authService;

    private final BrowserAuthResponses browserAuthResponses;


    @Operation(summary = "브라우저용 CSRF 토큰 조회")
    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    /**
        유저 로그인
     */
    @Operation(summary = "유저 로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<BrowserAuthResponse>> loginUser(
            @RequestBody @Valid LoginRequest request
            ) {


        AuthTokenResponse response = authService.login(request.email(), request.password());

        return browserAuthResponses.session(
                HttpStatus.OK, "로그인에 성공하였습니다", response
        );

    }

    /**
     * 토큰 재발급
     */
    @Operation(summary = "토큰 재발급")
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"
    )
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/token/refresh")
    public ResponseEntity<CommonResponse<BrowserAuthResponse>> reissue(
            @CookieValue(name = BrowserAuthResponses.REFRESH_COOKIE_NAME, required = false) String refreshToken
            ) {

        AuthTokenResponse response = authService.reissue(refreshToken);



        return browserAuthResponses.session(
                HttpStatus.OK, "토큰 재발급에 성공하였습니다.", response
        );
    }

    /**
     * 유저 로그아웃
     */
    @Operation(summary = "유저 로그아웃")
    @ApiResponse(responseCode = "200", description = "유저 로그아웃 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> logoutUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        String accessToken = authorization.substring("Bearer ".length()).trim();

        authService.logoutUser(userInfoDetails.getUsername(), accessToken);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, browserAuthResponses.expiredRefreshCookie())
                .body(CheckResponse.of(true, "로그아웃을 완료했습니다."));
    }








}
