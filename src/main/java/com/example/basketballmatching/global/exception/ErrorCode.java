package com.example.basketballmatching.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // jwt
    // JWT
    NOT_FOUND_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 갱신해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

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
    LOGOUT_USER(HttpStatus.BAD_REQUEST, "로그아웃 유저입니다"),
    PROVIDER_NOT_MATCH(HttpStatus.BAD_REQUEST, "로그인 방식이 일치하지 않습니다."),
    // validation
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_PATTERN(HttpStatus.BAD_REQUEST, "잘못된 패턴입니다."),
    PAST_BIRTHDAY(HttpStatus.BAD_REQUEST, "과거 날짜를 입력해주세요."),
    FUTURE_DATE(HttpStatus.BAD_REQUEST, "현재 시각 이후로 입력해주세요."),

    // OAuth2
    OAUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH2 토큰이 존재하지 않습니다."),
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "요청 검증 상태가 유효하지 않습니다."),
    OAUTH_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 발급에 실패하였습니다."),
    OAUTH_USERINFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 요청에 실패하였습니다."),
    OAUTH_USERINFO_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 응답 파싱에 실패하였습니다."),

    // game

    INVALID_GAME_TIME(HttpStatus.BAD_REQUEST, "시작/종료 시간이 유효하지 않습니다."),
    GAME_TIME_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "경기시간은 1시간 이상 2시간 이하입니다."),
    PLACE_SCHEDULE_OVERLAP(HttpStatus.BAD_REQUEST, "해당 장소에 겹치는 경기가 존재합니다."),
    INVALID_HEADCOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 인원수입니다.")

    ;



    private final HttpStatus httpStatus;
    private final String errorMessage;

    public int getStatusCode() {
        return httpStatus.value();
    }
}
