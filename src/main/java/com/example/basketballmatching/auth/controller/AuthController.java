package com.example.basketballmatching.auth.controller;


import com.example.basketballmatching.auth.dto.LoginDto;
import com.example.basketballmatching.auth.dto.ReIssueTokenDto;
import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.user.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class AuthController {



    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenDto>> loginUser(
            @RequestBody @Valid LoginDto.Request request
    ) {

        TokenDto token = authService.loginUser(request.getEmail(), request.getPassword());

        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.of("로그인에 성공하였습니다.", token));

    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenDto>> reissue(
            @RequestBody @Valid ReIssueTokenDto request
            ) {

        TokenDto reissueToken = authService.reissue(request.getEmail(), request.getRefreshToken());

        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + reissueToken.getAccessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.of("토큰 재발급에 성공하였습니다.", reissueToken));

    }

    @PatchMapping("/logout")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> logoutUser(
            HttpServletRequest request, @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        String accessToken = request.getHeader("Authorization");

        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        CheckResponse checkResponse = authService.logoutUser(userInfoDetails.getUsername(), accessToken);

        return ResponseEntity.ok(checkResponse);

    }

}
