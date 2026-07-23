package com.example.basketballmatching.user.oauth2.controller;


import com.example.basketballmatching.auth.dto.AuthTokenResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.user.oauth2.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth2")
@Tag(name = "OAUTH2")
public class OAuth2Controller {

    private final OAuthService oAuthService;

    @Operation(summary = "카카오 로그인 요청 (리다이렉트)")
    @GetMapping("/login/kakao")
    public void getKakaoAuthUrl(HttpServletResponse response) throws IOException {

        response.sendRedirect(oAuthService.responseUrl());

    }

    @Operation(summary = "카카오 콜백 (code 수신 후 로그인 처리)")
    @ApiResponse(responseCode = "200", description = "카카오 로그인 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @GetMapping("/kakao")
    public ResponseEntity<CommonResponse<AuthTokenResponse>> kakaoLogin(
            @RequestParam(name = "code") String code
    ) throws IOException{

        log.info("[카카오 API 서버] code : {}", code);

        AuthTokenResponse tokenDto = oAuthService.kakaoLogin(code);
        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDto.accessToken());



        return ResponseEntity.ok()
                .headers(headers)
                .body(CommonResponse.of("카카오 로그인에 성공하였습니다.", tokenDto));
    }
}
