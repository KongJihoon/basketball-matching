package com.example.basketballmatching.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {


    // JWT (인증 실패 = 401 / 권한 부족  = 403)
    NOT_FOUND_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 존재하지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다. 갱신해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다."),

    // user (리소스 없음 = 404, 중복 = 409, 입력 검증 = 400)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ALREADY_EXIST_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    ALREADY_EXIST_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    ALREADY_VERIFIED_EMAIL(HttpStatus.CONFLICT, "이미 인증된 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증을 먼저 진행해주세요."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "새로운 비밀번호를 입력해주세요."),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증번호입니다."),
    LOGOUT_USER(HttpStatus.UNAUTHORIZED, "로그아웃 유저입니다"),
    PROVIDER_NOT_MATCH(HttpStatus.BAD_REQUEST, "로그인 방식이 일치하지 않습니다."),

    // validation (입력 검증 = 400)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INVALID_PATTERN(HttpStatus.BAD_REQUEST, "잘못된 패턴입니다."),
    PAST_BIRTHDAY(HttpStatus.BAD_REQUEST, "과거 날짜를 입력해주세요."),
    FUTURE_DATE(HttpStatus.BAD_REQUEST, "현재 시각 이후로 입력해주세요."),

    // OAuth2(입력 검증 = 400, 외부 연동 실패 = 502)
    OAUTH_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAuth 인가 코드가 존재하지 않습니다."),
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "OAuth 요청 상태가 유효하지 않습니다."),
    OAUTH_TICKET_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 OAuth Ticket입니다."),
    OAUTH_PROVIDER_ID_NOT_FOUND(HttpStatus.BAD_GATEWAY, "소셜 로그인 사용자 식별자를 확인할 수 없습니다."),
    OAUTH_ACCOUNT_LINK_REQUIRED(HttpStatus.CONFLICT, "동일한 이메일의 기존 계정이 있습니다. 기존 계정 로그인 후 소셜 계정을 연결해주세요."),
    OAUTH_WITHDRAWN_USER(HttpStatus.CONFLICT, "탈퇴한 계정입니다. 재가입 절차를 진행해주세요."),
    OAUTH_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 발급에 실패하였습니다."),
    OAUTH_USERINFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 요청에 실패하였습니다."),
    OAUTH_USERINFO_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 응답 파싱에 실패하였습니다."),
    OAUTH_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "카카오 계정의 이메일 제공 동의가 필요합니다."),


    // game (입력 검증 = 400/ 리소스 없음 = 404/ 권한 부족 =  403/ 중복 = 409)
    INVALID_GAME_TIME(HttpStatus.BAD_REQUEST, "시작/종료 시간이 유효하지 않습니다."),
    GAME_TIME_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "경기시간은 1시간 이상 2시간 이하입니다."),
    PLACE_SCHEDULE_OVERLAP(HttpStatus.CONFLICT, "해당 장소에 겹치는 경기가 존재합니다."),
    INVALID_HEADCOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 인원수입니다."),
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "경기를 찾을 수 없습니다."),
    NOT_GAME_CREATOR(HttpStatus.FORBIDDEN, "경기 생성자가 아닙니다."),
    UPDATE_GAME_HEAD_COUNT(HttpStatus.BAD_REQUEST, "경기 인원 수를 변경해주세요."),
    LOCK_BY_GAME(HttpStatus.BAD_REQUEST, "잠시 후에 다시 시도해주세요."),

    // participant (입력 검증 = 400/ 리소스 없음 = 404/ 권한 부족 =  403/ 중복 = 409)
    ALREADY_APPLY_GAME_USER(HttpStatus.CONFLICT, "이미 참가 신청한 유저입니다."),
    FULL_HEADCOUNT_GAME(HttpStatus.CONFLICT, "남은 신청 자리가 없습니다."),
    NOT_ALLOWED_TO_JOIN(HttpStatus.BAD_REQUEST, "경기 참가를 요청할 수 없습니다."),
    NOT_ALLOWED_CANCEL(HttpStatus.FORBIDDEN, "경기 취소를 요청할 수 없습니다."),
    ONLY_FEMALE_GAME(HttpStatus.FORBIDDEN, "여성 유저만 신청가능합니다."),
    ONLY_MALE_GAME(HttpStatus.FORBIDDEN, "남성 유저만 신청가능합니다."),
    NOT_APPLY_USER(HttpStatus.BAD_REQUEST, "경기 참가 신청을 한 유저가 아닙니다."),
    NOT_APPLY_KICKOUT_USER(HttpStatus.BAD_REQUEST, "강퇴당한 유저는 신청 불가입니다."),
    ALREADY_START_GAME(HttpStatus.CONFLICT, "이미 시작한 경기입니다."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "경기 참가자를 찾을 수 없습니다."),
    ALREADY_ACCEPT_USER(HttpStatus.CONFLICT, "이미 수락된 참가자입니다."),
    NOT_REJECT_CREATOR(HttpStatus.BAD_REQUEST, "경기 생성자는 거절할 수 없습니다."),
    ALREADY_REJECT_USER(HttpStatus.CONFLICT, "이미 거절된 참가자입니다."),
    NOT_ACCEPT_USER(HttpStatus.BAD_REQUEST, "수락된 참가자가 아닙니다."),
    ALREADY_CANCELED_USER(HttpStatus.CONFLICT, "이미 취소한 참가자입니다."),
    NOT_KICKOUT_CREATOR(HttpStatus.BAD_REQUEST, "경기 생성자는 강퇴할 수 없습니다."),
    ALREADY_KICKOUT_USER(HttpStatus.CONFLICT, "이미 강퇴한 참가자입니다."),
    NOT_DELETE_GAME(HttpStatus.FORBIDDEN, "경기를 삭제할 수 없습니다."),
    NOT_APPLY_GAME_CREATOR(HttpStatus.BAD_REQUEST, "경기 생성자는 참가신청을 할 수 없습니다."),
    CLOSED_GAME(HttpStatus.BAD_REQUEST, "모집 마감된 경기입니다."),
    ALREADY_PRECESSED_STATUS(HttpStatus.BAD_REQUEST, "이미 변경된 상태입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태변경입니다."),
    ALREADY_FINAL_STATUS(HttpStatus.BAD_REQUEST, "이미 적용된 상태변경입니다."),
    INVALID_PARTICIPANT_GAME_STATUS(HttpStatus.BAD_REQUEST, "탈퇴 처리 대상이 아닌 참가 상태입니다."),

    // level
    NOT_GAME_ENDED(HttpStatus.CONFLICT, "아직 경기가 종료되지 않았습니다."),
    CANNOT_EVALUATE_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 평가할 수 없습니다."),
    ALREADY_EVALUATED(HttpStatus.CONFLICT, "이미 평가한 참가자입니다."),
    INVALID_LEVEL_SCORE(HttpStatus.BAD_REQUEST, "평가점수는 1 ~ 5점만 입력가능합니다."),
    ONLY_EVALUATE_ACCEPT_USER(HttpStatus.FORBIDDEN, "경기 수락자만 평가가 가능합니다."),

    // report
    ALREADY_REPORTED_USER(HttpStatus.CONFLICT, "이미 신고받은 유저입니다."),
    NOT_FOUND_REPORT(HttpStatus.NOT_FOUND, "신고내역을 찾을 수 없습니다."),
    ALREADY_CHECK_REPORT(HttpStatus.CONFLICT, "이미 처리된 신고내역입니다."),


    // blackList
    ALREADY_BLACK_USER(HttpStatus.CONFLICT, "이미 블랙리스트 등록된 유저입니다."),
    BLACKLIST_USER(HttpStatus.FORBIDDEN, "블랙리스트 유저입니다."),


    // friend
    ALREADY_EXIST_FRIEND(HttpStatus.BAD_REQUEST, "이미 친구목록에 존재하는 유저입니다."),
    INVALID_FRIEND_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 친구요청입니다.");



    private final HttpStatus httpStatus;
    private final String errorMessage;

    public int getStatusCode() {
        return httpStatus.value();
    }
}
