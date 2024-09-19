package com.example.basketballproject.auth.dto;


import com.example.basketballproject.user.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @Email
    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;

    @NotNull(message = "생년월일을 입력해주세요.")
    private LocalDate birth;

    private String userType;

    @NotBlank(message = "성별을 입력해주세요.")
    private String genderType;

    @NotBlank(message = "포지션을 입력해주세요.")
    private String position;

    private LocalDateTime createdDateTime;


    public static SignUpDto toEntity(UserEntity userEntity) {

        return SignUpDto.builder()
                .loginId(userEntity.getLoginId())
                .password(userEntity.getPassword())
                .email(userEntity.getEmail())
                .name(userEntity.getName())
                .nickname(userEntity.getNickname())
                .birth(userEntity.getBirth())
                .userType(String.valueOf(userEntity.getUserType()))
                .genderType(String.valueOf(userEntity.getGenderType()))
                .position(String.valueOf(userEntity.getPosition()))
                .createdDateTime(userEntity.getCreatedDateTime())
                .build();


    }

}
