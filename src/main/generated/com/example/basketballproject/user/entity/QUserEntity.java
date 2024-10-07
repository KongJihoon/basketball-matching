package com.example.basketballproject.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserEntity is a Querydsl query type for UserEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserEntity extends EntityPathBase<UserEntity> {

    private static final long serialVersionUID = 932562049L;

    public static final QUserEntity userEntity = new QUserEntity("userEntity");

    public final DatePath<java.time.LocalDate> birth = createDate("birth", java.time.LocalDate.class);

    public final DateTimePath<java.time.LocalDateTime> createdDateTime = createDateTime("createdDateTime", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> deletedDateTime = createDateTime("deletedDateTime", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final BooleanPath emailAuth = createBoolean("emailAuth");

    public final EnumPath<com.example.basketballproject.user.type.GenderType> genderType = createEnum("genderType", com.example.basketballproject.user.type.GenderType.class);

    public final StringPath loginId = createString("loginId");

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final EnumPath<com.example.basketballproject.user.type.Position> position = createEnum("position", com.example.basketballproject.user.type.Position.class);

    public final DateTimePath<java.time.LocalDateTime> updatedDateTime = createDateTime("updatedDateTime", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final EnumPath<com.example.basketballproject.user.type.UserType> userType = createEnum("userType", com.example.basketballproject.user.type.UserType.class);

    public QUserEntity(String variable) {
        super(UserEntity.class, forVariable(variable));
    }

    public QUserEntity(Path<? extends UserEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserEntity(PathMetadata metadata) {
        super(UserEntity.class, metadata);
    }

}

