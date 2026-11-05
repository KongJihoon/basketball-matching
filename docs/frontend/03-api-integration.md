# API 연동 명세서

작성일: 2026-11-04 · 상태: 소스 기준 계약 초안 v1

## 1. 기준과 표기

기준은 현재 Controller·요청/응답 DTO·Service·SecurityConfig다. 라이브 서버 응답이나 OpenAPI를 추출한 결과는 아니다. 구현 시작 시 `/v3/api-docs`와 실제 응답을 대조하고 이 문서의 차이를 갱신한다.

아래 URI는 모두 `/api/v1` 뒤의 경로다. `C<T>`는 CommonResponse, `K`는 CheckResponse, `P<T>`는 Spring Page이며 숫자는 정상 HTTP 상태다. USER/ADMIN은 서버 메서드 권한 기준이고 소유권·참가자 관계는 별도 비즈니스 검증이다.

## 2. 전송·응답 공통 계약

```ts
type CommonResponse<T> = { message: string; data: T };
type CheckResponse = { success: boolean; message: string };
type ApiError = {
  statusCode: number;
  errorCode: string;
  errorMessage: string;
  details: Array<{ field: string; code: string; message: string }> | null;
};
type PageData<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}; // 프론트가 사용할 필드의 모델. 런타임 직렬화 구조는 OpenAPI/실응답으로 검증.
```

- JSON 요청은 `Content-Type: application/json`, 보호 API는 `Authorization: Bearer <accessToken>`을 사용한다.
- 쿠키 기반 JWT 인증은 현재 구현되어 있지 않다. OAuth state 쿠키는 JWT 쿠키와 다르다.
- 무본문 액션은 불필요한 JSON body를 보내지 않는다. 오류는 `response.ok`와 본문을 함께 확인한다.
- `details`는 없는 값/null도 허용하도록 방어 처리하고, 비 JSON·네트워크 오류는 공통 fallback을 제공한다.
- 페이지는 0부터, 화면은 1부터 표시한다. 모든 목록에 `data.content`가 있는 것은 아니다.
- Long ID는 현재 프론트에서 number로 다루되 안전 정수 범위를 초과한 ID가 필요해지면 서버 문자열 직렬화를 먼저 도입한다.
- LocalDate는 `YYYY-MM-DD`, LocalDateTime은 `YYYY-MM-DDTHH:mm:ss`다. 서비스 기준 시각은 Asia/Seoul이며 임의로 `Z`를 붙이지 않는다.
- PATCH는 수정 가능한 변경 필드만 보낸다. null/빈 문자열/미전송을 같은 뜻으로 취급하지 않는다.

## 3. 현재 엔드포인트 38개

### 3.1 회원·이메일·비밀번호: 12개

| Method | URI | 권한 | 요청 | 정상 응답 |
| --- | --- | --- | --- | --- |
| POST | `/email-verifications` | 공개 | `{email}` | 202 K |
| POST | `/email-verifications/confirm` | 공개 | `{email,code}` | 200 K |
| POST | `/users/signup` | 공개 | SignUpRequest | 201 `C<SignUpResponse>` |
| GET | `/users/availability/email` | 공개 | query `email` | 200 K; 중복은 409 |
| GET | `/users/availability/nickname` | 공개 | query `nickname` | 200 K; 중복은 409 |
| GET | `/users/mypage` | USER·ADMIN | 없음 | 200 `C<UserProfileResponse>` |
| PATCH | `/users/mypage` | USER | UpdateUserRequest | 200 `C<UserProfileResponse>` |
| PATCH | `/users/mypage/password` | USER | `{currentPassword,newPassword,newCheckPassword}` | 200 K |
| DELETE | `/users/mypage` | USER | 없음, Bearer 필요 | 200 K |
| POST | `/password-resets/email-verifications` | 공개 | `{email}` | 202 K |
| POST | `/password-resets/email-verifications/confirm` | 공개 | `{email,code}` | 200 K |
| PATCH | `/password-resets` | 공개 | `{email,password,checkPassword}` | 200 K |

SignUpRequest: `email, password, checkPassword, nickname, name, birth, phone, address, position, genderType`. 모두 필수다.

SignUpResponse: `email, nickname, name, birth, phone, address, position, genderType, userType, createdAt`. JWT를 반환하지 않는다.

