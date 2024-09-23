package com.example.basketballproject.user.controller;

import com.example.basketballproject.auth.dto.SignUpDto;
import com.example.basketballproject.auth.service.AuthService;
import com.example.basketballproject.global.dto.SendMailRequest;
import com.example.basketballproject.global.dto.VerifyMailRequest;
import com.example.basketballproject.global.service.MailService;
import com.example.basketballproject.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
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


    @PostMapping("/user")
    public ResponseEntity<?> signUpUser(@RequestBody @Validated SignUpDto.Request request) {

        UserDto userDto = authService.signUp(request);

        return ResponseEntity.ok(SignUpDto.Response.toEntity(userDto));
    }

    @PostMapping("/mail/certification")
    public ResponseEntity<?> sendCertificationMail(@RequestBody @Validated SendMailRequest request) {

        mailService.sendAuthMail(request.getEmail());

        return ResponseEntity.ok().body("이메일 전송 완료");
    }

    @PostMapping("/mail/verify")
    public ResponseEntity<?> sendVerifyMail(@RequestBody @Validated VerifyMailRequest request) {

        mailService.verifyEmail(request.getEmail(), request.getCode());

        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
