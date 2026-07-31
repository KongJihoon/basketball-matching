# OAuth State와 MockMvc 통합 테스트 학습 노트

## 1. 이 문서의 목적

이 문서는 OAuth State 보안 리팩터링에서 처음 사용한 개념과 문법을 다시 학습하기 위해 작성했다.

ADR은 왜 이 방식을 선택했는지 기록하고,
리팩터링 문서는 어떤 구조로 변경했는지 기록한다.

이 문서는 다음 질문에 답하는 것을 목표로 한다.

- OAuth State는 왜 필요한가?
- Redis와 쿠키를 함께 사용하는 이유는 무엇인가?
- `SecureRandom`, Base64 URL 인코딩은 무엇인가?
- `ResponseCookie`의 각 속성은 무엇을 의미하는가?
- `try-finally`로 쿠키를 삭제하는 이유는 무엇인가?
- MockMvc는 실제 서버와 무엇이 다른가?
- `@MockBean`은 일반 Mockito `@Mock`과 무엇이 다른가?
- `perform`, `andExpect`, `andReturn`은 어떤 순서로 동작하는가?
- 변조와 재사용 테스트는 무엇을 증명하는가?

---

## 2. OAuth State를 쉽게 이해하기

OAuth State는 로그인 요청에 붙이는 일회용 확인표와 비슷하다.

```text
1. 우리 서버가 확인표를 만든다.
2. 확인표를 브라우저 쿠키와 Redis에 저장한다.
3. 카카오 로그인 요청에도 같은 확인표를 붙인다.
4. 카카오가 Callback으로 확인표를 돌려준다.
5. 세 곳의 확인표가 맞는지 검사한다.
```

현재 구현에서 비교하는 값은 다음과 같다.

```text
Callback Query Parameter의 state
브라우저 oauth_state 쿠키
Redis oauth:state:{state}
```

세 값 중 하나라도 없거나 다르면 인증을 중단한다.

### State가 없을 때 가능한 문제

공격자가 자신의 카카오 계정으로 OAuth 로그인을 시작했다고 가정한다.

공격자는 자신의 Authorization Code를 포함한 Callback 요청을 피해자에게 전달할 수 있다.
서버가 인가 코드만 확인하면 피해자의 브라우저가 공격자 계정으로 로그인될 수 있다.

이것을 Login CSRF라고 한다.

### Redis에만 State를 저장하면 부족한 이유

Redis에 State가 존재한다는 사실은 우리 서버가 발급한 State임을 의미한다.

하지만 어느 브라우저에 발급했는지는 알 수 없다.
공격자가 자신의 브라우저에서 발급한 State도 Redis에는 정상적으로 존재한다.

따라서 HttpOnly 쿠키에도 State를 저장하여 OAuth 요청을 시작한 브라우저와 연결했다.

---

## 3. SecureRandom 문법

```java
private final SecureRandom secureRandom =
        new SecureRandom();
```

`SecureRandom`은 보안 목적의 난수를 생성한다.

일반적인 `Random`은 같은 Seed를 알면 이후 값을 예측할 수 있으므로
인증 토큰이나 State 생성에 적합하지 않다.

```java
byte[] randomBytes = new byte[32];
secureRandom.nextBytes(randomBytes);
```

코드의 의미:

```text
new byte[32]
→ 32바이트 크기의 배열 생성

nextBytes(randomBytes)
→ 배열을 보안 난수로 채움
```

32바이트는 256비트다.

```text
32 byte × 8 bit = 256 bit
```

### 필드가 생성자 파라미터가 되지 않는 이유

클래스에는 `@RequiredArgsConstructor`가 있지만 `SecureRandom`은 생성자 파라미터가 아니다.

```java
private final SecureRandom secureRandom =
        new SecureRandom();
```

이미 필드 선언에서 값이 초기화되었기 때문이다.

Lombok은 초기화되지 않은 `final` 필드와 `@NonNull` 필드를 생성자 파라미터로 만든다.

---

## 4. Base64 URL 인코딩

