package com.example.basketballproject.gameUser.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QParticipantEntity is a Querydsl query type for ParticipantEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QParticipantEntity extends EntityPathBase<ParticipantEntity> {

    private static final long serialVersionUID = -1846991023L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QParticipantEntity participantEntity = new QParticipantEntity("participantEntity");

    public final DateTimePath<java.time.LocalDateTime> acceptDateTime = createDateTime("acceptDateTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdDateTime = createDateTime("createdDateTime", java.time.LocalDateTime.class);

    public final com.example.basketballproject.gameCreator.entity.QGameEntity gameEntity;

    public final NumberPath<Long> participantId = createNumber("participantId", Long.class);

    public final EnumPath<com.example.basketballproject.gameUser.type.ParticipantStatus> status = createEnum("status", com.example.basketballproject.gameUser.type.ParticipantStatus.class);

    public final com.example.basketballproject.user.entity.QUserEntity userEntity;

    public QParticipantEntity(String variable) {
        this(ParticipantEntity.class, forVariable(variable), INITS);
    }

    public QParticipantEntity(Path<? extends ParticipantEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QParticipantEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QParticipantEntity(PathMetadata metadata, PathInits inits) {
        this(ParticipantEntity.class, metadata, inits);
    }

    public QParticipantEntity(Class<? extends ParticipantEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.gameEntity = inits.isInitialized("gameEntity") ? new com.example.basketballproject.gameCreator.entity.QGameEntity(forProperty("gameEntity"), inits.get("gameEntity")) : null;
        this.userEntity = inits.isInitialized("userEntity") ? new com.example.basketballproject.user.entity.QUserEntity(forProperty("userEntity")) : null;
    }

}

