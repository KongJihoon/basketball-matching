package com.example.basketballmatching.game.domain;


import com.example.basketballmatching.game.type.*;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import com.example.basketballmatching.user.type.GenderType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

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


    private static final int CREATOR_COUNT = 1;

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

    public void deleteByCreator(UserEntity requester, LocalDateTime deletedAt) {

        validateDeleteRequester(requester);
        validateDeleteDeadline(deletedAt);

        this.deletedDateTime = deletedAt;

    }

    public void validateParticipantCancel(UserEntity participant, LocalDateTime now) {

        validateNotCreatorCancel(participant);

        LocalDateTime cancelDeadline = startDateTime.minusMinutes(30);

        if (!now.isBefore(cancelDeadline)) {
            throw new CustomException(NOT_ALLOWED_CANCEL);
        }
    }

    public void validateParticipantKickout(UserEntity participant, LocalDateTime now) {

        LocalDateTime kickoutDeadLine = startDateTime.minusHours(1);

        if (Objects.equals(userEntity.getUserId(), participant.getUserId())) {
            throw new CustomException(NOT_KICKOUT_CREATOR);
        }

        if (!now.isBefore(kickoutDeadLine)) {
            throw new CustomException(NOT_ALLOWED_KICKOUT);
        }

    }


    private void validateNotCreatorCancel(UserEntity participant) {
        if (Objects.equals(userEntity.getUserId(), participant.getUserId())) {
            throw new CustomException(NOT_CANCEL_GAME_CREATOR);
        }
    }

    private void validateDeleteRequester(UserEntity requester) {
        if (!Objects.equals(userEntity.getUserId(), requester.getUserId())) {
            throw new CustomException(NOT_GAME_CREATOR);
        }
    }

    private void validateDeleteDeadline(LocalDateTime deletedAt) {

        LocalDateTime deleteDeadline = startDateTime.minusHours(1);

        if (!deletedAt.isBefore(deleteDeadline)) {
            throw new CustomException(GAME_DELETE_NOT_ALLOWED_AT_THIS_TIME);
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

        if (startDateTime == null || endDateTime == null || now == null) {
            throw new CustomException(INVALID_GAME_TIME);
        }

        LocalDateTime minimumStartDateTime = now.plusHours(24);

        if (startDateTime.isBefore(minimumStartDateTime)) {
            throw new CustomException(GAME_SCHEDULE_TOO_SOON);
        }

        if (!endDateTime.isAfter(startDateTime)) {
            throw new CustomException(INVALID_GAME_TIME);
        }


        long durationMinutes = Duration.between(
                startDateTime, endDateTime
        ).toMinutes();

        if (durationMinutes < 60 || durationMinutes > 120) {
            throw new CustomException(GAME_TIME_OUT_OF_RANGE);
        }

    }

    private void validateUpdateHeadCount(MatchFormat finalMatchFormat, int finalHeadCount) {

        if (finalHeadCount < participantCount) {
            throw new CustomException(INVALID_HEADCOUNT);
        }

        finalMatchFormat.validateHeadCount(finalHeadCount);

    }

    private void validateMatchConditionChange(MatchFormat matchFormat, MatchGenderType matchGenderType) {

        if (!existsOtherParticipants()) {
            return;
        }

        boolean matchFormatChanged = matchFormat != null && matchFormat != this.matchFormat;

        boolean matchGenderTypeChanged = matchGenderType != null && matchGenderType != this.matchGenderType;

        if (matchFormatChanged || matchGenderTypeChanged) {

            throw new CustomException(GAME_UPDATE_NOT_ALLOWED);
        }

    }

    private boolean existsOtherParticipants() {
        return participantCount > CREATOR_COUNT;
    }




    private void updateRecruitmentStatus(int headCount) {

        if (headCount <= participantCount) {
            this.gameStatus = GameStatus.CLOSED;
            return;
        }

        this.gameStatus = GameStatus.RECRUITING;

    }


    public void cancelByCreatorWithdrawal(LocalDateTime now) {
        if (deletedDateTime != null) {
            return;
        }

        this.deletedDateTime = now;
    }

    public void validateJoin(UserEntity participant, LocalDateTime now) {
        validateNotCreator(participant);
        validateRecruiting();
        validateJoinDeadline(now);
        validateApplicantGender(participant.getGenderType());


    }

    private void validateNotCreator(UserEntity applicant) {

        if (Objects.equals(userEntity.getUserId(), applicant.getUserId())) {
            throw new CustomException(NOT_APPLY_GAME_CREATOR);
        }
    }

    private void validateRecruiting() {
        if (gameStatus == GameStatus.CLOSED || participantCount >= headCount) {
            throw new CustomException(FULL_HEADCOUNT_GAME);
        }
    }

    private void validateJoinDeadline(LocalDateTime now) {
        LocalDateTime deadline = startDateTime.minusMinutes(30);

        if (!now.isBefore(deadline)) {
            throw new CustomException(NOT_ALLOWED_TO_JOIN);
        }
    }

    private void validateApplicantGender(GenderType genderType) {
        if (matchGenderType == MatchGenderType.FEMALE_ONLY && genderType == GenderType.MALE) {
            throw new CustomException(ONLY_FEMALE_GAME);
        }

        if (matchGenderType == MatchGenderType.MALE_ONLY && genderType == GenderType.FEMALE) {
            throw new CustomException(ONLY_MALE_GAME);
        }
    }




    public void increaseParticipantCount() {
        this.participantCount++;

        updateRecruitmentStatus(headCount);
    }



    public void decreaseParticipantCount() {
        this.participantCount--;

        updateRecruitmentStatus(headCount);
    }






}