난수 바이트는 그대로 URL Query Parameter에 넣기 어렵다.
따라서 문자열로 변환한다.

```java
return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(randomBytes);
```

### `getUrlEncoder()`

일반 Base64에는 `+`, `/` 문자가 포함될 수 있다.
이 문자는 URL에서 특별한 의미를 가질 수 있다.

URL-safe Base64는 다음처럼 변환한다.

```text
+ 대신 -
/ 대신 _
```

### `withoutPadding()`

Base64 문자열 끝에 붙을 수 있는 `=` 문자를 제거한다.

```text
일반 Base64: abcdef==
Padding 제거: abcdef
```

32바이트를 Padding 없이 Base64 URL 인코딩하면 현재 테스트 기준 43자 문자열이 생성된다.

```java
assertEquals(43, state.length());
assertTrue(state.matches("[A-Za-z0-9_-]{43}"));
```

---

## 5. ConfigurationProperties와 Duration

```java
@ConfigurationProperties(prefix = "app.oauth2.security")
public record OAuthSecurityProperties(
        Duration stateExpiration,
        boolean secureCookie
) {
}
```

YAML 설정을 Java 객체로 바인딩한다.

```yaml
app:
  oauth2:
    security:
      state-expiration: 3m
      secure-cookie: false
```

매핑 결과:

```text
state-expiration → stateExpiration
secure-cookie    → secureCookie
```

Spring Boot의 Relaxed Binding으로 kebab-case YAML과 camelCase Java 필드가 연결된다.

`3m`은 `Duration.ofMinutes(3)`에 해당한다.

```java
properties.stateExpiration().toMillis()
```

결과:

```text
180000 밀리초
```

### Record를 사용한 이유

설정 객체는 생성 이후 값이 변할 필요가 없다.

Record를 사용하면 다음 코드가 자동으로 만들어진다.

```text
생성자
접근 메서드
equals/hashCode
toString
```

접근할 때는 일반 Getter가 아니라 Record Component 이름을 사용한다.

```java
properties.stateExpiration();
properties.secureCookie();
```

---

## 6. ResponseCookie 문법

```java
ResponseCookie cookie = ResponseCookie
        .from("oauth_state", state)
        .httpOnly(true)
        .secure(properties.secureCookie())
        .sameSite("Lax")
        .path(CALLBACK_PATH)
        .maxAge(properties.stateExpiration())
        .build();
```

Builder 패턴으로 HTTP 응답 쿠키를 생성한다.

### `HttpOnly`

```java
.httpOnly(true)
```

브라우저 JavaScript에서 `document.cookie`로 읽지 못하게 한다.

XSS가 발생했을 때 JavaScript를 통한 쿠키 탈취 가능성을 줄인다.

### `Secure`

```java
.secure(properties.secureCookie())
```

`true`이면 HTTPS 요청에서만 쿠키를 전송한다.

로컬환경은 HTTP이므로 `false`, 운영환경은 HTTPS이므로 `true`를 사용한다.

```text
local → false
prod  → true
```

### `SameSite=Lax`

```java
.sameSite("Lax")
```

다른 사이트에서 시작한 일반적인 하위 요청에는 쿠키 전송을 제한하지만,
카카오에서 우리 Callback으로 돌아오는 최상위 GET 이동에는 쿠키를 보낼 수 있다.

`Strict`를 사용하면 외부 카카오 사이트에서 돌아오는 OAuth Callback에 쿠키가 포함되지 않을 수 있다.

### `Path`

```java
.path("/api/v1/auth/oauth2/kakao/callback")
```

State 쿠키를 전체 API에 전송할 필요가 없으므로 Callback 경로로 범위를 제한한다.

### `Max-Age`

```java
.maxAge(properties.stateExpiration())
```

브라우저 쿠키의 유효시간을 Redis State TTL과 동일하게 맞춘다.

### 응답에 쿠키 추가

```java
response.addHeader(
        HttpHeaders.SET_COOKIE,
        cookie.toString()
);
```

