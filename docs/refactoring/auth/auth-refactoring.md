# Auth 인증 책임 분리 리펙터링

## 1. 개요

기존 인증 구조는 로그인, JWT 생성, RefreshToken 저장, 로그아웃 처리 및 Security Filter 인증 처리가 여러 클래스에 뒤섞여있었다.

기능은 정상적으로 동작하지만 JWT 생성 클래스가 Redis 저장까지 담당하고 있었고,
인증 Filter가 Redis 조회와 JSON 예외 응답 생성을 직접 담당하여 처리하고 있었다.

기존 방식을 유지하면서 각 컴포넌트의 책임과 테스트 범위를 명확히 분리하는 것이 리펙터링의 목적이다.

---

## 2. 기존 문제

### AuthService 인터페이스와 단일 구현체

기존 구조는 `AuthService` 인터페이스와 `AuthServiceImpl` 구현체로 분리되어 있었다.

그러나 실제 구현체는 하나였다.

그 결과 다음 문제가 발생한다.

- 인증 로직을 확인하기 위해 인터페이스와 구현체 둘 다 접근해야 한다.
- 실제 확장 지점 없이 파일과 구조만 증가.

### TokenProvider의 과도한 책임

기존 `TokenProvider`는 다음 책임을 담당했다.

- AccessToken 생성
- RefreshToken 생성
- JWT 검증 및 파싱
- RefreshToken Redis 저장

JWT 생성 책임과 Redis 저장 책임이 결합되어 TokenProvider 단위 테스트에도 Redis 의존성이 필요하다.


### 인증 Filter의 과도한 책임

`AuthenticationFilter`는 다음 책임을 모두 담당하고 있었다.

- Authorization 헤더 추출
- JWT 검증
- 로그아웃 토큰 Redis 조회
- 블랙리스트 사용자 Redis 조회
- Authentication 생성
- SecurityContext 저장
- JSON 예외 응답 생성

또한 `filterChain.doFilter()`가 인증 예외 처리 범위 안에 있어 Controller나 Service에서 발생한 예외까지 filter가 가로챌 가능성이 있었다.

### 토큰 재발급 요청의 중복 정보

RefreshToken 내부에는 사용자 이메일이 Subject로 포함되어 있었지만, 기존 재발급 요청은 이메일과 RefreshToken을 함께 전달받았다.

동일한 사용자 식별 정보를 두 곳에서 전달받기 때문에 검증 대상과 API 요청 구조가 불필요하게 복잡했다.

---

## 3. 목표

- `AuthService`는 로그인과 토큰 재발급 흐름만 담당
- `TokenProvider`는 JWT 생성과 검증만 담당
- Redis의 인증 토큰 상태는 별도 컴포넌트에서 관리한다.
- 인증 Filter는 인증 흐름만 담당한다.
- Security 오류 응답 형식을 전역 API 오류 응답과 통일한다.
- 단위 테스트와 통합 테스트의 검증 분위를 구분한다.
- CI에서 단위 테스트와 통합 테스트를 분리하여 실행한다.

---

## 4. 적용한 구조

### AuthService

담당 책임:

- 일반 로그인
- 카카오 로그인 사용자 검증
- 비밀번호 및 로그인 제공자 검증
- 블랙리스트 사용자 검증
- Access Token과 Refresh Token 발급 흐름 조정
- Refresh Token 기반 Access Token 재발급
- 로그아웃 서비스 호출

### TokenProvider

담당 책임:

- Access Token 생성
- Refresh Token 생성
- JWT 파싱
- JWT 만료 및 유효성 검증
- 토큰에서 사용자 이메일 추출
- Spring Security Authentication 생성
- Access Token의 남은 만료시간 계산

### AuthTokenStore

담당 책임:

- Refresh Token 저장
- Refresh Token 조회 및 삭제
- 로그아웃된 Access Token 저장
- Access Token 폐기 여부 조회
- 인증 토큰 Redis Key 생성

### UserSessionRevocationService

담당 책임:

- Access Token 존재 여부 검증
- Access Token 남은 만료시간 조회
- 로그아웃된 Access Token 등록
- Refresh Token 삭제

### BlacklistStore

담당 책임:

- Redis에서 블랙리스트 사용자 여부 조회
- 블랙리스트 Redis Key 생성

### AuthenticationFilter

담당 책임:

- Authorization 헤더에서 Bearer Token 추출
- Access Token 검증
- 로그아웃 토큰 및 블랙리스트 사용자 검증
- Authentication 생성
- SecurityContext 저장

### SecurityErrorResponseWriter

담당 책임:

- Security Filter 단계에서 발생한 오류를 JSON으로 변환
- ErrorResponse 기반의 공통 오류 형식 제공
- Spring에서 관리하는 ObjectMapper 재사용

---

## 5. 서비스와 HTTP 응답 책임 분리

