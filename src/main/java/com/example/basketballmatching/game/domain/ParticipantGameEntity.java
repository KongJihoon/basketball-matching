package com.example.basketballmatching.game.domain;


import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.game.type.ParticipantGameStatus;
import com.example.basketballmatching.global.exception.CustomException;
import com.example.basketballmatching.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.example.basketballmatching.game.type.ParticipantGameStatus.*;
import static com.example.basketballmatching.global.exception.ErrorCode.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ParticipantGameEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantGameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantGameStatus participantGameStatus;

    private LocalDateTime applyDateTime;

    private LocalDateTime acceptDateTime;

    private LocalDateTime rejectDateTime;

    private LocalDateTime canceledDateTime;

    private LocalDateTime kickoutDateTime;

    private LocalDateTime deletedDateTime;



    @ManyToOne
    @JoinColumn(nullable = false)
    private GameEntity gameEntity;

    @ManyToOne
    @JoinColumn(nullable = false)
    private UserEntity userEntity;

    @Builder(access = AccessLevel.PRIVATE)
    private ParticipantGameEntity(GameEntity game, UserEntity applicant) {
        this.gameEntity = game;
        this.userEntity = applicant;
    }


    public static ParticipantGameEntity createApply(GameEntity gameEntity, UserEntity userEntity, LocalDateTime appliedAt) {

        ParticipantGameEntity participation = ParticipantGameEntity.builder()
                .game(gameEntity)
                .applicant(userEntity)
                .build();


        participation.transitionTo(APPLY, appliedAt);

        return participation;


    }

    public static ParticipantGameEntity createCreator(
            GameEntity gameEntity, UserEntity userEntity, LocalDateTime createdAt
    ) {

        ParticipantGameEntity participantGameEntity = ParticipantGameEntity.builder()
                .game(gameEntity)
                .applicant(userEntity)
                .build();

        participantGameEntity.transitionTo(ACCEPT, createdAt);
        return participantGameEntity;
    }

    public void reapply(LocalDateTime appliedAt) {
        transitionTo(APPLY, appliedAt);

        this.canceledDateTime = null;

    }

    public void accept(LocalDateTime now) {

        transitionTo(ACCEPT, now);
    }

    public void cancel(LocalDateTime now) {
        transitionTo(CANCEL, now);
    }

    public void reject(LocalDateTime now) {

        transitionTo(REJECT, now);
    }

    public void kickout(LocalDateTime now) {

        transitionTo(KICKOUT, now);
    }

    public void delete(LocalDateTime now) {
        transitionTo(DELETE, now);
    }




    private void transitionTo(ParticipantGameStatus newStatus, LocalDateTime now) {

        ParticipantGameStatus oldStatus = participantGameStatus;

        if (oldStatus != null) {
            validateTransition(oldStatus, newStatus);
        }

        boolean wasOccupied = isOccupied(oldStatus);
        boolean willOccupied = isOccupied(newStatus);

        if (!wasOccupied && willOccupied) {
            gameEntity.increaseParticipantCount();
        }

        if (wasOccupied && !willOccupied) {
            gameEntity.decreaseParticipantCount();
        }

        participantGameStatus = newStatus;

        applyTimestamp(newStatus, now);

    }


    private boolean isOccupied(ParticipantGameStatus status) {
        return status == ACCEPT;
    }

    private void validateTransition(ParticipantGameStatus oldStatue, ParticipantGameStatus newStatus) {

        if (oldStatue == newStatus) {
            throw new CustomException(ALREADY_PRECESSED_STATUS);
        }

        switch (oldStatue) {
            case APPLY -> {
                if (newStatus != ACCEPT && newStatus != REJECT && newStatus != CANCEL && newStatus != DELETE) {
                    throw new CustomException(INVALID_STATUS_TRANSITION);
                }
            }
            case ACCEPT -> {
                if (newStatus != CANCEL && newStatus != KICKOUT && newStatus != DELETE) {
                    throw new CustomException(INVALID_STATUS_TRANSITION);
                }
            }
            case CANCEL -> {
                if (newStatus != APPLY && newStatus != DELETE) {
                    throw new CustomException(INVALID_STATUS_TRANSITION);
                }
            }
            case REJECT, KICKOUT, DELETE -> {
                throw new CustomException(ALREADY_FINAL_STATUS);
            }
        }

    }

    private void applyTimestamp(ParticipantGameStatus status, LocalDateTime now) {
        switch (status) {
            case APPLY -> applyDateTime = now;
            case ACCEPT -> acceptDateTime = now;
            case REJECT -> rejectDateTime = now;
            case CANCEL -> canceledDateTime = now;
            case KICKOUT -> kickoutDateTime = now;
            case DELETE -> deletedDateTime = now;
        }
    }



}

