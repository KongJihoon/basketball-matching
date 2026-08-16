package com.example.basketballmatching.game.controller;


import com.example.basketballmatching.game.dto.response.MyUpcomingGameResponse;
import com.example.basketballmatching.game.service.MyGameService;
import com.example.basketballmatching.global.dto.CommonResponse;
import com.example.basketballmatching.global.security.UserInfoDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mypage/games")
@RequiredArgsConstructor
@Validated
@Tag(name = "MY_GAME")
public class MyGameQueryController {

    private final MyGameService myGameService;

    @Operation(
            summary = "내 예정 경기 조회",
            description = "참가가 확정된 예정 경기를 시작 시각순으로 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "내 예정 경기 조회 성공"
    )
    @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음"
    )
    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommonResponse<Page<MyUpcomingGameResponse>>> getUpcomingGames(
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100)
            int size,

            @AuthenticationPrincipal
            UserInfoDetails userInfoDetails
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<MyUpcomingGameResponse> response = myGameService.getUpcomingGames(userInfoDetails.getUserEntity().getUserId(), pageRequest);

        return ResponseEntity.ok(
                CommonResponse.of("내 예정 경기 조회가 완료되었습니다.", response)
        );
    }
}
