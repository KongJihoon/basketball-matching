# OAuth State와 브라우저 쿠키를 이용한 CSRF 방어

## 문제 상황

기존 카카오 OAuth 인가 요청은 다음 값만 전달했다.

```text
client_id
redirect_uri
response_type=code
```

Callback도 카카오가 전달한 `code`만 받아 처리했다.

```java
@GetMapping("/kakao/callback")
public ResponseEntity<?> kakaoCallback(
        @RequestParam String code
) {
    // OAuth 로그인 처리
}
```

이 구조에서는 Callback 요청이 현재 브라우저에서 시작한 OAuth 요청인지 확인할 수 없다.

공격자가 자신의 카카오 계정으로 발급받은 인가 응답을 피해자의 브라우저에서 실행하게 만들면,
피해자가 공격자의 서비스 계정으로 로그인되는 Login CSRF가 발생할 수 있다.

OAuth Ticket을 일회용으로 소비하는 기능은 Callback 이후의 Ticket 재사용은 방지하지만,
Callback 자체가 정상적인 브라우저에서 시작되었는지는 증명하지 못한다.

---

## 선택지

### 선택지 1. 별도의 State 검증 없이 인가 코드만 사용

#### 장점

- 구현이 단순하다.
- 쿠키와 Redis 저장소가 추가로 필요하지 않다.

#### 단점

- Callback이 현재 브라우저에서 시작한 요청인지 확인할 수 없다.
- 공격자가 자신의 인가 응답을 다른 브라우저에 주입할 수 있다.
- OAuth Callback의 CSRF 방어 요구사항을 만족하지 못한다.

### 선택지 2. State를 Redis에만 저장하고 존재 여부 확인

#### 장점

- 임의로 생성한 State인지 확인할 수 있다.
- TTL과 일회성 소비를 적용할 수 있다.

#### 단점

- State가 어느 브라우저에서 발급되었는지 확인할 수 없다.
- 공격자가 자신의 브라우저에서 발급한 유효한 State와 인가 코드를 피해자에게 전달할 수 있다.
- 서버가 발급한 값이라는 사실만 확인할 뿐, 요청 브라우저와의 연결은 보장하지 못한다.

### 선택지 3. State를 Redis에 저장하고 HttpOnly 쿠키와 연결

#### 장점

- Callback의 State와 OAuth를 시작한 브라우저를 연결할 수 있다.
- Redis TTL로 State의 수명을 제한할 수 있다.
- Redis `GETDEL`을 이용해 State를 한 번만 소비할 수 있다.
- HttpOnly 쿠키로 JavaScript 접근을 제한할 수 있다.
- 운영환경에서 Secure 쿠키를 사용해 HTTPS 연결에서만 전달할 수 있다.

#### 단점

- Redis가 중단되면 OAuth 로그인을 완료할 수 없다.
- 쿠키 설정과 프로필별 Secure 정책이 추가된다.
- OAuth 인가 URL 생성과 Callback 처리 과정이 이전보다 복잡해진다.

---

## 결정

예측하기 어려운 State를 생성해 Redis에 저장하고,
동일한 State를 HttpOnly 쿠키로 브라우저에 저장한다.

```text
Redis Key: oauth:state:{state}
Redis Value: valid
TTL: 3분
```

State는 `SecureRandom`으로 32바이트 난수를 생성한 뒤 URL-safe Base64 문자열로 변환한다.

```java
byte[] randomBytes = new byte[32];
secureRandom.nextBytes(randomBytes);

String state = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(randomBytes);
```

브라우저 쿠키에는 다음 속성을 적용한다.

```text
Name: oauth_state
HttpOnly: true
SameSite: Lax
Path: /api/v1/auth/oauth2/kakao/callback
Max-Age: 3분
Secure: local=false, prod=true
```

Callback에서는 다음 순서로 검증한다.

```text
1. Callback Query Parameter의 State 확인
2. oauth_state 쿠키 확인
3. Callback State와 쿠키 State 비교
4. Redis GETDEL로 State 조회와 삭제
5. State 검증 성공 후 카카오 Token API 호출
6. 성공과 실패 여부와 관계없이 State 쿠키 삭제
```

문자열 비교에는 `MessageDigest.isEqual()`을 사용한다.

```java
MessageDigest.isEqual(
        returnedState.getBytes(StandardCharsets.UTF_8),
        cookieState.getBytes(StandardCharsets.UTF_8)
);
```

Redis State는 조회와 삭제를 하나의 명령으로 처리한다.

```java
String storedValue = redisService.getAndDeleteData(
        "oauth:state:" + returnedState
);
```

따라서 동일한 State로 동시에 Callback을 요청해도 하나의 요청만 성공할 수 있다.

---

## 트레이드오프

보안을 위해 OAuth 흐름이 Redis와 쿠키에 의존하게 되었다.

Redis 장애나 브라우저의 쿠키 차단이 발생하면 OAuth 로그인을 다시 시작해야 한다.
하지만 State는 장기간 유지해야 하는 데이터가 아니며,
실패 시 로그인 과정을 다시 시작할 수 있으므로 별도의 복구 로직은 추가하지 않는다.

로컬환경은 HTTP를 사용하므로 `Secure=false`를 사용한다.
운영환경은 HTTPS를 전제로 `Secure=true`를 사용한다.

```yaml
# application-local.yml
secure-cookie: false
```

```yaml
# application-prod.yml
secure-cookie: true
```

---

## 결과

- OAuth Callback의 Login CSRF 방어 추가
- OAuth 요청을 시작한 브라우저와 Callback 연결
- 변조되거나 누락된 State 차단
- 만료되거나 이미 사용한 State 재사용 차단
- State 검증 전 카카오 외부 API 호출 방지
- Callback 처리 후 State 쿠키 삭제
- 로컬 HTTP와 운영 HTTPS 환경의 쿠키 정책 분리

