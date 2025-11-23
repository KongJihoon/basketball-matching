package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class EditUserDto {


    private String nickname;


    @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String phone;

    private String address;

    private GenderType genderType;

    private Position position;
}
