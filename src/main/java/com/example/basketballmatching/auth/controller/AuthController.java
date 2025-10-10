package com.example.basketballmatching.auth.controller;


import com.example.basketballmatching.auth.dto.LoginDto;
import com.example.basketballmatching.auth.dto.TokenDto;
import com.example.basketballmatching.auth.service.AuthService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
