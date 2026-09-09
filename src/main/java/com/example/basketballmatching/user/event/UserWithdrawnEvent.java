package com.example.basketballmatching.user.event;

import com.example.basketballmatching.game.dto.GameCancelNotificationDto;

import java.util.List;


/**
 * 회원 탈퇴 커밋 이후 처리할 세션 폐기와
 * 경기 취소 알림에 필요한 데이터 전달
 */
public record UserWithdrawnEvent(
        Long userId,
        String email,
        String accessToken,
        List<GameCancelNotificationDto> notices
) {

    public UserWithdrawnEvent {
        notices = List.copyOf(notices);
    }
}
