package com.example.basketballmatching.global.security;

import com.example.basketballmatching.user.domain.UserEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class UserInfoDetails implements UserDetails {

    private final UserEntity userEntity;


    /**
     * 프로젝트의 사용자 권한을 Spring Security에 GrantedAuthority로 변환한다.
     *
     * USER -> ROLE_USER
     * ADMIN -> ROLE_ADMIN
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.userEntity.getUserType().getDescription()));
    }


    @Override
    public String getPassword() {
        return this.userEntity.getPassword();
    }
    // Spring Security에서 사용자를 식별하는 username으로 이메일을 사용한다.
    @Override
    public String getUsername() {
        return this.userEntity.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 이메일 인증을 완료한 사용자만 활성 계정으로 표현한다.
    @Override
    public boolean isEnabled() {
        return this.userEntity.isEmailAuth();
    }
}
