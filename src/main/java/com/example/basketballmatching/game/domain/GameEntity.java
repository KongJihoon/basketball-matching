package com.example.basketballmatching.game.domain;


import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.global.exception.ErrorCode;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.example.basketballmatching.global.exception.ErrorCode.*;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false)
    private int participantCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FieldStatus fieldStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MatchGenderType matchGenderType;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
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

    @Builder(access = AccessLevel.PRIVATE)
    private GameEntity(String title, String content, Integer headCount, Integer participantCount, FieldStatus fieldStatus, MatchFormat matchFormat,GameStatus gameStatus, GameUserLevel gameUserLevel
            , MatchGenderType matchGenderType, LocalDateTime startDateTime, LocalDateTime endDateTime, String placeName, String address, CityName cityName, Double latitude, Double longitude, UserEntity userEntity) {

        this.title = title;
        this.content = content;
        this.headCount = headCount;
        this.participantCount = participantCount;
        this.fieldStatus = fieldStatus;
        this.matchFormat = matchFormat;
        this.gameStatus = gameStatus;
        this.gameUserLevel = gameUserLevel;
        this.matchGenderType = matchGenderType;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.placeName = placeName;
        this.address = address;
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userEntity = userEntity;


    }

    public static GameEntity create(String title, String content, Integer headCount, FieldStatus fieldStatus, MatchFormat matchFormat
    , MatchGenderType matchGenderType, LocalDateTime startDateTime, LocalDateTime endDateTime, String placeName, String address, CityName cityName, Double latitude, Double longitude, UserEntity creator, LocalDateTime now) {

        validateSchedule(startDateTime, endDateTime, now);

        matchFormat.validateHeadCount(headCount);

        return GameEntity.builder()
                .title(title)
                .content(content)
                .headCount(headCount)
                .participantCount(0)
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
                .gameUserLevel(creator.getGameUserLevel())
                .userEntity(creator)
                .build();
    }

    public void updateGame(String title, String content, Integer headCount, MatchFormat matchFormat, MatchGenderType matchGenderType, LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime now) {

        validateMatchConditionChange(matchFormat, matchGenderType);

        validateScheduleChange(startDateTime, endDateTime, now);


        MatchFormat finalMatchFormat = matchFormat != null ? matchFormat : this.matchFormat;

        int finalHeadCount = headCount != null ? headCount : this.headCount;

        validateUpdateHeadCount(finalMatchFormat, finalHeadCount);


        if (title != null) {
            this.title = title;
        }

        if (content != null) {
            this.content = content;
        }

        if (headCount != null) {
            this.headCount = headCount;

            updateRecruitmentStatus(headCount);
        }

        if (matchFormat != null) {
            this.matchFormat = matchFormat;
        }

        if (matchGenderType != null) {
            this.matchGenderType = matchGenderType;


        }

        if (startDateTime != null) {
            this.startDateTime =
                    startDateTime;

            this.endDateTime =
                    endDateTime;
        }


    }

    private void validateScheduleChange(LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime now) {

        if (startDateTime == null && endDateTime == null) {
            return;
        }

        if (startDateTime == null || endDateTime == null) {
            throw new CustomException(GAME_SCHEDULE_REQUIRED_TOGETHER);
        }

        validateSchedule(startDateTime, endDateTime, now);

    }

    private static void validateSchedule(LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime now) {


        if (startDateTime == null || endDateTime == null || now == null
        || !startDateTime.isAfter(now) || !endDateTime.isAfter(startDateTime)) {
            throw new CustomException(INVALID_GAME_TIME);
        }

        long durationMinutes = Duration.between(
                startDateTime, endDateTime
        ).toMinutes();

        if (durationMinutes < 60 || durationMinutes > 120) {
            throw new CustomException(GAME_TIME_OUT_OF_RANGE);
        }

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