최종 HTTP 응답에는 대략 다음 Header가 추가된다.

```text
Set-Cookie: oauth_state=...; Path=/api/v1/auth/oauth2/kakao/callback; Max-Age=180; HttpOnly; SameSite=Lax
```

---

## 7. MessageDigest.isEqual 문법

```java
MessageDigest.isEqual(
        returnedState.getBytes(StandardCharsets.UTF_8),
        cookieState.getBytes(StandardCharsets.UTF_8)
);
```

두 문자열을 UTF-8 바이트 배열로 변환해서 비교한다.

```text
returnedState String
→ UTF-8 byte[]

cookieState String
→ UTF-8 byte[]
```

일반 `String.equals()`도 값의 일치 여부를 비교할 수 있다.
보안 관련 비밀값 비교에서는 비교 과정의 시간 차이를 줄이는 방식을 사용할 수 있다.

현재 State는 URL에도 포함되는 값이므로 비밀번호와 완전히 같은 비밀정보는 아니지만,
보안 토큰 비교 정책을 명확하게 하기 위해 이 방식을 적용했다.

---

## 8. Redis GETDEL과 일회성 소비

```java
String storedValue = redisService.getAndDeleteData(
        stateKey(returnedState)
);
```

내부 구현:

```java
return redisTemplate.opsForValue()
        .getAndDelete(key);
```

`GETDEL`은 값을 조회하고 같은 명령에서 Key를 삭제한다.

다음 두 명령을 따로 실행하는 방식과 다르다.

```text
GET key
DELETE key
```

GET과 DELETE 사이에 다른 요청이 들어오면 두 요청이 모두 같은 값을 읽을 수 있다.

GETDEL은 조회와 삭제가 하나의 Redis 명령으로 처리되므로
동일한 State를 한 요청만 소비할 수 있다.

```text
첫 번째 요청 → valid 반환 후 Key 삭제
두 번째 요청 → null 반환
```

---

## 9. try-finally로 쿠키 삭제

```java
try {
    OAuthCallbackResponse callbackResponse =
            oAuthService.kakaoCallback(
                    code,
                    state,
                    cookieState
            );

    return ResponseEntity.ok(...);
} finally {
    oAuthStateCookieManager.delete(response);
}
```

`finally`는 `try` 내부에서 성공하거나 예외가 발생해도 실행된다.

```text
Callback 성공 → 쿠키 삭제
State 불일치 → 쿠키 삭제
카카오 API 실패 → 쿠키 삭제
서비스 예외 발생 → 쿠키 삭제
```

State 검증이 실패한 쿠키도 계속 브라우저에 남길 이유가 없으므로 항상 삭제한다.

쿠키 삭제는 같은 이름과 같은 Path로 빈 값을 설정하고 `Max-Age=0`을 사용한다.

```java
ResponseCookie.from(COOKIE_NAME, "")
        .path(CALLBACK_PATH)
        .maxAge(0)
        .build();
```

쿠키는 이름뿐 아니라 Path도 식별에 사용되므로 생성할 때와 삭제할 때 Path가 같아야 한다.

---

## 10. `@RequestParam(required = false)`를 사용한 이유

```java
@RequestParam(
        name = "state",
        required = false
)
String state
```

기본값인 `required=true`에서는 Parameter가 없을 때 Controller 메서드에 진입하지 않는다.

Spring MVC가 먼저 `MissingServletRequestParameterException`을 발생시킨다.

현재 프로젝트의 공통 예외처리가 모든 일반 예외를 500으로 변환하기 때문에
OAuth 전용 오류 응답을 만들기 어렵다.

`required=false`로 Controller 진입을 허용하고 서비스에서 검증한다.

```java
if (!StringUtils.hasText(state)) {
    throw new CustomException(OAUTH_STATE_INVALID);
}
```

그러면 다음처럼 프로젝트가 정의한 오류로 응답할 수 있다.

```text
HTTP 400
OAUTH_STATE_INVALID
```

---

## 11. MockMvc란 무엇인가