UpdateUserRequest: `nickname?, phone?, address?, genderType?, position?`. UserProfileResponse: `userId, email, nickname, name, birth, phone, address, position, genderType, userType, createdAt, updatedAt`.

입력 검증 규칙은 [화면·정책 명세](02-screens-and-policies.md)를 따른다.

### 3.2 인증·OAuth: 7개

| Method | URI | 권한 | 요청 | 정상 응답 |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | 공개 | `{email,password}` | 200 `C<AuthTokenResponse>` |
| POST | `/auth/token/refresh` | 공개 | `{refreshToken}` | 200 `C<AuthTokenResponse>` |
| POST | `/auth/logout` | USER | 없음, Bearer 필요 | 200 K |
| GET | `/auth/oauth2/kakao/authorization` | 공개 | 브라우저 이동 | 카카오 인가 페이지로 redirect·state 쿠키 |
| GET | `/auth/oauth2/kakao/callback` | 공개·state 검증 | query `code,state`, state 쿠키 | 200 `C<OAuthCallbackResponse>` |
| POST | `/auth/oauth2/token` | LOGIN 티켓 | `{ticket}` | 200 `C<AuthTokenResponse>` |
| POST | `/auth/oauth2/signup` | SIGNUP 티켓 | OAuthSignUpRequest | 201 `C<AuthTokenResponse>` |

AuthTokenResponse: `{accessToken, refreshToken, user:{userId,email,nickname,userType}}`.

OAuthCallbackResponse: `{flowType:"LOGIN"|"SIGNUP",ticket,email,nickname}`. LOGIN의 email/nickname은 null, SIGNUP에는 가입 기본 정보가 포함된다.

OAuthSignUpRequest: `ticket, nickname, name, birth, phone, address, position, genderType`. 이메일은 요청에서 임의로 지정하지 않고 티켓의 서버 정보를 사용한다.

### 3.3 경기·참가·내 경기: 11개

| Method | URI | 권한·관계 | 요청 | 정상 응답 |
| --- | --- | --- | --- | --- |
| GET | `/games` | 공개 | GameListCondition, page=0,size=10 | 200 `C<P<GameListResponse>>` |
| GET | `/games/{gameId}` | 공개 | 경로 gameId | 200 `C<GameDetailResponse>` |
| POST | `/games` | USER | CreateGameRequest | 201 `C<CreateGameResponse>` |
| PATCH | `/games/{gameId}` | USER·생성자 | UpdateGameRequest | 200 `C<GameDetailResponse>` |
| DELETE | `/games/{gameId}` | USER·생성자 | 없음 | 200 K |
| POST | `/games/{gameId}/participations` | USER | 없음 | 201 `C<GameParticipantResponse>` |
| PATCH | `/games/{gameId}/participations/me/cancel` | USER·본인 참가 | 없음 | 200 K |
| GET | `/games/{gameId}/participants` | USER·확정 참가자 | page=0,size=10 | 200 `C<P<GameParticipantListResponse>>` |
| PATCH | `/games/{gameId}/participants/{participantId}/kickout` | USER·생성자 | 없음 | 200 K |
| GET | `/mypage/games/upcoming` | USER | page=0,size=10 | 200 `C<P<MyUpcomingGameResponse>>` |
| GET | `/mypage/games/completed` | USER | page=0,size=10 | 200 `C<P<MyCompletedGameResponse>>` |

`participantId`에는 명단 응답의 **participationId**를 넣는다. 신고 요청의 targetUserId와 혼동하지 않는다. 201 응답의 Location은 생성 리소스 식별용이며 해당 Location을 조회할 GET API가 항상 존재하는 것은 아니다.

GameListCondition: `date?, keyword?, cityName?, matchFormat?, fieldStatus?, matchGenderType?, gameStatus?, sortType?`. date 생략 시 현재 이후 경기, 지정 시 해당 날짜의 시작 경기로 조회한다. sortType 생략은 START_TIME_ASC이며 `sort=...` 대신 `sortType`을 사용한다. 프론트 검색 UI에서 필터는 단일 enum 값으로 전송한다.

