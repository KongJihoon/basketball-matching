package com.example.basketballproject.global.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST.value(), "유효하지 않은 입력 값입니다."),
    INVALID_PATTERN(HttpStatus.BAD_REQUEST.value(), "형식에 맞게 입력 해야합니다."),


    // user
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), "사용자를 찾을 수 없습니다.")
    ,PRECEED_SIGNUP(HttpStatus.BAD_REQUEST.value(), "회원가입을 먼저 진행해주세요."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "존재하지 않는 이메일입니다"),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST.value(), "유효하지 않은 이메일 인증 코드입니다."),
    ALREADY_EXIST_USER(HttpStatus.CONFLICT.value(), "이미 존재하는 회원입니다."),
    ALREADY_EXIST_NICKNAME(HttpStatus.BAD_REQUEST.value(), "이미 사용중인 닉네임입니다."),
    ALREADY_EXIST_LOGINID(HttpStatus.BAD_REQUEST.value(), "이미 사용중인 아이디 입니다."),



    //server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "내부 서버 오류가 발생했습니다.");

    private final int statusCode;
    private final String description;
}
