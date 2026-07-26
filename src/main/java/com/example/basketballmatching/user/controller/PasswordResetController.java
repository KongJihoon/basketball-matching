package com.example.basketballmatching.user.controller;

import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.user.dto.ConfirmEmailVerificationRequest;
import com.example.basketballmatching.user.dto.ResetPasswordRequest;
import com.example.basketballmatching.user.dto.SendEmailVerificationRequest;
import com.example.basketballmatching.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/password-resets")
@RequiredArgsConstructor
@Tag(name = "USER")
public class PasswordResetController {

    private final MailService mailService;
    private final UserService userService;

    /**
     * 비밀번호 재설정 인증번호 전송
     */
    @Operation(summary = "비밀번호 재설정 인증번호 전송")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "인증번호 전송 요청 접수"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "올바르지 않은 이메일",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "가입된 사용자를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "메일 발송 처리 실패",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/email-verifications")
    public ResponseEntity<CheckResponse> sendPasswordAuthCode(
            @RequestBody @Valid SendEmailVerificationRequest request
            ) {

        mailService.sendPasswordAuthCode(request.email());

        return ResponseEntity
                .accepted().body(CheckResponse.of(true, "인증번호가 전송되었습니다."));

    }

    /**
     * 비밀번호 재설정 인증번호 확인
     */
    @Operation(
            summary = "비밀번호 재설정 인증번호 확인"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증번호 확인 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "인증번호가 없거나 일치하지 않음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "가입된 사용자를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping(
            "/email-verifications/confirm"
    )
    public ResponseEntity<CheckResponse>
    confirmVerificationCode(
            @RequestBody @Valid
            ConfirmEmailVerificationRequest request
    ) {

        userService.verifyPasswordCode(request.email(), request.code());

        return ResponseEntity.ok(
                CheckResponse.of(true, "비밀번호를 변경해주세요.")
        );
    }

    /**
     * 비밀번호 재설정
     * PATCH /api/v1/password-resets
     */
    @Operation(
            summary = "비밀번호 재설정"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이메일 미인증 또는 비밀번호 확인 불일치",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "가입된 사용자를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PatchMapping
    public ResponseEntity<CheckResponse>
    resetPassword(
            @RequestBody @Valid
            ResetPasswordRequest request
    ) {

        userService.resetPassword(request.email(), request.password(), request.checkPassword());

        return ResponseEntity.ok(
                CheckResponse.of(true, "비밀번호 변경을 완료하였습니다.")

        );
    }
}
