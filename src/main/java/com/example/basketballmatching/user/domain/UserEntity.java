package com.example.basketballmatching.user.domain;

import com.example.basketballmatching.gameUsers.type.GameUserLevel;
import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Position position;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GenderType genderType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoginProvider loginProvider;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private GameUserLevel gameUserLevel = GameUserLevel.NONE;

    @Builder.Default
    private boolean emailAuth = false;

    private LocalDateTime deletedDateTime;



    public void setEmailAuth() {
        this.emailAuth = true;
    }

    public void setDeletedDateTime(LocalDateTime deletedDateTime) {
        this.deletedDateTime = deletedDateTime;
    }

    public void editUserInfo(String nickname, String phone, String address, GenderType genderType, Position position) {

        if (nickname != null) {
            this.nickname = nickname;
        }

        if (phone != null) {
            this.phone = phone;
        }

        if (address != null) {
            this.address = address;
        }

        if (position != null) {
            this.position = position;
        }

        if (genderType != null) {
            this.genderType = genderType;
        }

    }

    public void setPassword(String password) {

        this.password = password;

    }

    public static UserEntity create(String email, String password, String nickname, String name, LocalDate birth, String phone, String address, Position position, GenderType genderType) {

        return UserEntity.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .birth(birth)
                .phone(phone)
                .address(address)
                .position(position)
                .userType(UserType.USER)
                .genderType(genderType)
                .loginProvider(LoginProvider.LOCAL)
                .build();

    }

    public void withdraw(LocalDateTime withdrawnAt) {
        if (deletedDateTime != null) {
            return;
        }

        this.deletedDateTime = withdrawnAt;
    }


    public void updateLevel(GameUserLevel gameUserLevel) {
        this.gameUserLevel = gameUserLevel;
    }
}