MockMvc는 실제 8080 포트에 서버를 띄우지 않고 Spring MVC 요청 처리를 테스트한다.

다음 항목은 실제 Spring MVC 구성으로 동작한다.

```text
Controller
RequestMapping
Security Filter Chain
Validation
ExceptionHandler
JSON 직렬화
HTTP Header와 Cookie
```

다음 항목은 Mock 객체를 사용한다.

```text
HttpServletRequest
HttpServletResponse
실제 네트워크 포트
```

따라서 단위 테스트보다 넓은 범위를 검증하지만,
실제 외부 서버로 HTTP 통신하는 End-to-End 테스트는 아니다.

---

## 12. 테스트 Annotation 이해

### IntegrationTestSupport 상속

```java
class OAuthStateFlowIntegrationTest
        extends IntegrationTestSupport {
}
```

부모 클래스에 다음 설정이 있다.

```java
@SpringBootTest
@ActiveProfiles("test")
```

그리고 Testcontainers의 실제 MySQL과 Redis 주소를 Spring 설정에 연결한다.

따라서 자식 테스트는 전체 Spring Context와 실제 테스트용 저장소를 사용한다.

### `@AutoConfigureMockMvc`

```java
@AutoConfigureMockMvc
```

Spring Boot가 `MockMvc`를 자동 구성한다.

```java
@Autowired
private MockMvc mockMvc;
```

IntelliJ가 빨간 줄을 표시하더라도 다음 Gradle 결과가 성공하면 실제 Bean 구성은 정상이다.

```text
compileTestJava → BUILD SUCCESSFUL
OAuthStateFlowIntegrationTest → BUILD SUCCESSFUL
```

### `@MockBean`

```java
@MockBean
private KakaoOAuthClient kakaoOAuthClient;
```

일반 `@Mock`과 차이가 있다.

```text
@Mock
→ 테스트 클래스 내부의 Mockito 객체
→ Spring Context의 기존 Bean은 바뀌지 않음

@MockBean
→ Spring Context의 KakaoOAuthClient Bean을 Mockito Mock으로 교체
→ 실제 OAuthService에도 Mock 객체가 주입됨
```

외부 카카오 서버는 테스트 안정성에 영향을 주므로 Mock으로 교체한다.

반면 다음 객체는 실제 Bean을 사용한다.

```text
OAuth2Controller
OAuthService
OAuthStateStore
OAuthStateCookieManager
RedisService
GlobalExceptionHandler
SecurityFilterChain
```

이 테스트의 핵심은 외부 카카오 서버를 테스트하는 것이 아니라
우리 애플리케이션의 State 보안 흐름을 검증하는 것이다.

---

## 13. MockMvc 요청 문법

```java
mockMvc.perform(
        get(CALLBACK_PATH)
                .param("code", AUTHORIZATION_CODE)
                .param("state", state)
                .cookie(
                        new Cookie(
                                "oauth_state",
                                state
                        )
                )
)
```

### `get(path)`

GET 요청을 생성한다.

```java
get("/api/v1/auth/oauth2/kakao/callback")
```

### `.param()`

Query Parameter를 추가한다.

```java
.param("code", "authorization-code")
.param("state", "oauth-state")
```

생성되는 요청 형태:

```text
GET /callback?code=authorization-code&state=oauth-state
```

### `.cookie()`

브라우저가 서버에 보내는 Cookie를 추가한다.

```java
.cookie(new Cookie("oauth_state", state))
```

Response의 `Set-Cookie`와 Request의 `Cookie`를 구분해야 한다.

```text
Set-Cookie → 서버가 브라우저에 쿠키 저장 요청
Cookie     → 브라우저가 서버로 저장된 쿠키 전송
```

### `perform()`

생성한 요청을 Spring MVC에 전달한다.

```java
mockMvc.perform(requestBuilder)
```

---

## 14. MockMvc 응답 검증 문법

### HTTP 상태 검증

```java
.andExpect(status().isOk())
```

```java
.andExpect(status().isBadRequest())
```

```java
.andExpect(status().is3xxRedirection())
```

