package com.example.basketballmatching.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다."),

    // user
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "사용자를 찾을 수 없습니다."),
    ALREADY_EXIST_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    ALREADY_EXIST_NICKNAME(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    ALREADY_VERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "이미 인증된 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증을 먼저 진행해주세요."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증번호입니다."),
    // validation
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_PATTERN(HttpStatus.BAD_REQUEST, "잘못된 패턴입니다."),
    PAST_BIRTHDAY(HttpStatus.BAD_REQUEST, "과거 날짜를 입력해주세요.");



    private final HttpStatus httpStatus;
    private final String errorMessage;

    public int getStatusCode() {
        return httpStatus.value();
    }
}
