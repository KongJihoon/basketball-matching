package com.example.basketballmatching.user.entity;

import com.example.basketballmatching.global.entity.BaseEntity;
import com.example.basketballmatching.user.dto.EditUserDto;
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

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
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
    private boolean emailAuth = false;

    public void setEmailAuth() {
        this.emailAuth = true;
    }


    public void editUserInfo(EditUserDto editUserDto) {

        if (editUserDto.getNickname() != null) {
            this.nickname = editUserDto.getNickname();
        }

        if (editUserDto.getPhone() != null) {
            this.phone = editUserDto.getPhone();
        }

        if (editUserDto.getAddress() != null) {
            this.address = editUserDto.getAddress();
        }

        if (editUserDto.getPosition() != null) {
            this.position = editUserDto.getPosition();
        }

        if (editUserDto.getGenderType() != null) {
            this.genderType = editUserDto.getGenderType();
        }

    }

    public void setPassword(String password) {

        this.password = password;

    }




}
