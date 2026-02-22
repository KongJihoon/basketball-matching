package com.example.basketballmatching.gameCreator.entity;


import com.example.basketballmatching.gameCreator.dto.EditGameDto;
import com.example.basketballmatching.gameCreator.type.*;
import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
// 중복 생성 방지 DB Unique 인덱스
@Table(
        name = "game_entity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_game_place_time",
                        columnNames = {
                                "place_name",
                                "address",
                                "start_date_time",
                                "end_date_time"
                        }
                )
        }
)
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GameUserLevel gameUserLevel;

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

        if (editGameDto.getMatchGenderType() != null) {
            this.matchGenderType = editGameDto.getMatchGenderType();
        }





    }

    public void increaseParticipantCount() {
        this.participantCount++;
    }

    public void decreaseParticipantCount() {
        this.participantCount--;
    }

    // 테스트용
    public void setDeletedDateTime(LocalDateTime deletedDateTime) {
        this.deletedDateTime = deletedDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }


    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }


    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }
}
