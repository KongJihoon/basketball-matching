package com.example.basketballmatching.game.domain;


import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.game.type.GameUserLevel;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.domain.UserEntity;
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
    private int participantCount = 0;

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

    public void editGameInfo(String title, String content, Integer headCount, MatchFormat matchFormat, MatchGenderType matchGenderType) {

        if (title != null) {
            this.title = title;
        }

        if (content != null) {
            this.content = content;
        }

        if (headCount > 0) {
            this.headCount = headCount;
        }

        if (matchFormat != null) {
            this.matchFormat = matchFormat;
        }

        if (matchGenderType != null) {
            this.matchGenderType = matchGenderType;
        }





    }

    public static GameEntity create(String title, String content, Integer headCount, FieldStatus fieldStatus, MatchFormat matchFormat
    , MatchGenderType matchGenderType, LocalDateTime startDateTime, LocalDateTime endDateTime, String placeName, String address, CityName cityName, Double latitude, Double longitude, UserEntity userEntity) {

        return GameEntity.builder()
                .title(title)
                .content(content)
                .headCount(headCount)
                .fieldStatus(fieldStatus)
                .matchGenderType(matchGenderType)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .placeName(placeName)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .cityName(cityName)
                .matchFormat(matchFormat)
                .gameStatus(GameStatus.RECRUITING)
                .userEntity(userEntity)
                .build();
    }

    public void cancelByCreatorWithdrawal(LocalDateTime now) {
        if (deletedDateTime != null) {
            return;
        }

        this.deletedDateTime = now;
    }

    public void setGameUserLevel(GameUserLevel gameUserLevel) {
        this.gameUserLevel = gameUserLevel;
    }


    public void increaseParticipantCount() {
        this.participantCount++;
    }

    public void decreaseParticipantCount() {
        this.participantCount--;
    }

    public void setStatue(GameStatus status) {
        this.gameStatus = status;
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
