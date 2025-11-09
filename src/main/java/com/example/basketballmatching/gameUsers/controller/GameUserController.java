package com.example.basketballmatching.gameUsers.controller;


import com.example.basketballmatching.gameUsers.dto.ApplyGameUserDto;
import com.example.basketballmatching.gameUsers.service.GameUserService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/game")
public class GameUserController {

    private final GameUserService gameUserService;

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<ApplyGameUserDto>> applyGame(
            @RequestParam Long gameId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
            ) {

        ApiResponse<ApplyGameUserDto> applyGame = gameUserService.applyGame(gameId, userInfoDetails.getUserEntity().getUserId());

        return ResponseEntity.ok(applyGame);

    }

}
