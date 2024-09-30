package com.example.basketballproject.auth.controller;

import com.example.basketballproject.auth.dto.SignInDto;
import com.example.basketballproject.auth.dto.SignUpDto;
import com.example.basketballproject.auth.dto.TokenDto;
import com.example.basketballproject.auth.service.AuthService;
import com.example.basketballproject.global.dto.SendMailRequest;
import com.example.basketballproject.global.dto.VerifyMailRequest;
import com.example.basketballproject.global.service.MailService;
import com.example.basketballproject.user.dto.DeleteUserDto;
import com.example.basketballproject.user.dto.EditDto;
import com.example.basketballproject.user.dto.UserDto;
import com.example.basketballproject.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;

    private final MailService mailService;


    /**
     * 유저 회원가입
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

    /**
     * 유저 로그아웃
     */
    @PostMapping("/logOut")
    public ResponseEntity<?> logOut(HttpServletRequest request, @AuthenticationPrincipal UserEntity userEntity) {

        authService.logOut(request, userEntity);

        return ResponseEntity.ok(HttpStatus.OK);
    }


    /**
     * 유저 정보 조회
     */
    @GetMapping("/user/info")
    public ResponseEntity<UserDto> getUserInfo(
            HttpServletRequest request,
            @AuthenticationPrincipal UserEntity user
    ) {

        UserDto userInfo = authService.getUserInfo(request, user);

        return ResponseEntity.ok(userInfo);

    }


    /**
     * 유저 정보 수정
     */
    @PatchMapping("/user/edit")
    public ResponseEntity<EditDto.Response> editUserInfo(
            HttpServletRequest request,
            @RequestBody @Validated EditDto.Request editDto,
            @AuthenticationPrincipal UserEntity user
    ) {

        UserDto userDto = authService.editUserInfo(request, editDto, user);

        TokenDto token = authService.getToken(userDto);

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", token.getAccessToken());

        return ResponseEntity.ok()
                .headers(headers)
                .body(EditDto.Response.fromDto(userDto));

    }


    /**
     * 유저 회원 탈퇴
     */
    @PostMapping("/delete")
    public ResponseEntity<HttpStatus> deleteUser(
            HttpServletRequest request,
            @RequestBody @Validated DeleteUserDto deleteUserDto,
            @AuthenticationPrincipal UserEntity user
    ) {


        authService.deleteUser(request, deleteUserDto, user);


        return ResponseEntity.ok(HttpStatus.OK);
    }

}
