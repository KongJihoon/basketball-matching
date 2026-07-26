package com.example.basketballmatching.user.controller;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.user.dto.ConfirmEmailVerificationRequest;
import com.example.basketballmatching.user.dto.SendEmailVerificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email-verifications")
@RequiredArgsConstructor
@Tag(name = "EMAIL_VERIFICATION")
public class EmailVerificationController {

    private final MailService mailService;

    /**
     * 회원가입 이메일 전송
     */
    @Operation(summary = "회원가입 이메일 전송")
    @ApiResponse(responseCode = "202", description = "회원가입 이메일 전송 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(
            responseCode = "400",
            description = "올바르지 않은 이메일",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "내부 서버 오류",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping
    public ResponseEntity<CheckResponse> sendMailAuth(
            @RequestBody @Valid SendEmailVerificationRequest request
            ) {
        mailService.sendAuthMail(request.email());

        return ResponseEntity.
                accepted().body(CheckResponse.of(true, "이메일 인증번호가 전송되었습니다."));
    }


    /**
     * 회원가입 이메일 인증번호 확인
     */
    @Operation(summary = "회원가입 이메일 인증번호 확인")
    @ApiResponse(responseCode = "200", description = "회원가입 이메일 인증번호 확인 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/confirm")
    public ResponseEntity<CheckResponse> verifyEmailAuth(
            @RequestBody @Valid ConfirmEmailVerificationRequest request
            ) {

        mailService.verifyEmailAuth(request.email(), request.code());

        return ResponseEntity.ok(
                CheckResponse.of(true, "이메일 인증에 성공하였습니다.")
        );

    }
}
