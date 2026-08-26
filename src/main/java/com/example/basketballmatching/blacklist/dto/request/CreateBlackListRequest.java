package com.example.basketballmatching.blacklist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateBlackListRequest(
        @Schema(description = "제재 근거가 되는 승인된 신고 ID", example = "1")
        @NotNull
        Long reportId
) {
}
