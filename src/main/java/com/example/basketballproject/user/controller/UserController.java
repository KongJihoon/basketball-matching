package com.example.basketballproject.user.controller;

import com.example.basketballproject.user.dto.DeleteUserDto;
import com.example.basketballproject.user.dto.FindUserDto;
import com.example.basketballproject.user.entity.UserEntity;
import com.example.basketballproject.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
@RequestMapping("/api/user")
@RestController
public class UserController {

    private final UserService userService;


    @GetMapping("/find/id")
    public ResponseEntity<String> findLoginId(
            @RequestParam String email
            ) {

        String loginId = userService.findLoginId(email);

        return ResponseEntity.ok(loginId);

    }

    @PatchMapping("/find/password")
    public ResponseEntity<Boolean> findPassword(
            @RequestParam String loginId
    ) throws NoSuchAlgorithmException {
        boolean password = userService.findPassword(loginId);

        return ResponseEntity.ok(password);
    }



}
