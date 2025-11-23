package com.example.basketballmatching.auth.controller;


import com.example.basketballmatching.auth.dto.LoginDto;
import com.example.basketballmatching.auth.dto.ReIssueTokenDto;
import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AUTH")
public class AuthController {



    private final AuthService authService;


    /**
        유저 로그인
     */
    @Operation(summary = "유저 로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<TokenDto>> loginUser(
            @RequestBody @Valid LoginDto.Request request
    ) {

        TokenDto token = authService.loginUser(request.getEmail(), request.getPassword());

        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(CommonResponse.of("로그인에 성공하였습니다.", token));

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
    @PostMapping("/reissue")
    public ResponseEntity<CommonResponse<TokenDto>> reissue(
            @RequestBody @Valid ReIssueTokenDto request
            ) {

        TokenDto reissueToken = authService.reissue(request.getEmail(), request.getRefreshToken());

        HttpHeaders headers = new HttpHeaders();

        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + reissueToken.getAccessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(CommonResponse.of("토큰 재발급에 성공하였습니다.", reissueToken));

    }

    /**
     * 유저 로그아웃
     */
    @Operation(summary = "유저 로그아웃")
    @ApiResponse(responseCode = "200", description = "유저 로그아웃 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
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