### JSON Path 검증

```java
.andExpect(
        jsonPath("$.data.flowType")
                .value("SIGNUP")
)
```

JSON Path 의미:

```text
$              → JSON 최상위
$.data         → data 객체
$.data.flowType → data 내부 flowType
```

예외 응답 검증:

```java
.andExpect(
        jsonPath("$.errorCode")
                .value("OAUTH_STATE_INVALID")
)
```

### Header 검증

```java
.andExpect(
        header().string(
                HttpHeaders.SET_COOKIE,
                containsString("Max-Age=0")
        )
)
```

`containsString`은 Hamcrest Matcher다.

전체 Header 문자열이 완전히 같은지 비교하지 않고,
필요한 일부 문자열이 포함됐는지 확인한다.

쿠키 속성 순서는 프레임워크에 따라 달라질 수 있으므로 부분 검증이 더 안정적이다.

---

## 15. MvcResult와 andReturn

```java
MvcResult result = mockMvc.perform(...)
        .andExpect(status().isOk())
        .andReturn();
```

`andExpect`만 사용하면 응답을 검증하고 끝난다.

응답 Body, Redirect URL 또는 Header 값을 이후 코드에서 직접 사용하려면 `andReturn()`으로 결과를 받는다.

```java
String responseBody = result.getResponse()
        .getContentAsString();
```

```java
String redirectUrl = result.getResponse()
        .getRedirectedUrl();
```

---

## 16. ObjectMapper와 JsonNode

```java
JsonNode responseBody = objectMapper.readTree(
        result.getResponse().getContentAsString()
);
```

응답 JSON 문자열을 트리 형태로 변환한다.

```java
String ticket = responseBody
        .path("data")
        .path("ticket")
        .asText();
```

처리 순서:

```text
JSON 문자열
→ JsonNode
→ data 노드
→ ticket 노드
→ String
```

DTO 전체로 역직렬화하지 않고 특정 값만 후속 테스트 정리에 사용할 때 편리하다.

---

## 17. thenAnswer와 invocation.getArgument

```java
when(kakaoOAuthClient.createAuthorizationUrl(
        anyString()
)).thenAnswer(invocation -> {

    String state = invocation.getArgument(0);

    return UriComponentsBuilder
            .fromUriString(
                    "https://kauth.kakao.com/oauth/authorize"
            )
            .queryParam("state", state)
            .build()
            .toUriString();
});
```

`thenReturn()`은 항상 같은 값을 반환한다.

```java
thenReturn("고정된 값");
```

`thenAnswer()`는 Mock 메서드에 실제로 전달된 인자에 따라 반환값을 만들 수 있다.

```java
String state = invocation.getArgument(0);
```

의미:

```text
createAuthorizationUrl(state)
                       ↑
              0번째 인자를 조회
```

State는 테스트 실행 시 `SecureRandom`으로 생성되므로 미리 값을 알 수 없다.
따라서 전달받은 State를 Redirect URL에 포함하기 위해 `thenAnswer()`를 사용했다.

---

## 18. UriComponentsBuilder 문법

Redirect URL 생성:

```java
UriComponentsBuilder
        .fromUriString(AUTHORIZATION_URL)
        .queryParam("state", state)
        .build()
        .toUriString();
```

Redirect URL에서 State 조회:

```java
String state = UriComponentsBuilder
        .fromUriString(redirectUrl)
        .build()
        .getQueryParams()
        .getFirst("state");
```

문자열을 직접 `split("state=")`하지 않고 URI 구조로 안전하게 처리한다.

---

## 19. RequestBuilder 헬퍼 메서드

```java
private RequestBuilder callbackRequest(
        String state
) {
    return get(CALLBACK_PATH)
            .param("code", AUTHORIZATION_CODE)
            .param("state", state)
            .cookie(
                    new Cookie(
                            "oauth_state",
                            state
                    )
            );
}
```

정상 요청과 재사용 요청이 같은 형태이므로 중복을 제거했다.

