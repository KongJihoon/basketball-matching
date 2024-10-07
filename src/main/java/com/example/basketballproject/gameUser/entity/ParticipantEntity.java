package com.example.basketballproject.gameUser.entity;


import com.example.basketballproject.gameCreator.entity.GameEntity;
import com.example.basketballproject.gameUser.type.ParticipantStatus;
import com.example.basketballproject.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long participantId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdDateTime;

    private LocalDateTime acceptDateTime;

    @ManyToOne
    private GameEntity gameEntity;

    @ManyToOne
    private UserEntity userEntity;
}
