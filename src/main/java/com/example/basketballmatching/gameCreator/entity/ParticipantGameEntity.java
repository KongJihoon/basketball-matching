package com.example.basketballmatching.gameCreator.entity;


import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.gameCreator.type.ParticipantGameStatus;
import com.example.basketballmatching.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class ParticipantGameEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantGameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantGameStatus participantGameStatus;

    private LocalDateTime acceptDateTime;

    private LocalDateTime rejectDateTime;

    private LocalDateTime canceledDateTime;

    private LocalDateTime withDrawDateTime;

    private LocalDateTime kickoutDateTime;

    private LocalDateTime deletedDateTime;



    @ManyToOne
    @JoinColumn(nullable = false)
    private GameEntity gameEntity;

    @ManyToOne
    @JoinColumn(nullable = false)
    private UserEntity userEntity;

    public ParticipantGameEntity toGameCreatorEntity(
            GameEntity gameEntity, UserEntity userEntity
    ) {

        return ParticipantGameEntity.builder()
                .participantGameStatus(ParticipantGameStatus.ACCEPT)
                .gameEntity(gameEntity)
                .userEntity(userEntity)
                .build();
    }

    public void setParticipantGameStatusAndAcceptDateTime(ParticipantGameStatus participantGameStatus, LocalDateTime acceptDateTime) {
        this.participantGameStatus = participantGameStatus;
        this.acceptDateTime = acceptDateTime;
    }


    public void setParticipantGameStatusAndRejectDateTime(ParticipantGameStatus participantGameStatus, LocalDateTime rejectDateTime) {
        this.participantGameStatus = participantGameStatus;
        this.rejectDateTime = rejectDateTime;
    }

    public void setParticipantGameStatusAndCanceledDateTime(ParticipantGameStatus participantGameStatus, LocalDateTime canceledDateTime) {
        this.participantGameStatus = participantGameStatus;
        this.canceledDateTime = canceledDateTime;
    }

    public void setParticipantGameStatusAndKickoutDateTime(ParticipantGameStatus participantGameStatus, LocalDateTime kickoutDateTime) {
        this.participantGameStatus = participantGameStatus;
        this.kickoutDateTime = kickoutDateTime;
    }

    public void setParticipantGameStatusAndDeletedDateTime(ParticipantGameStatus participantGameStatus, LocalDateTime deletedDateTime) {
        this.participantGameStatus = participantGameStatus;
        this.deletedDateTime = deletedDateTime;
    }

    public void setBlackUserStatus(ParticipantGameStatus participantGameStatus, LocalDateTime localDateTime) {

        if (participantGameStatus.equals(ParticipantGameStatus.ACCEPT)) {
            this.participantGameStatus = ParticipantGameStatus.KICKOUT;
            this.kickoutDateTime = localDateTime;
        }

        if (participantGameStatus.equals(ParticipantGameStatus.APPLY)) {
            this.participantGameStatus = ParticipantGameStatus.CANCEL;
            this.canceledDateTime = localDateTime;
        }

    }


}
