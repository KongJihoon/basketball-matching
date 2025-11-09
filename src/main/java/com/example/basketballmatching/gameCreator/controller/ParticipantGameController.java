package com.example.basketballmatching.gameCreator.controller;


import com.example.basketballmatching.gameCreator.dto.ApplyGameUserListDto;
import com.example.basketballmatching.gameCreator.service.ParticipantGameService;
import com.example.basketballmatching.global.dto.ApiResponse;
import com.example.basketballmatching.global.dto.CheckResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/creator")
@RequiredArgsConstructor
public class ParticipantGameController {

    private final ParticipantGameService participantGameService;

    @GetMapping("/apply-user")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<List<ApplyGameUserListDto>>> getApplyParticipantList (
            @RequestParam Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails

            ) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");

        ApiResponse<List<ApplyGameUserListDto>> participantList = participantGameService.getApplyParticipantList(gameId, userInfoDetails.getUserEntity().getUserId(), pageRequest);



        return ResponseEntity.ok(participantList);
    }

    @PatchMapping("/accept-user")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> acceptGameUser (
            @RequestParam Long gameId,
            @RequestParam Long participantId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.acceptGameUser(participantId, userInfoDetails.getUserEntity().getUserId(), gameId);

        return ResponseEntity.ok(checkResponse);
    }

}
