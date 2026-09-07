package com.example.basketballmatching.user.controller;


import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.exception.dto.ErrorResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import com.example.basketballmatching.user.dto.*;
import com.example.basketballmatching.user.service.UserService;
import com.example.basketballmatching.user.service.UserWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "USER")
public class UserController {


    private final UserService userService;

    private final UserWithdrawalService userWithdrawalService;


    /**
     * 회원가입
     */
    @Operation(summary = "회원가입")
    @ApiResponse(responseCode = "201", description = "회원가입에 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = "409", description = "중복 데이터",
    content = {@Content(mediaType = "application/json",
    schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignUpResponse>> signup(
            @RequestBody @Valid SignUpRequest request
            ) {

        SignUpResponse response = userService.signUp(request);


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(
                        "회원가입에 성공하였습니다.",
                        response
                ));

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
    @GetMapping("/availability/email")
    public ResponseEntity<CheckResponse> checkEmail(
            @Parameter(name = "email", example = "test@test.com", required = true)
            @RequestParam String email
    ) {

        userService.checkEmail(email);

        return ResponseEntity.ok(
                CheckResponse.of(true, "사용가능한 이메일입니다.")
        );
    }

    @Operation(summary = "닉네임 중복 확인")
    @ApiResponse(responseCode = "200", description = "닉네임 중복 확인 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "409", description = "중복 데이터",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/availability/nickname")
    public ResponseEntity<CheckResponse> checkNickname(
            @Parameter(name = "nickname", example = "커리", required = true)
            @RequestParam String nickname
    ) {

        userService.checkNickname(nickname);

        return ResponseEntity.ok(
                CheckResponse.of(true, "사용 가능한 닉네입입니다.")
        );
    }



    @Operation(summary = "회원 정보 조회")
    @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공")
    @ApiResponse(responseCode = "403", description = "권한 부족",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @GetMapping("/mypage")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CommonResponse<UserProfileResponse>> getUserInfo(@AuthenticationPrincipal UserInfoDetails userInfoDetails) {

        Long userId = userInfoDetails.getUserEntity().getUserId();

        UserProfileResponse response = userService.getUserInfo(userId);

        return ResponseEntity.ok(
                    CommonResponse.of("회원정보 조회에 성공하였습니다.", response)
        );

    }

    @Operation(summary = "회원 정보 수정")
    @ApiResponse(responseCode = "200", description = "회원 정보 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PatchMapping("/mypage")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CommonResponse<UserProfileResponse>> editUserInfo(@AuthenticationPrincipal UserInfoDetails userInfoDetails, @RequestBody @Valid UpdateUserRequest request) {

        Long userId = userInfoDetails.getUserEntity().getUserId();


        UserProfileResponse response = userService.editUserInfo(userId, request);


        return ResponseEntity.ok(
                CommonResponse.of("회원정보 수정이 완료되었습니다.", response)
        );

    }




    /**
     * 로그인 사용자 비밀번호 변경
     */
    @Operation(summary = "비밀번호 변경")
    @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PreAuthorize("hasAnyRole('USER')")
    @PatchMapping("/mypage/password")
    public ResponseEntity<CheckResponse> changePassword(
            @RequestBody @Valid ChangePasswordRequest request, @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        Long userId = userInfoDetails.getUserEntity().getUserId();

        userService.changePassword(userId, request);

        return ResponseEntity.ok(
                CheckResponse.of(true, "비밀번호 변경을 완료하였습니다.")
        );

    }

    /**
     * 로그인한 사용자의 회원 탈퇴를 요청한다.
     * Access Token은 탈퇴 후 현재 세션을 폐기하기 위해 전달한다.
     */
    @Operation(summary = "회원 탈퇴")
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = CheckResponse.class))})
    @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))})
    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/mypage")
    public ResponseEntity<CheckResponse> withdraw(
            HttpServletRequest request,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails

    ) {

        String accessToken = resolveAccessToken(request);

        userWithdrawalService.withdraw(
                userInfoDetails.getUserEntity().getUserId(), accessToken
        );


        return ResponseEntity.ok(
                CheckResponse.of(
                        true, "회원탈퇴에 성공하였습니다."
                )
        );

    }

    /**
     * Authorization 헤더의 Bearer 토큰에서 Access Token을 추출한다.
     * 올바른 Bearer 형식이 아니면 서비스에서 검증할 수 있도록 null 반환.
     */
    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring(7);
    }


}
