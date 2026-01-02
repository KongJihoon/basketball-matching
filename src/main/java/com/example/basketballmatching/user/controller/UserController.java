package com.example.basketballmatching.user.controller;


import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.VerifyEmailDto;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.global.service.MailService;
import com.example.basketballmatching.user.dto.*;
import com.example.basketballmatching.user.dto.SignUpDto.Response;
import com.example.basketballmatching.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "USER")
public class UserController {


    private final UserService userService;

    private final MailService mailService;


    /**
     * 회원가입
     */
    @Operation(summary = "회원가입")
    @ApiResponse(responseCode = "200", description = "회원가입에 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = "409", description = "중복 데이터",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<Response>> signup(
            @RequestBody @Valid SignUpDto.Request request
            ) {

        CommonResponse<Response> response = userService.signUp(request);


        return ResponseEntity.ok(response);

    }

    /**
     * 이메일 중복 확인
     */

    @Operation(summary = "이메일 중복 확인")
    @ApiResponse(responseCode = "200", description = "이메일 중복 확인 성공",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "409", description = "중복 데이터",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/check-email")
    public ResponseEntity<CheckResponse> checkEmail(
            @Parameter(name = "email", example = "test@test.com", required = true)
            @RequestParam String email
    ) {
        CheckResponse checkResponse = userService.checkEmail(email);

        return ResponseEntity.ok(checkResponse);
    }

    @Operation(summary = "닉네임 중복 확인")
    @ApiResponse(responseCode = "200", description = "닉네임 중복 확인 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "409", description = "중복 데이터",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/check-nickname")
    public ResponseEntity<CheckResponse> checkNickname(
            @Parameter(name = "nickname", example = "커리", required = true)
            @RequestParam String nickname
    ) {
        CheckResponse checkResponse = userService.checkNickname(nickname);

        return ResponseEntity.ok(checkResponse);
    }



    @Operation(summary = "회원가입 이메일 전송")
    @ApiResponse(responseCode = "200", description = "회원가입 이메일 전송 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "500", description = "내부 서버 오류",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/send-mail")
    public ResponseEntity<CheckResponse> sendMailAuth(
            @Parameter(name = "email", example = "test@test.com", required = true)
            @RequestParam String email
    ) {
        mailService.sendAuthMail(email);

        return ResponseEntity.ok(CheckResponse.of(true, "이메일 인증번호가 전송되었습니다."));
    }

    @Operation(summary = "회원가입 이메일 인증번호 확인")
    @ApiResponse(responseCode = "200", description = "회원가입 이메일 인증번호 확인 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/verify-mail")
    public ResponseEntity<CheckResponse> verifyEmailAuth(
            @RequestBody VerifyEmailDto request
            ) {

        CheckResponse checkResponse = mailService.verifyEmailAuth(request.getEmail(), request.getCode());

        return ResponseEntity.ok(checkResponse);

    }

    @Operation(summary = "회원 정보 조회")
    @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 부족",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/user-info")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CommonResponse<UserDto>> getUserInfo(@AuthenticationPrincipal UserInfoDetails userInfoDetails) {

        CommonResponse<UserDto> userInfo = userService.getUserInfo(userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(userInfo);

    }

    @Operation(summary = "회원 정보 수정")
    @ApiResponse(responseCode = "200", description = "회원 정보 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/edit-info")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<UserDto>> editUserInfo(@AuthenticationPrincipal UserInfoDetails userInfoDetails, @RequestBody @Valid EditUserDto editUserDto) {

        Long userId = userInfoDetails.getUserEntity().getUserId();

        CommonResponse<UserDto> response = userService.editUserInfo(userId, editUserDto);

        return ResponseEntity.ok(response);

    }
    @Operation(summary = "비밀번호 변경 인증번호 전송")
    @ApiResponse(responseCode = "200", description = "비밀번호 변경 인증번호 전송 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "500", description = "내부 서버 오류",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/password/send-auth")
    public ResponseEntity<CheckResponse> sendPasswordAuthCode(
            @Parameter(name = "email", example = "test@test.com", required = true)
            @RequestParam String email
    ) {

        mailService.sendPasswordAuthCode(email);

        return ResponseEntity.ok(CheckResponse.of(true, "인증번호가 전송되었습니다."));

    }

    @Operation(summary = "비밀번호 변경 인증번호 확인")
    @ApiResponse(responseCode = "200", description = "비밀번호 변경 인증번호 확인 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/password/verify-code")
    public ResponseEntity<CheckResponse> verifyPasswordCode(
            @RequestBody VerifyEmailDto request
    ) {

        CheckResponse checkResponse = userService.verifyPasswordCode(request.getEmail(), request.getCode());

        return ResponseEntity.ok(checkResponse);
    }


    @Operation(summary = "비밀번호 찾기 비밀번호 변경")
    @ApiResponse(responseCode = "200", description = "비밀번호 찾기 비밀번호 변경 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/password/reset")
    public ResponseEntity<CheckResponse> resetPassword(
            @RequestBody @Valid ResetPasswordDto request
            ) {

        CheckResponse checkResponse = userService.resetPassword(request.getEmail(), request.getPassword(), request.getCheckPassword());


        return ResponseEntity.ok(checkResponse);

    }

    @Operation(summary = "비밀번호 변경")
    @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PreAuthorize("hasAnyRole('USER')")
    @PatchMapping("/password/change")
    public ResponseEntity<CheckResponse> changePassword(
            @RequestBody @Valid ChangePasswordDto request, @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        CheckResponse checkResponse = userService.changePassword(userInfoDetails.getUserEntity().getUserId(), request);

        return ResponseEntity.ok(checkResponse);

    }

    @Operation(summary = "회원 탈퇴")
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PreAuthorize("hasAnyRole('USER')")
    @PatchMapping("/delete")
    public ResponseEntity<CheckResponse> deleteUser(
            HttpServletRequest request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails

    ) {

        String accessToken = request.getHeader("Authorization");

        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);

        }

        CheckResponse checkResponse = userService.deleteUser(userInfoDetails.getUserEntity().getUserId(), accessToken);

        return ResponseEntity.ok(checkResponse);

    }


}
