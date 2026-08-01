package com.example.basketballmatching.report.entity;


import com.example.basketballmatching.game.domain.GameEntity;
import com.example.basketballmatching.report.dto.CreateReportDto;
import com.example.basketballmatching.report.type.ReportType;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_user_id", nullable = false)
    private UserEntity reportUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private GameEntity gameEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime reportedDateTime;

    @Builder.Default
    private boolean isBanned = false;


    public static ReportEntity create(UserEntity reportUser, UserEntity targetUser, GameEntity gameEntity, CreateReportDto createReportDto) {
        return ReportEntity.builder()
                .reportUser(reportUser)
                .targetUser(targetUser)
                .gameEntity(gameEntity)
                .reportType(createReportDto.getReportType())
                .content(createReportDto.getContent())
                .reportedDateTime(LocalDateTime.now())
                .build();
    }

    public void setBanned() {
        this.isBanned = true;
    }



}