| 응답 모델 | 필드 |
| --- | --- |
| GameListResponse | gameId, title, placeName, address, startDateTime, headCount, participantCount, matchGenderType, matchFormat, gameStatus |
| GameDetailResponse / CreateGameResponse | 목록의 모든 필드 + content, fieldStatus, endDateTime, latitude, longitude, cityName, creatorId, creatorNickname |
| GameParticipantResponse | participationId, gameId, status, joinedAt |
| GameParticipantListResponse | participationId, userId, nickname, position, creator |
| MyUpcomingGameResponse | gameId, title, placeName, address, cityName, matchFormat, matchGenderType, startDateTime, endDateTime, headCount, participantCount, gameStatus, creator |
| MyCompletedGameResponse | MyUpcomingGameResponse에서 gameStatus 제외 |

CreateGameRequest는 `title,content,headCount,fieldStatus,matchFormat,matchGenderType,startDateTime,endDateTime,placeName,address`가 필수이고 latitude/longitude는 선택이다. UpdateGameRequest는 `title?,content?,headCount?,matchFormat?,matchGenderType?,startDateTime?,endDateTime?`만 허용한다.

```json
{
  "title": "잠실 저녁 3대3",
  "content": "가볍게 함께 농구할 분을 모집합니다.",
  "headCount": 8,
  "fieldStatus": "OUTDOOR",
  "matchFormat": "THREE_ON_THREE",
  "matchGenderType": "MIXED",
  "startDateTime": "2026-11-10T19:00:00",
  "endDateTime": "2026-11-10T21:00:00",
  "placeName": "잠실 농구장",
  "address": "서울특별시 송파구 올림픽로 25"
}
```

예시 날짜는 설명용이다. 실제 호출에는 현재보다 최소 24시간 이후 시각을 사용한다.

### 3.4 신고·관리자: 5개

| Method | URI | 권한 | 요청 | 정상 응답 |
| --- | --- | --- | --- | --- |
| POST | `/games/{gameId}/reports` | USER·종료 경기 확정 참가자 | `{targetUserId,reportType,content}` | 201 `C<CreateReportResponse>` |
| GET | `/admin/reports` | ADMIN | status=PENDING,page=0,size=20 | 200 `C<P<ReportListResponse>>` |
| PATCH | `/admin/reports/{reportId}` | ADMIN | `{decision,reason}` | 200 `C<ReviewReportResponse>` |
| POST | `/admin/blacklists` | ADMIN | `{reportId}` | 201 `C<CreateBlackListResponse>` |
| GET | `/admin/blacklists` | ADMIN | status=ACTIVE,page=0,size=20 | 200 `C<P<BlackListResponse>>` |

CreateReportResponse: `reportId,reportedAt`. ReviewReportResponse: `reportId,reportStatus,reviewedAt`.

ReportListResponse: `reportId,gameId,gameTitle,reporterId,reporterNickname,targetUserId,targetNickname,reportType,content,reportStatus,reportedAt,reviewedAt,reviewReason`. 검토 전 reviewedAt/reviewReason은 null일 수 있다.

CreateBlackListResponse: `blackListId,reportId,targetUserId,bannedAt,expiresAt,status`. BlackListResponse는 여기에 `email,nickname,bannedByUserId`가 추가된다. 신고 승인과 제재 생성은 별도 API다.

### 3.5 알림: 3개

| Method | URI | 권한 | 요청 | 정상 응답 |
| --- | --- | --- | --- | --- |
| GET | `/notifications/subscribe` | USER | Bearer, 선택 Last-Event-ID 헤더 | 200 text/event-stream |
| GET | `/notifications/unread` | USER | page=0,size=10, 최대100 | 200 `C<NotificationResponse[]>` |
| PATCH | `/notifications/{notificationId}/read` | USER·소유 알림 | 없음 | 200 `C<ReadNotificationResponse>` |

NotificationResponse: `notificationId,notificationType,content,createdAt`. ReadNotificationResponse: `notificationId,isRead,readDateTime`이며 실제 JSON 속성은 서버 직렬화 확인 대상이다. 같은 알림을 다시 읽어도 최초 읽음 시각을 유지한다.

## 4. enum과 표시

