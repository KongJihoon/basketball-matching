package com.example.basketballmatching.notifications.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {

    KICKED_OUT("강퇴"),
    ACCEPT_GAME("경기 참가 수락"),
    REJECT_GAME("경기 참가 거절"),
    DELETE_GAME("경기 삭제");

    private final String description;
}
