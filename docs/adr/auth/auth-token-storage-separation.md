# JWT 생성과 Redis 토큰 저장 책임 분리

## 문제 상황

기존 TokenProvider는 JWT 생성과 검증뿐만 아니라
Refresh Token을 Redis에 저장하는 책임도 가지고 있었다.


```java
String refreshToken =
        generateRefreshToken(email);

redisService.setDataExpireMillis(
        "refreshToken:" + email,
        refreshToken,
        expiration
);
```

이 구조에서는 JWT 생성 로직이 Redis 구현에 직접 의존한다.

그 결과 다음 문제가 발생했다.
- TokenProvider 단위 테스트에 Redis 의존성이 필요하다.
- JWT 생성 정책과 토큰 저장 정책을 독립적으로 변경하기 어렵다.
- Refresh Token과 로그아웃 Access Token Key가 여러 클래스에 분산된다.

---

## 선택지

### 선택지 1. TokenProvider에서 Redis 저장 유지

#### 장점
- 토큰 생성과 저장이 하나의 메서드에서 처리된다.
- 호출하는 서비스 코드가 단순하다.

#### 단점
- JWT 생성과 Redis 저장 책임이 결합된다.
- TokenProvider를 Redis없이 사용할 수 없다.
- 토큰 저장 정책 변경이 JWT 컴포넌트에 영향을 준다.


### 선택지 2. AuthTokenStore 분리

#### 장점
- TokenProvider가 JWT 생성과 검증에 집중할 수 있다.
- Redis Token Key를 한 곳에서 관리할 수 있다.
- 토큰 저장 정책을 독립적으로 테스트하고 변경할 수 있다.

#### 단점
- AuthService가 토큰 생성 이후 저장을 명시적으로 호출해야한다.
- 인증 흐름에 협력 객체가 추가된다.

---

## 결정

TokenProvider에서는 RedisService 의존성을 제거한다.
Refresh Token과 로그아웃된 Access Token의 저장 및 조회는
AuthTokenStore에서 담당한다.

```java
String refreshToken =
        tokenProvider.createRefreshToken(email);

authTokenStore.saveRefreshToken(
        email,
        refreshToken,
        expirationMillis
);
```

AuthService는 토큰 발급 흐름을 조정하고,
TokenProvider와 AuthTokenStore는 각각 생성과 저장을 담당한다.

---

## 결과

- TokenProvider에서 RedisService 의존성 제거
- JWT 생성과 Redis 저장 책임 분리
- Refresh Token Key 관리 위치 통합
- 로그아웃 Access Token Key 관리 위치 통합
- TokenProvider 단위 테스트 범위 단순화