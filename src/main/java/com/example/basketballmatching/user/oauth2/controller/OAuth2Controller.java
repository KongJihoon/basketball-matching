package com.example.basketballmatching.user.oauth2.controller;


import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.user.oauth2.dto.KakaoDto.Response;
import com.example.basketballmatching.user.oauth2.service.OAuthService;
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
public class OAuth2Controller {

    private final OAuthService oAuthService;

    private final AuthService authService;

    @GetMapping("/login/kakao")
    public void getKakaoAuthUrl(HttpServletResponse response) throws IOException {

        response.sendRedirect(oAuthService.responseUrl());

    }


    @GetMapping("/kakao")
    public ResponseEntity<ApiResponse<Response>> kakaoLogin(
            @RequestParam(name = "code") String code
    ) throws IOException{

        log.info("[카카오 API 서버] code : {}", code);

        TokenDto tokenDto = oAuthService.kakaoLogin(code);
        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDto.getAccessToken());



        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.of("카카오 로그인에 성공하였습니다.", Response.fromDto(tokenDto.getUserDto(), tokenDto.getRefreshToken())));
    }
}
