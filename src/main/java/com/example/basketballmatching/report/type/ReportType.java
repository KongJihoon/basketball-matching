package com.example.basketballmatching.report.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportType {

    POOR_SPORTSMANSHIP("비매너 및 악의적 플레이"),
    NO_SHOW("무단 불참 / 노쇼"),
    ABUSIVE_LANGUAGE("욕설 및 언어폭력");

    private final String description;
}
