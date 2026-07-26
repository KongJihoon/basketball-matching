package com.example.basketballmatching.user.dto;

import com.example.basketballmatching.user.type.GenderType;
import com.example.basketballmatching.user.type.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SignUpRequest(

        @Schema(
                description = "이메일",
                example = "test@test.com"
        )
        @NotBlank(
                message = "이메일은 필수 입력값입니다."
        )
        @Email(
                message = "이메일 형식이 올바르지 않습니다."
        )
        String email,

        @Schema(
                description = "비밀번호",
                example = "Test@1234"
        )
        @NotBlank(
                message = "비밀번호를 입력해주세요."
        )
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)"
                        + "(?=.*[~!@#$%^&*()])"
                        + "[a-zA-Z\\d~!@#$%^&*()]{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다."
        )
        String password,

        @Schema(
                description = "비밀번호 확인",
                example = "Test@1234"
        )
        @NotBlank(
                message = "비밀번호 확인을 입력해주세요."
        )
        String checkPassword,

        @Schema(
                description = "닉네임",
                example = "커리"
        )
        @NotBlank(
                message = "닉네임을 입력해주세요."
        )
        @Size(
                min = 2,
                max = 12,
                message = "닉네임은 2~12자여야 합니다."
        )
        String nickname,

        @Schema(
                description = "이름",
                example = "서장훈"
        )
        @NotBlank(
                message = "이름을 입력해주세요."
        )
        String name,

        @Schema(
                description = "생년월일",
                example = "1997-01-01"
        )
        @NotNull(
                message = "생년월일을 입력해주세요."
        )
        LocalDate birth,

        @Schema(
                description = "휴대폰 번호",
                example = "010-1111-0000"
        )
        @NotBlank(
                message = "휴대폰 번호를 입력해주세요."
        )
        @Pattern(
                regexp = "^01[016789]-\\d{3,4}-\\d{4}$",
                message = "휴대폰 번호 형식이 올바르지 않습니다."
        )
        String phone,

        @Schema(
                description = "주소",
                example = "서울특별시 강남구"
        )
        @NotBlank(
                message = "주소를 입력해주세요."
        )
        String address,

        @Schema(
                description = "포지션",
                example = "GUARD"
        )
        @NotNull(
                message = "포지션을 입력해주세요."
        )
        Position position,

        @Schema(
                description = "성별",
                example = "MALE"
        )
        @NotNull(
                message = "성별을 입력해주세요."
        )
        GenderType genderType
) {
}