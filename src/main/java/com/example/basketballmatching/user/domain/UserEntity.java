package com.example.basketballmatching.user.domain;

import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.LoginProvider;
import com.example.basketballmatching.user.type.Position;
import com.example.basketballmatching.user.type.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "zip_code", length = 5)
    private String zipCode;

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

    private boolean emailAuth;

    private LocalDateTime deletedDateTime;

    @Builder(access = AccessLevel.PRIVATE)
    private UserEntity(
            String email,
            String password,
            String nickname,
            String name,
            LocalDate birth,
            String phone,
            String address,
            String addressDetail,
            String zipCode,
            Position position,
            GenderType genderType,
            LoginProvider loginProvider,
            boolean emailAuth,
            UserType userType
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.birth = birth;
        this.phone = phone;
        this.address = address;
        this.addressDetail = addressDetail == null || addressDetail.isBlank() ? null : addressDetail.strip();
        this.zipCode = zipCode;
        this.position = position;
        this.genderType = genderType;
        this.loginProvider = loginProvider;
        this.emailAuth = emailAuth;
        this.userType = UserType.USER;
    }


    public static UserEntity create(
            String email, String password, String nickname, String name,
            LocalDate birth, String phone, String address,
            String addressDetail, String zipCode,
            Position position, GenderType genderType
    ) {
        return UserEntity.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .name(name)
                .birth(birth)
                .phone(phone)
                .address(address)
                .addressDetail(addressDetail)
                .zipCode(zipCode)
                .position(position)
                .genderType(genderType)
                .loginProvider(LoginProvider.LOCAL)
                .build();
    }

    public static UserEntity createOAuth(
            String email, String nickname, String name,
            LocalDate birth, String phone, String address,
            String addressDetail, String zipCode,
            Position position, GenderType genderType, LoginProvider loginProvider
    ) {
        return UserEntity.builder()
                .email(email)
                .nickname(nickname)
                .name(name)
                .birth(birth)
                .phone(phone)
                .address(address)
                .addressDetail(addressDetail)
                .zipCode(zipCode)
                .position(position)
                .genderType(genderType)
                .loginProvider(loginProvider)
                .emailAuth(true)
                .build();
    }

    public void verifiedEmail() {
        this.emailAuth = true;
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

    public void withdraw(LocalDateTime withdrawnAt) {
        if (deletedDateTime != null) {
            return;
        }

        this.deletedDateTime = withdrawnAt;
    }


}
