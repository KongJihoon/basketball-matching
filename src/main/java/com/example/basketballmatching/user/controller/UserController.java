package com.example.basketballmatching.user.controller;


import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.VerifyEmailDto;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.user.dto.*;
import com.example.basketballmatching.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/send-mail")
    public ResponseEntity<CheckResponse> sendMailAuth(
            @RequestParam String email
    ) {
        mailService.sendAuthMail(email);

        return ResponseEntity.ok(CheckResponse.of(true, "이메일 인증번호가 전송되었습니다."));
    }

    @PostMapping("/verify-mail")
    public ResponseEntity<CheckResponse> verifyEmailAuth(
            @RequestBody VerifyEmailDto request
            ) {

        CheckResponse checkResponse = mailService.verifyEmailAuth(request.getEmail(), request.getCode());

        return ResponseEntity.ok(checkResponse);

    }

    @GetMapping("/user-info")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<UserDto>> getUserInfo(@AuthenticationPrincipal UserInfoDetails userInfoDetails) {

        ApiResponse<UserDto> userInfo = userService.getUserInfo(userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(userInfo);

    }

    @PatchMapping("/edit-info")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<UserDto>> editUserInfo(@AuthenticationPrincipal UserInfoDetails userInfoDetails, @RequestBody @Valid EditUserDto editUserDto) {

        Long userId = userInfoDetails.getUserEntity().getUserId();

        ApiResponse<UserDto> response = userService.editUserInfo(userId, editUserDto);

        return ResponseEntity.ok(response);

    }

    @PostMapping("/password/send-auth")
    public ResponseEntity<CheckResponse> sendPasswordAuthCode(
            @RequestParam String email
    ) {

        mailService.sendPasswordAuthCode(email);

        return ResponseEntity.ok(CheckResponse.of(true, "인증번호가 전송되었습니다."));

    }

    @PostMapping("/password/verify-code")
    public ResponseEntity<CheckResponse> verifyPasswordCode(
            @RequestBody VerifyEmailDto request
    ) {

        CheckResponse checkResponse = userService.verifyPasswordCode(request.getEmail(), request.getCode());

        return ResponseEntity.ok(checkResponse);
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<CheckResponse> resetPassword(
            @RequestBody @Valid ResetPasswordDto request
            ) {

        CheckResponse checkResponse = userService.resetPassword(request.getEmail(), request.getPassword(), request.getCheckPassword());


        return ResponseEntity.ok(checkResponse);

    }

    @PreAuthorize("hasAnyRole('USER')")
    @PatchMapping("/password/change")
    public ResponseEntity<CheckResponse> changePassword(
            @RequestBody @Valid ChangePasswordDto request, @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        CheckResponse checkResponse = userService.changePassword(userInfoDetails.getUserEntity().getUserId(), request);

        return ResponseEntity.ok(checkResponse);

    }


}