기존 로그아웃 서비스는 HTTP 응답 객체에 관여했다.

리펙터링 후 서비스는 인증 로직만 처리하고
Controller가 `CommonReponse`와 `CheckResponse`를 생성하도록 변경했다.

로그인과 토큰 재발급 결과는 다음 응답 DTO로 변환

```java
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        AuthenticatedUserResponse user
) {
}
```

AccessToken은 Authorization 응답 헤더와 DTO에 중복으로 전달하지 않고 응답 DTO를 통해 전달하도록 정리했다.

---

## 6. 인증 API 정리

기존 인증 API가 User 경로와 뒤섞여 있었으며
토큰 재발급과 로그아웃의 HTTP Method 및 URI 표현도 일관되지 않았다.

리펙터링 후 인증 API를 다음과 같이 정리했다.
```text
POST /api/v1/auth/login
POST /api/v1/auth/token/refresh
POST /api/v1/auth/logout
```

토큰 재발급 요청에는 이메일을 제거하고 RefreshToken만 전달받도록 변경하였다.
```java
public record TokenRefreshRequest(
        @NotBlank
        String refreshToken
) {
}
```

---

## 7. Security Filter 예외처리 개선

기존 Filter는 ObjectMapper를 직접 생성하고
Map을 이용해 JSON 오류 응답을 만들었다.

```java
ObjectMapper objectMapper =
        new ObjectMapper();
```

이 방식은 Spring에서 관리하는 ObjectMapper 설정을 재사용할 수 없고, 프로젝트 `ErrorResponse` 형식과도 중복된다.

리펙터링 후 `SecurityErrorResponseWriter`가 Spring ObjectMapper와 `ErrorResponse`를 이용해 응답을 생성한다.

```java
objectMapper.writeValue(
        response.getWriter(),
        ErrorResponse.of(errorCode)
);
```

또한 `filterChain.doFilter()`를 인증 예외 처리 범위 밖으로 이동했다.

이를 통해 Controller와 Service에서 발생한 예외를 AuthenticationFilter가 내부 인증 오류로 변경하지 않도록 헀다.

---

## 8. 테스트 전략

### 단위 테스트

Mockito를 사용하여 각 서비스의 분기와 협력 객체 호출을 검증했다.

주요 검증 대상: 

- Authorization 헤더가 없는 요청
- 정상 Access Token 인증
- 로그아웃 Access Token 접근 차단
- 블랙리스트 사용자 접근 차단
- 잘못된 JWT 인증 실패
- Filter 이후 발생한 예외가 인증 Filter에 의해 처리되지 않는지 검증
- 일반 로그인 성공 및 실패
- 로그인 제공자 불일치
- 블랙리스트 사용자 로그인 실패
- Refresh Token 저장
- Access Token 재발급
- 사용자 세션 폐기
- Access Token이 없는 로그아웃 요청

### 통합 테스트

Testcontainers를 이용하여 실제 MySQL과 Redis를 사용했다.

주요 검증 대상:
- 실제 PasswordEncoder를 이용한 로그인
- Access Token과 Refresh Token 생성
- JWT Subject에 사용자 이메일 저장
- Refresh Token Redis 저장 및 TTL
- 저장된 Refresh Token을 이용한 Access Token 재발급
- 로그아웃 후 Refresh Token 삭제
- 로그아웃 Access Token Redis 저장 및 TTL
- Redis에 등록된 블랙리스트 사용자의 로그인 차단

---

## 9. CI 검증

Github Actions에서 단위 테스트와 통합테스트 Job을 분리했다.

### Unit Test
- UserService 단위 테스트
- UserWithdrawalService 단위 테스트
- AuthService 단위 테스트
- UserSessionRevocationService 단위 테스트
- AuthenticationFilter 단위 테스트

### Integration Tests
- UserService 통합 테스트
- UserWithdrawalService 통합 테스트
- AuthService 통합 테스트

통합 테스트는 GitHub Actions의 Docker 환경에서

Testcontainers를 이용하여 MySQL과 Redis를 실행한다.


---

## 💡성과

- AuthService 인터페이스와 단일 구현체 구조를 제거했다.
- JWT 생성과 Redis 저장 책임을 분리했다.
- Redis 인증 Token Key 관리 위치를 통합했다.
- AuthenticationFilter에서 Redis 직접 접근을 제거했다.
- Security 오류 응답을 공통 ErrorResponse 형식으로 통일했다.
- Filter가 Controller와 Service 예외를 가로채는 범위를 제거했다.
- 인증 API URI와 HTTP Method를 정리했다.
- 토큰 재발급 요청의 중복 사용자 정보를 제거했다.
- 실제 MySQL과 Redis를 이용한 인증 통합 테스트를 구성했다.
- CI에서 단위 테스트와 통합 테스트를 독립적으로 검증하도록 개선했다.