`get()`의 반환 객체는 `RequestBuilder` 역할을 수행하며,
`MockMvc.perform()`에 전달할 수 있다.

```java
mockMvc.perform(
        callbackRequest(state)
);
```

---

## 20. Mockito verify 문법

### 한 번 호출됐는지 검증

```java
verify(kakaoOAuthClient)
        .getUserInfo(AUTHORIZATION_CODE);
```

기본값은 `times(1)`이다.

```java
verify(kakaoOAuthClient, times(1))
        .getUserInfo(AUTHORIZATION_CODE);
```

### 호출되지 않았는지 검증

```java
verify(
        kakaoOAuthClient,
        never()
).getUserInfo(anyString());
```

변조된 State에서는 카카오 API를 호출하기 전에 요청을 차단했는지 증명한다.

### 재사용 테스트의 `times(1)`

```java
verify(
        kakaoOAuthClient,
        times(1)
).getUserInfo(AUTHORIZATION_CODE);
```

첫 번째 Callback에서 한 번 호출되고,
두 번째 재사용 Callback에서는 호출되지 않았음을 함께 증명한다.

---

## 21. 테스트 데이터 정리

통합 테스트는 실제 Redis를 사용한다.

테스트가 생성한 Key를 남겨두면 다른 테스트에 영향을 줄 수 있다.

```java
private final Set<String> createdRedisKeys =
        new HashSet<>();
```

생성한 State와 Ticket Key를 기록한다.

```java
createdRedisKeys.add(stateKey(state));
createdRedisKeys.add(ticketKey(ticket));
```

각 테스트 종료 후 삭제한다.

```java
@AfterEach
void clearRedis() {
    createdRedisKeys.forEach(
            redisService::deleteData
    );

    createdRedisKeys.clear();
}
```

State가 정상 소비된 경우 이미 Redis에서 삭제됐지만,
존재하지 않는 Key를 다시 삭제해도 문제가 없으므로 동일한 정리 방식을 사용한다.

---

## 22. 정상 흐름 테스트 해석

```text
1. GET /kakao/authorization 요청
2. SecureRandom State 생성
3. 실제 Redis에 oauth:state:{state}=valid 저장
4. 응답의 oauth_state Set-Cookie 검증
5. Redirect URL에서 State 추출
6. Callback Query에 State 전달
7. Callback Cookie에 같은 State 전달
8. 실제 Redis에서 State GETDEL
9. Mock Kakao 사용자 정보 반환
10. SIGNUP OAuth Ticket 발급
11. 응답 JSON과 쿠키 삭제 검증
12. Redis에서 State가 사라졌는지 확인
```

이 테스트는 단순히 Controller 응답만 확인하지 않는다.

```text
HTTP 요청
Controller
Cookie Manager
OAuthService
OAuthStateStore
실제 Redis
공통 예외처리
HTTP 응답
```

전체 흐름을 함께 검증한다.

---

## 23. 변조 테스트 해석

테스트는 Query State와 Cookie State를 다르게 보낸다.

```text
Query State  : tampered-state
Cookie State : 정상 발급 State
```

기대 결과:

```text
HTTP 400
OAUTH_STATE_INVALID
카카오 API 호출 0회
State 쿠키 삭제
```

Redis State가 남아 있는 것도 확인한다.

두 State가 일치하지 않으므로 Redis에서 소비하기 전에 요청을 차단하기 때문이다.

---

## 24. 재사용 테스트 해석

같은 State로 Callback을 두 번 요청한다.

```text
첫 번째 요청
→ Redis GETDEL 성공
→ OAuth 처리 성공

두 번째 요청
→ Redis에 State 없음
→ OAUTH_STATE_INVALID
```

이 테스트는 State가 단순히 만료시간만 가진 것이 아니라 실제 일회용인지 확인한다.

---

## 25. 이 테스트가 검증하지 않는 것

`OAuthStateFlowIntegrationTest`가 모든 OAuth 기능을 검증하는 것은 아니다.

검증하지 않는 범위:

