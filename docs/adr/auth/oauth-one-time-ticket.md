# OAuth Callback에서 일회용 Ticket 사용

## 문제 상황

기존 카카오 Callback은 인증 성공 직후
서비스 `AccessToken`과 `RefreshToken`을 응답으로 반환한다.

React 프론트엔드와 OAuth 리다이렉트 흐름을 연결하면 JWT가 URL, 브라우저 기록, 리다이렉트 로그 또는 외부 분석 도구에 노출될
가능성을 고려해야한다.

신규 사용자에게는 추가정보 입력 과정도 필요하므로
Callback 시점에 서비스 회원가입과 JWT 발급을 완료할 수 없다.

---

## 선택지

### 선택지 1. Callback에서 JWT 직접 반환

#### 장점

- 구현이 단순하다.
- 별도 Ticket 저장소가 필요하지 않다.

#### 단점

- 리다이렉트 구간에서 장기 인증정보가 노출될 수 있다.
- 신규 회원가입 추가정보 흐름을 분리하기 어렵다.
- Callback이 카카오 인증과 서비스 인증 발급을 모두 담당한다.

### 선택지 2. 짧은 수명의 일회용 Ticket 발급

#### 장점

- Callback에서 서비스 JWT를 직접 전달하지 않는다.
- 로그인과 추가 회원가입 흐름을 구분할 수 있다.
- Ticket에 서버가 신뢰하는 OAuth 정보를 보관할 수 있다.
- 짧은 TTL과 일회성 소비로 탈취 위험을 줄일 수 있다.

#### 단점

- Redis 저장소가 필요하다.
- Ticket 발급·직렬화·소비 로직이 추가된다.
- Redis 장애 시 OAuth 흐름을 완료할 수 없다.
- Ticket 소비 후 DB 장애가 발생하면 인증을 다시 진행해야 한다.

---

## 결정

Callback에서는 JWT 대신 10분 동안 유효한 OAuth Ticket을 발급한다.

Ticket Payload에는 다음 정보를 저장한다.

```text
flowType
provider
providerUserId
email
```

Redis Key는 다음 규칙을 사용한다.
```text
oauth:ticket:{ticket}
```

Ticket 소비는 Redis GETDEL을 이용해 조회와 삭제를 원자적으로 실행한다.

LOGIN Ticket은 서비스 JWT와 교환하고,
SIGNUP Ticket은 추가정보 회원가입에 사용한다.

클라이언트가 전달하는 이메일이나 Provider ID를 신뢰하지 않고
Ticket에 저장된 서버 측 데이터를 사용한다.

---
## 결과

- Callback에서 서비스 JWT 직접 노출 제거
- 로그인과 회원가입 흐름 분리
- Ticket 재사용 방지
- 동시에 같은 Ticket을 소비하는 요청 중 하나만 성공
- 회원가입 시 OAuth 사용자 식별정보 위변조 방지
- React 프론트엔드 연동을 위한 API 기반 인증 구조 마련
