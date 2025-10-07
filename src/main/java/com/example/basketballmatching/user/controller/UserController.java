package com.example.basketballmatching.user.controller;


import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.user.dto.SignUpDto;
import com.example.basketballmatching.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpDto.Response>> signup(
            @RequestBody @Valid SignUpDto.Request request
            ) {

        ApiResponse<SignUpDto.Response> response = userService.signUp(request);


        return ResponseEntity.ok(response);

    }



}
