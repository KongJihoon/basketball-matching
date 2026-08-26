package com.example.basketballmatching.blacklist.dto.response;

import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import com.example.basketballmatching.blacklist.type.BlackListStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record BlackListResponse(
        @Schema(example = "1")
        Long blackListId,

        @Schema(example = "1")
        Long reportId,

        @Schema(example = "3")
        Long targetUserId,

        @Schema(example = "user@example.com")
        String email,

        @Schema(example = "농구왕")
        String nickname,

        @Schema(example = "2")
        Long bannedByUserId,

        @Schema(example = "2026-08-25T14:00:00")
        LocalDateTime bannedAt,

        @Schema(example = "2026-09-01T14:00:00")
        LocalDateTime expiresAt,

        @Schema(example = "ACTIVE")
        BlackListStatus status
) {
    public static BlackListResponse fromEntity(BlackListEntity blackList, LocalDateTime now) {

        return new BlackListResponse(
                blackList.getBlackListId(),
                blackList.getReportEntity().getReportId(),
                blackList.getUserEntity().getUserId(),
                blackList.getUserEntity().getEmail(),
                blackList.getUserEntity().getNickname(),
                blackList.getBannedBy().getUserId(),
                blackList.getBannedDateTime(),
                blackList.getExpiresAt(),
                blackList.getStatus(now)
        );
    }

}
