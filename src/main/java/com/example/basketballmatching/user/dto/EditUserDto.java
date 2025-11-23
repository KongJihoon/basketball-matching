package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import io.swagger.v3.oas.annotations.media.Schema;
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


    @Schema(name = "닉네임", example = "커리", defaultValue = "커리")
    private String nickname;

    @Schema(description = "휴대폰 번호", example = "010-1111-0000")
    @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String phone;

    @Schema(description = "주소", example = "서울특별시 강남구")
    private String address;

    @Schema(description = "성별", example = "MALE", defaultValue = "MALE")
    private GenderType genderType;

    @Schema(description = "포지션", example = "NONE", defaultValue = "NONE")
    private Position position;
}
