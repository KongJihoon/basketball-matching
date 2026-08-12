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


    private LocalDateTime acceptDateTime;

    private LocalDateTime canceledDateTime;

    private LocalDateTime kickoutDateTime;

    private LocalDateTime deletedDateTime;



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private GameEntity gameEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private UserEntity userEntity;

    @Builder(access = AccessLevel.PRIVATE)
    private ParticipantGameEntity(GameEntity game, UserEntity applicant) {
        this.gameEntity = game;
        this.userEntity = applicant;
    }


    public static ParticipantGameEntity createParticipation(GameEntity gameEntity, UserEntity userEntity, LocalDateTime appliedAt) {

        ParticipantGameEntity participation = ParticipantGameEntity.builder()
                .game(gameEntity)
                .applicant(userEntity)
                .build();


        participation.transitionTo(ACCEPT, appliedAt);

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

    public void join(LocalDateTime joinedAt) {
        switch (participantGameStatus) {
            case CANCEL-> {
                transitionTo(ACCEPT, joinedAt);

                this.canceledDateTime = null;
            }

            case ACCEPT -> throw new CustomException(ALREADY_ACCEPT_USER);

            case KICKOUT -> throw new CustomException(ALREADY_KICKOUT_USER);

            case DELETE -> throw new CustomException(ALREADY_FINAL_STATUS);

        }
    }

    public void cancelParticipation(LocalDateTime canceledAt) {

        switch (participantGameStatus) {
            case ACCEPT -> transitionTo(CANCEL, canceledAt);
            case CANCEL -> throw new CustomException(ALREADY_CANCELED_USER);
            case KICKOUT -> throw new CustomException(ALREADY_KICKOUT_USER);
            case DELETE -> throw new CustomException(ALREADY_FINAL_STATUS);
        }

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
            case ACCEPT -> {
                if (newStatus != CANCEL && newStatus != KICKOUT && newStatus != DELETE) {
                    throw new CustomException(INVALID_STATUS_TRANSITION);
                }
            }
            case CANCEL -> {
                if (newStatus != ACCEPT && newStatus != DELETE) {
                    throw new CustomException(INVALID_STATUS_TRANSITION);
                }
            }
            case KICKOUT, DELETE ->
                    throw new CustomException(ALREADY_FINAL_STATUS);



        }

    }

    private void applyTimestamp(ParticipantGameStatus status, LocalDateTime now) {
        switch (status) {
            case ACCEPT -> acceptDateTime = now;
            case CANCEL -> canceledDateTime = now;
            case KICKOUT -> kickoutDateTime = now;
            case DELETE -> deletedDateTime = now;
        }
    }



}