- 실제 카카오 Authorization Server 연결
- 실제 카카오 Access Token 발급
- 브라우저별 쿠키 정책 차이
- 운영 HTTPS와 Reverse Proxy 설정
- React 페이지의 Redirect 처리
- 카카오 장애 및 Timeout

외부 카카오 API 통신은 별도의 API 테스트 또는 제한된 수동 테스트로 확인한다.

---

## 26. 자주 헷갈릴 수 있는 내용

### MockMvc는 Mockito Mock인가?

아니다.

MockMvc는 Spring MVC 요청 처리 테스트 도구다.
Mockito의 `mock()`과 목적이 다르다.

### `@MockBean`을 사용했는데 통합 테스트인가?

그렇다.

외부 카카오 Client만 Mock으로 교체했으며,
나머지 Spring Context와 Redis는 실제 구성을 사용한다.

통합 테스트에서도 테스트 범위 밖의 외부 시스템은 Mock으로 격리할 수 있다.

### 왜 Controller 단위 테스트가 아니라 통합 테스트인가?

이번 테스트의 목적은 Controller 메서드 한 개가 아니라 다음 협력을 확인하는 것이다.

```text
Security Filter
Controller
Cookie
Service
Redis
ExceptionHandler
```

### IntelliJ의 MockMvc 빨간 줄은 오류인가?

Gradle 테스트 컴파일과 실제 테스트가 성공했다면 코드 오류가 아니다.

부모 테스트 클래스의 `@SpringBootTest`와 자식 클래스의 `@AutoConfigureMockMvc` 조합을
IntelliJ Inspection이 완전히 인식하지 못할 수 있다.

다음 순서로 확인한다.

```text
Gradle Reload
compileTestJava 실행
해당 테스트 직접 실행
필요하면 IntelliJ Cache 재생성
```

---

## 27. 테스트 실행 명령어

State 관련 테스트만 실행:

```bash
./gradlew test \
  --tests "com.example.basketballmatching.auth.oauth2.service.OAuthStateStoreUnitTest" \
  --tests "com.example.basketballmatching.auth.oauth2.support.OAuthStateCookieManagerUnitTest" \
  --tests "com.example.basketballmatching.auth.oauth2.service.OAuthServiceUnitTest" \
  --tests "com.example.basketballmatching.auth.oauth2.controller.OAuthStateFlowIntegrationTest"
```

테스트 컴파일만 확인:

```bash
./gradlew compileTestJava
```

---

## 28. 이번 구현에서 기억할 핵심

```text
State는 OAuth 요청과 Callback을 연결한다.

Redis 존재 여부만 확인하지 않고 브라우저 쿠키와 연결한다.

State는 SecureRandom으로 생성한다.

HttpOnly는 JavaScript 접근을 제한한다.

SameSite=Lax는 OAuth의 외부 사이트 Redirect를 고려한 설정이다.

Secure는 local=false, prod=true로 구분한다.

GETDEL은 State를 원자적으로 한 번만 소비한다.

MockMvc는 서버 포트 없이 Spring MVC 전체 흐름을 테스트한다.

@MockBean은 Spring Context의 실제 Bean을 Mock으로 교체한다.

통합 테스트에서도 테스트 범위 밖의 외부 시스템은 Mock으로 격리할 수 있다.
```

---

## 29. 참고 자료

- [OAuth 2.0 RFC 6749 - CSRF](https://www.rfc-editor.org/rfc/rfc6749.html#section-10.12)
- [OAuth 2.0 Security Best Current Practice RFC 9700](https://datatracker.ietf.org/doc/html/rfc9700)
- [Spring Framework MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html)
- [Spring Boot 3.2.5 MockBean](https://docs.spring.io/spring-boot/docs/3.2.5/api/org/springframework/boot/test/mock/mockito/MockBean.html)
- [Spring Framework ResponseCookie](https://docs.spring.io/spring-framework/docs/6.1.x/javadoc-api/org/springframework/http/ResponseCookie.html)
- [Redis GETDEL](https://redis.io/docs/latest/commands/getdel/)

