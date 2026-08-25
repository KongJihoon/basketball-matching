package com.example.basketballmatching.blacklist.dto;

import com.example.basketballmatching.blacklist.domain.BlackListEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class BlackListDto {

    private Long blackListUserId;

    private LocalDateTime bannedDateTime;

    public static BlackListDto fromEntity(BlackListEntity blackListEntity) {
        return BlackListDto.builder()
                .blackListUserId(blackListEntity.getUserEntity().getUserId())
                .bannedDateTime(blackListEntity.getBannedDateTime())
                .build();
    }

}
