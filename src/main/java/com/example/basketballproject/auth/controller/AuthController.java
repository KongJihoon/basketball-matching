package com.example.basketballproject.auth.controller;

import com.example.basketballproject.auth.dto.SignInDto;
import com.example.basketballproject.auth.dto.SignUpDto;
import com.example.basketballproject.auth.dto.TokenDto;
import com.example.basketballproject.auth.service.AuthService;
import com.example.basketballproject.global.dto.SendMailRequest;
import com.example.basketballproject.global.dto.VerifyMailRequest;
import com.example.basketballproject.global.service.MailService;
import com.example.basketballproject.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;

    private final MailService mailService;


    /**
     * 고객 회원가입
     */
    @PostMapping("/user")
    public ResponseEntity<?> signUpUser(@RequestBody @Validated SignUpDto.Request request) {

        UserDto userDto = authService.signUp(request);

        return ResponseEntity.ok(SignUpDto.Response.toEntity(userDto));
    }


    /**
     * 이메일 인증번호 전송
     */
    @PostMapping("/mail/certification")
    public ResponseEntity<?> sendCertificationMail(@RequestBody @Validated SendMailRequest request) {

        mailService.sendAuthMail(request.getEmail());

        return ResponseEntity.ok().body("이메일 전송 완료");
    }


    /**
     * 이메일 인증
     */
    @PostMapping("/mail/verify")
    public ResponseEntity<?> sendVerifyMail(@RequestBody @Validated VerifyMailRequest request) {

        mailService.verifyEmail(request.getEmail(), request.getCode());

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * 유저 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
            @RequestBody @Validated SignInDto.Request request){

        UserDto userDto = authService.loginUser(request);

        TokenDto token = authService.getToken(userDto);

        HttpHeaders responseHeader = new HttpHeaders();

        responseHeader.set("Authorization", token.getAccessToken());

        return ResponseEntity.ok()
                .headers(responseHeader)
                .body(SignInDto.Response.fromDto(userDto, token.getRefreshToken()));
    }

}