| 모델 | 전송 값 |
| --- | --- |
| MatchFormat | THREE_ON_THREE, FIVE_ON_FIVE |
| FieldStatus | INDOOR, OUTDOOR |
| MatchGenderType | MALE_ONLY, FEMALE_ONLY, MIXED |
| GameStatus | RECRUITING, CLOSED |
| ParticipantGameStatus | ACCEPT, CANCEL, KICKOUT, DELETE |
| GameSortType | START_TIME_ASC, LATEST |
| Position | NONE, GUARD, CENTER, FORWARD |
| GenderType | MALE, FEMALE, NONE |
| UserType | USER, ADMIN |
| LoginProvider | LOCAL, KAKAO (현재 프로필 응답에는 없음) |
| ReportType | POOR_SPORTSMANSHIP, NO_SHOW, ABUSIVE_LANGUAGE |
| ReportStatus / Decision | PENDING, APPROVED, REJECTED / APPROVE, REJECT |
| BlackListStatus | ACTIVE, EXPIRED |
| NotificationType | KICKED_OUT, ACCEPT_GAME, REJECT_GAME, DELETE_GAME, UPDATE_GAME, BLACKLISTED, REQUEST_FRIEND |

CityName: `SEOUL=서울특별시, GYEONGGI=경기도, INCHEON=인천광역시, GANGWON=강원특별자치도, DAEJEON=대전광역시, SEJONG=세종특별자치시, CHUNGNAM=충청남도, CHUNGBUK=충청북도, DAEGU=대구광역시, GYEONGBUK=경상북도, GYEONGNAM=경상남도, BUSAN=부산광역시, ULSAN=울산광역시, GWANGJU=광주광역시, JEONNAM=전라남도, JEONBUK=전북특별자치도, JEJU=제주특별자치도`.

enum 값은 서버 영문을 전송하고 화면에서 한국어 라벨을 매핑한다. 알 수 없는 enum은 원시 값을 액션 판단에 사용하지 말고 일반 표시/fallback으로 처리한다. enum 존재만으로 승인·친구 기능을 구현하지 않는다.

## 5. 인증 처리 기본안

초기 검증판은 Access/Refresh·인증 사용자를 메모리에 저장하고 새로고침 시 재로그인한다. 이는 서버 변경 없이 시작하는 기본안이며 자동 로그인 UX는 범위 밖이다. 운영에서 지속 로그인을 요구하면 HttpOnly Refresh 쿠키/BFF 계약을 별도 확정하고 CSRF·쿠키 정책을 함께 변경한다. 암묵적으로 localStorage에 장기 토큰을 저장하지 않는다.

`EXPIRED_TOKEN`을 받은 보호 요청은 현재 세션에서 공유하는 하나의 갱신 Promise를 기다린다. 갱신 성공 시 Access/Refresh/user를 함께 교체하고 원 요청을 최대 1회 재시도한다. 갱신 실패는 세션·사용자 캐시·SSE 정리 후 로그인 안내다. 공개 호출에는 만료 토큰을 자동 첨부하지 않는 것이 기본이다.

로그인/가입/Refresh 요청 자체, 티켓 만료, 비밀번호 불일치, 권한 부족, 업무 오류는 자동 갱신하지 않는다. 로그아웃·다른 계정 로그인 도중 과거 갱신 결과가 도착하면 세션 generation을 확인해 폐기한다. 서버가 요청을 이미 처리했는지 모르는 네트워크 오류에는 생성·참가·신고·제재를 자동 재전송하지 않는다.

현재 Refresh는 이메일당 하나이며 회전 시 기존 만료일을 유지한다. 다른 기기 로그인으로 Refresh가 무효화될 수 있다. 이미 유효한 Access도 즉시 무효화된다고 가정하지 않는다.

## 6. SSE 계약과 캐시 갱신

