# User 도메인 책임 분리 리팩터링

## 1. 개요

기존 UserService는 회원가입, 회원정보 조회 및 수정, 비밀번호 변경,
회원 탈퇴, 경기 정리, 토큰 폐기, 알림 발송 등 여러 책임을 가지고 있었다.

기능은 정상적으로 동작하지만 하나의 서비스가 여러 도메인과 인프라 계층에 직접 의존하여 코드 변경 영향 범위가 커지는 문제가 있었다.

이번 리펙터링은 현재 구조를 유지하면서 User 도메인의 책임을 분리하는 것이다.

---

## 2. 기존 문제

### UserService의 과도한 책임

기존 회원 탈퇴 로직은 다음 작업을 하나의 서비스에서 처리했다.

- 사용자 조회 및 탈퇴 상태 변경
- 사용자 생성 예정 경기 취소
- 경기 참가 상태 변경
- RefreshToken 삭제
- AccessToken 블랙리스트 등록
- 경기 취소 알림 전송

UserService가 user, game, auth, notification 도메인을 모두 알고 있었다.

그 결과 다음 문제가 발생한다.

- UserService 변경 시 여러 도메인의 영향을 함께 확인해야 했다.
- 탈퇴 트랜잭션의 경계 파악 어려움
- 단위 테스트를 위해 지나치게 많은 Mock 객체가 필요

## 3. 목표

- UserService는 일반 사용자 기능만 담당한다.
- 회원 탈퇴 유스케이스는 별도 서비스로 분리한다.
- 경기 정리는 gameCreator 도메인이 위임한다.
- DB 커밋이 성공한 이후 토큰 제거와 알림을 처리
- 단위 테스트와 통합테스트의 책임을 명확히 구분

---

## 4. 적용한 구조

### UserService

담당 책임:

- 회원가입
- 이메일 및 닉네임 중복 확인
- 회원정보 조회
- 회원정보 수정
- 비밀번호 변경
- 비밀번호 재설정

### UserWithdrawalService

담당 책임:

- Access Token 존재 여부 검증
- 활성 사용자 조회
- 탈퇴 시각 생성
- 경기 정리 서비스 호출
- UserEntity 탈퇴 상태 변경
- UserWithdrawnEvent 발행

### UserWithdrawalGameService

담당 책임:

- 탈퇴 사용자가 생성한 예정 경기 취소
- 해당 경기 참가자 상태 변경
- 탈퇴 사용자가 참가한 다른 예정 경기의 참가 상태 변경
- 알림 발송에 필요한 정보 반환

### UserWithdrawnEventListener

담당 책임:

- Refresh Token 삭제
- Access Token 블랙리스트 등록
- 경기 취소 알림 발송

---

## 5. 서비스 응답과 HTTP 응답 분리

기존 UserService는 다음과 같은 HTTP 응답 객체를 직접 반환했다.
- CommonResponse
- CheckResponse

이 구조에서는 서비스 계층이 Controller의 응답 형식에 의존했다.
리팩터링 후 서비스는 다음 값만 반환하도록 변경했다.
- SignUpDto.Response
- UserDto
- void
`CommonResponse`와 `CheckResponse` 생성은 Controller에서 담당한다.

이를 통해 서비스 계층을 HTTP 응답 형식으로부터 분리하고,
서비스 단위 테스트에서도 HTTP 응답 객체를 검증하지 않도록 개선했다.

---

## 6. API 책임 분리

기존에는 사용자와 관련된 API가 하나의 UserController에 집중되어 있었다.

이를 다음과 같이 분리했다.

### UserController
```text
POST   /api/v1/users/signup
GET    /api/v1/users/availability/email
GET    /api/v1/users/availability/nickname
GET    /api/v1/users/mypage
PATCH  /api/v1/users/mypage
PATCH  /api/v1/users/mypage/password
DELETE /api/v1/users/mypage
```

### EmailVerificationController
```text
POST /api/v1/email-verifications
POST /api/v1/email-verifications/confirm
```

### PasswordResetController
```text
POST  /api/v1/password-resets/email-verifications
POST  /api/v1/password-resets/email-verifications/confirm
PATCH /api/v1/password-resets
```

---

## 7. 시간 생성 방식 개선

기존에는 `LocalDateTime.now()`를 직접 호출하여 테스트에서 정확한 탈퇴 시간을 검증하기 어려웠다.

리펙터링 후 Clock을 주입받아 시간을 생성하도록 변경했다.

```java
LocalDateTime withdrawnAt =
        LocalDateTime.now(clock);
```
운영 환경에서는 TimeConfig가 시스템 Clock을 제공하고,
단위 테스트에서는 Clock.fixed()를 사용한다.

이를 통해 시간에 의존하는 테스트를 현재 시각과 관계없이
동일한 결과로 실행할 수 있게 되었다.

---

## 8. 테스트 전략

### 단위 테스트
Mockito를 사용하여 서비스의 분기와 협력 객체 호출을 검증했다.

주요 검증 대상:
- 회원가입 성공 및 실패
- 이메일 및 닉네임 중복
- 사용자 조회 실패
- 회원정보 수정
- 비밀번호 변경 및 재설정
- 탈퇴 토큰 누락
- 탈퇴 사용자 조회 실패
- 탈퇴 이벤트 발행 내용
- 고정 Clock을 이용한 탈퇴 시각

통합 테스트

Testcontainers를 이용하여 실제 MySQL과 Redis를 사용했다.

주요 검증 대상:
- 회원가입 데이터가 실제 MySQL에 저장되는지
- 비밀번호가 실제 PasswordEncoder로 암호화되는지
- Redis 인증 키가 삭제되는지
- JPA Dirty Checking으로 회원정보가 반영되는지
- 탈퇴 회원이 활성 사용자 조회에서 제외되는지
- 비밀번호 변경 권한 키와 TTL이 Redis에 저장되는지
- 회원 탈퇴 트랜잭션 커밋 이후 AFTER_COMMIT 이벤트가 실행되는지
- Refresh Token이 삭제되는지
- Access Token이 블랙리스트에 저장되는지
- 경기 취소 알림이 DB에 저장되는지

---

## 💡성과
- UserService에서 회원 탈퇴 책임을 분리했다.
- 회원 탈퇴의 DB 처리와 커밋 이후 부가 작업의 경계를 명확하게 만들었다.
- 서비스 계층에서 HTTP 응답 객체 의존성을 제거했다.
- 시간 의존 로직을 테스트 가능한 구조로 변경했다.
- H2가 아닌 운영 DB와 동일한 MySQL 기반 통합 테스트 환경을 구성했다.
- Redis를 포함한 실제 통합 흐름을 검증할 수 있게 되었다.
- 단위 테스트는 분기 검증, 통합 테스트는 기술 연동 검증이라는 기준을 세웠다.
- User 관련 리팩터링 이후 깨졌던 테스트를 새로운 책임 구조에 맞게 복구했다.

