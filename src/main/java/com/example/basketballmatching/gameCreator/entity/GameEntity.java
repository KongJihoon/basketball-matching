package com.example.basketballmatching.gameCreator.entity;


import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.entity.UserEntity;
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
public class GameEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gameId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int headCount;

    @Builder.Default
    @Column(nullable = false)
    private int participantCount = 1;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FieldStatus fieldStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchGenderType matchGenderType;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column
    private LocalDateTime endDateTime;


    private LocalDateTime deletedDateTime;

    @Column(nullable = false)
    private String placeName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CityName cityName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchFormat matchFormat;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GameStatus gameStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private UserEntity userEntity;

    public void editGameInfo(EditGameDto editGameDto) {

        if (editGameDto.getTitle() != null) {
            this.title = editGameDto.getTitle();
        }

        if (editGameDto.getContent() != null) {
            this.content = editGameDto.getContent();
        }

        if (editGameDto.getHeadCount() > 0) {
            this.headCount = editGameDto.getHeadCount();
        }

        if (editGameDto.getMatchFormat() != null) {
            this.matchFormat = editGameDto.getMatchFormat();
        }





    }

    // 테스트용
    public void setDeletedDateTime(LocalDateTime deletedDateTime) {
        this.deletedDateTime = deletedDateTime;
    }




}
