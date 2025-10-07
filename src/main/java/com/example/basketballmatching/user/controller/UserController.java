package com.example.basketballmatching.user.controller;


import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.VerifyEmailDto;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;

    private final MailService mailService;


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpDto.Response>> signup(
            @RequestBody @Valid SignUpDto.Request request
            ) {

        ApiResponse<SignUpDto.Response> response = userService.signUp(request);


        return ResponseEntity.ok(response);

    }

    @PostMapping("/check-email")
    public ResponseEntity<CheckResponse> checkEmail(
            @RequestParam String email
    ) {
        CheckResponse checkResponse = userService.checkEmail(email);

        return ResponseEntity.ok(checkResponse);
    }

    @PostMapping("/check-nickname")
    public ResponseEntity<CheckResponse> checkNickname(
            @RequestParam String nickname
    ) {
        CheckResponse checkResponse = userService.checkNickname(nickname);

        return ResponseEntity.ok(checkResponse);
    }

    @PostMapping("send-mail")
    public ResponseEntity<CheckResponse> sendMailAuth(
            @RequestParam String email
    ) {
        CheckResponse checkResponse = mailService.sendAuthMail(email);

        return ResponseEntity.ok(checkResponse);
    }

    @PostMapping("verify-mail")
    public ResponseEntity<CheckResponse> verifyEmailAuth(
            @RequestBody VerifyEmailDto request
            ) {

        CheckResponse checkResponse = mailService.verifyEmailAuth(request.getEmail(), request.getCode());

        return ResponseEntity.ok(checkResponse);

    }

}