- 이벤트 `connect`: 연결 안내 문자열. JSON 알림으로 파싱하지 않는다.
- 이벤트 `notification`, 영속 알림: NotificationResponse. notificationId 기준 중복 제거 후 unread를 재조회한다.
- 같은 이벤트의 경기 생성 메시지: `{receiverUserId,gameId,title,message,createdAtEpochMs}`. 본인 경기 생성 완료 안내이며 DB 알림이 아니다.
- 현재 두 payload에 공통 discriminator는 없다. `notificationId` 또는 `gameId/receiverUserId` 구조 검증으로 구분하고 알 수 없는 payload는 무시·개발 로그 처리한다.
- 마지막 수신 SSE `id`를 기억하고 재접속에 Last-Event-ID로 보낸다. Notification ID와 SSE event ID는 다른 값이다.
- Fetch 기반 SSE 도구로 헤더를 전송한다. 재시도는 지수 backoff와 상한을 두고, 로그아웃/언마운트는 AbortController로 종료한다. 인증 만료는 갱신 후 새 토큰으로 연결을 다시 연다.
- 연결 30분 timeout·로컬 5분 캐시를 전제로 재접속 후 unread를 항상 재조회한다. 인스턴스 전환에 의한 이벤트 누락은 캐시만으로 보장할 수 없다.
- `UPDATE_GAME`, `DELETE_GAME`, `KICKED_OUT` 이벤트는 현재 gameId가 없으므로 관련 경기 캐시 그룹을 무효화한다. B-04 완료 뒤 대상 경기 단위로 좁힐 수 있다.
- 페이지가 받은 알림 한 건을 읽지 않은 전체 개수에 단순 가산하지 않는다. 제재 이벤트 수신/403은 접근 제한 안내로 전환하며 갱신 루프를 종료한다.

| 변경 | 무효화할 데이터 |
| --- | --- |
| 생성 | 경기 목록, 내 예정 경기 |
| 참가·취소 | 해당 상세, 내 참가 상태, 참가자, 내 경기, 경기 목록 |
| 수정·강퇴 | 해당 상세, 참가자, 내 참가 상태, 내 경기, 경기 목록 |
| 삭제 | 목록·내 경기, 상세는 삭제 상태/목록 이동 |
| 프로필 수정 | 프로필·인증 표시 사용자·본인 참가자 표시 |
| 신고 검토 | 관리자 신고 목록 |
| 제재 | 신고·제재 목록·영향받은 경기 데이터 |
| 읽음 | unread 현재 페이지 재조회 |

## 7. 주요 오류와 UI 대응

| 코드/상황 | 대응 |
| --- | --- |
| EXPIRED_TOKEN | 보호 요청에 한해 단일 갱신 |
| INVALID_TOKEN, LOGOUT_USER, 갱신 실패 | 세션 종료 안내 |
| BLACKLIST_USER | 접근 제한 안내, 재시도/갱신 중단 |
| ALREADY_EXIST_EMAIL/NICKNAME | 해당 필드 오류·수정 |
| INVALID_CODE, EMAIL_NOT_VERIFIED | 코드/인증 단계로 복귀·재요청 |
| FULL_HEADCOUNT_GAME, ALREADY_ACCEPT_USER | 최신 상태 재조회 후 안내 |
| NOT_ALLOWED_TO_JOIN/CANCEL, NOT_ALLOWED_KICKOUT | 시간·관계 제한 안내 |
| ONLY_MALE_GAME, ONLY_FEMALE_GAME | 성별 조건 안내 |
| NOT_APPLY_KICKOUT_USER | 재참가 불가 안내 |
| GAME_NOT_FOUND | 상세 없음·목록 복귀 |
| PLACE_SCHEDULE_OVERLAP | 일정/장소 충돌 안내 |
| GAME_SCHEDULE_TOO_SOON, GAME_TIME_OUT_OF_RANGE | 일정 필드 오류 |
| GAME_UPDATE_NOT_ALLOWED, UPDATE_NOT_ALLOWED_AT_THIS_TIME | 수정 제한·재조회 |
| PARTICIPANT_LIST_ACCESS_DENIED | 명단 접근 제한 |
| REPORT_PERIOD_EXPIRED, ALREADY_REPORTED_USER | 기한/중복 접수 안내 |
| REPORT_ALREADY_PROCESSED | 관리자 목록 갱신 |
| REPORT_NOT_APPROVED, BLACKLIST_REPORT_ALREADY_USED, ALREADY_BLACK_USER | 제재 불가 이유·목록 갱신 |
| OAUTH_STATE_INVALID, OAUTH_TICKET_INVALID | 카카오 인증 재시작 |
| OAUTH_ACCOUNT_LINK_REQUIRED | 기존 계정 로그인 안내, 미구현 연결 버튼 없음 |
| 429, 네트워크 오류, 비 JSON/알 수 없는 5xx | 대기/재시도 안내·입력값 보존 |

이 표는 서버 오류 전체 목록을 복제한 것이 아니다. 정의되지 않은 코드도 공통 errorMessage 또는 안전한 기본 메시지로 표시한다. 미인증/AccessDenied 및 enum/JSON 바인딩의 공통 포맷은 B-07에서 확인한다.

