package com.example.basketballproject.user.entity;


import com.example.basketballproject.user.dto.EditDto;
import com.example.basketballproject.user.type.GenderType;
import com.example.basketballproject.user.type.Position;
import com.example.basketballproject.user.type.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GenderType genderType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Position position;

    @CreatedDate
    private LocalDateTime createdDateTime;

    @LastModifiedDate
    private LocalDateTime updatedDateTime;

    private LocalDateTime deletedDateTime;

    @Builder.Default
    private boolean emailAuth = false;

    public void changeEmailAuth() {
        this.emailAuth = true;
    }

    public void editInfo(EditDto.Request request) {

        if (this.name != null) {
            this.name = request.getName();
        }

        if (this.nickname != null) {
            this.nickname = request.getNickname();
        }

        if (this.genderType != null) {
            this.genderType = GenderType.valueOf(request.getGender());
        }

        if (this.position != null) {
            this.position = Position.valueOf(request.getPosition());
        }


    }

    public void passwordEdit(String password) {
        if (!password.isEmpty()) {
            this.password = password;
        }
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userType.getDescription()));
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
