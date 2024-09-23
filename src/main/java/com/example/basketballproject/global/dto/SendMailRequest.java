package com.example.basketballproject.global.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMailRequest {



    @NotBlank(message = "이메일을 입력해주새요.")
    @Email(message = "이메일 형식에 맞게 입력해주세요.")
    private String email;


}