## 8. 백엔드 보완 작업

모두 **제안/미구현** 상태다. 구현 완료·계약 검증 전에는 사용 가능한 API로 호출하지 않는다.

| ID | 보완 | 선행 대상 | 완료 조건 |
| --- | --- | --- | --- |
| B-01 | 카카오 콜백의 프론트 복귀·가입 티켓 재시도 | S-08/09 | state 유지, LOGIN/SIGNUP 분기, 중복·만료·가입 검증 실패 처리 |
| B-02 | 내 참가 상태 조회 | S-02 참가 액션 | 미참가/확정/취소/강퇴 상태를 명시적으로 조회 가능 |
| B-03 | 내 진행 중 경기 누락 해소 | S-10 | 시작~종료 경계에 경기가 예정 또는 진행 중에 표시 |
| B-04 | 영속 알림 이동 대상 | 알림→상세 이동 | 기존 null 대상/삭제된 대상 처리 포함 |
| B-05 | 프로필의 로그인 방식·비밀번호 변경 가능 여부 | S-11 | LOCAL/KAKAO 분기 가능 |
| B-06 | ADMIN 공통 로그아웃 허용 정책 | 관리자 출시 | ADMIN 로그아웃 성공·토큰 정리 |
| B-07 | 인증·인가·입력 바인딩 오류 포맷 통일 | 전체 연동 | 누락 인증, 권한 부족, 잘못된 enum/JSON/query가 올바른 4xx |
| B-08 | NONE 성별의 제한 경기 참가 정책 | 참가 출시 | 서버에서 명시적 허용/차단과 테스트 일치 |
| B-09 | 생성/수정 문자열 길이·생년월일 제약 | 폼 통합 | 화면과 DTO/저장소 검증의 일치 |

B-02 제안: `GET /api/v1/games/{gameId}/participations/me`, USER 전용, 200 C<{participationId:number|null,status:ParticipantGameStatus|null}>. 미참가는 두 값 null, 없는 경기는 404로 구분한다. 생성자는 공개 상세 creatorId와 본인 ID로 구분한다. 권한·시각·성별은 프론트에서 사전 안내하되 변경 요청에서 서버가 재검증한다. 이 응답만으로 모든 행위의 성공을 보장하지 않는다.

B-01 기본 제안: 서버 콜백이 state를 검증한 뒤 고정·허용된 SPA 완료 URL의 fragment로 티켓과 flowType을 전달하고 React가 즉시 읽고 URL에서 지운다. JWT와 사용자 개인정보는 전달 URL에 넣지 않는다. 신규 회원 기본 정보가 필요하면 별도 티켓 정보 계약을 추가한다. 소비 전 입력 검증 실패 시 재입력을 허용할지, 재인가를 요구할지 서버 원자성·재사용 방지와 함께 확정한다. 현재 상태에서는 가입 실패 후 재인가가 필요하다.

B-03 기본 제안: upcoming 조건을 미삭제·ACCEPT·end>now로 확장하고 진행 중 여부는 시각으로 표시한다. completed는 end<=now를 유지한다. 정렬·탭 문구와 테스트도 함께 변경한다.

정확한 미확인 개수·신고 제재 완료 표시·알림 전체 이력은 초기 필수 보완에서 제외한다. 기능을 추가할 때 별도 계약을 만든다.

## 9. 환경과 계약 검증

개발 요청은 `/api/v1` 상대 경로 → Vite 프록시 → localhost:8080이다. 운영도 같은 출처 `/api` 전달을 기본으로 한다. 출처 분리 시 CORS·쿠키·SSE 헤더 계약을 다시 작성한다. 프론트 환경 변수에는 JWT secret·SMTP 비밀번호·카카오 client secret·DB/Redis 인증을 넣지 않는다.

첫 연동에서 200/201/202, K/C/Page/List/SSE 각각을 확인한다. enum 오류·음수 page·잘못된 JSON·토큰 미제공·USER의 ADMIN 호출·제재 사용자를 함께 검증한다. 확인된 `/v3/api-docs` 스냅샷을 기준으로 타입을 생성하거나 갱신하고, 제안 API와 현재 API를 혼합하지 않는다.
