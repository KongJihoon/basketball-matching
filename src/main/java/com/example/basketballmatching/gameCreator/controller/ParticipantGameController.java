package com.example.basketballmatching.gameCreator.controller;


import com.example.basketballmatching.gameCreator.dto.AcceptGameUserListDto;
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

    @GetMapping("/search/apply")
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

    @GetMapping("/search/accept")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<ApiResponse<List<AcceptGameUserListDto>>> getAcceptParticipantList(
            @RequestParam Long gameId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");

        ApiResponse<List<AcceptGameUserListDto>> acceptParticipantList = participantGameService.getAcceptParticipantList(gameId, userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(acceptParticipantList);
    }


    @PatchMapping("/accept")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> acceptGameUser (
            @RequestParam Long gameId,
            @RequestParam Long participantId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.acceptGameUser(participantId, userInfoDetails.getUserEntity().getUserId(), gameId);

        return ResponseEntity.ok(checkResponse);
    }

    @PatchMapping("/reject")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> rejectGameUser (
            @RequestParam Long gameId,
            @RequestParam Long participantId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.rejectGameUser(participantId, userInfoDetails.getUserEntity().getUserId(), gameId);

        return ResponseEntity.ok(checkResponse);
    }

    @PatchMapping("/kickout")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> kickoutGameUser (
            @RequestParam Long gameId,
            @RequestParam Long participantId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.kickOutGameUser(participantId, userInfoDetails.getUserEntity().getUserId(), gameId);

        return ResponseEntity.ok(checkResponse);

    }

    @PatchMapping("/delete")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<CheckResponse> deleteGame (
            @RequestParam Long gameId,
            @AuthenticationPrincipal UserInfoDetails userInfoDetails
    ) {

        CheckResponse checkResponse = participantGameService.deleteGame(userInfoDetails.getUserEntity().getUserId(), gameId);


        return ResponseEntity.ok(checkResponse);
    }

}
