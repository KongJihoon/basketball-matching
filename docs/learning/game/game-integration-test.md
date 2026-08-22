# 경기 통합 테스트 학습 노트

## 1. 이 문서의 목적

이 문서는 경기 리팩터링 과정에서 작성한 테스트의 역할과 처음 사용한 문법을 다시 학습하기 위해 작성했다.

단위 테스트는 비즈니스 정책을 빠르게 검증하고,
통합 테스트는 Spring, JPA, QueryDSL, MySQL이 실제로 연결된 상태를 검증한다.

이 문서는 다음 질문에 답하는 것을 목표로 한다.

- 단위 테스트와 통합 테스트는 무엇이 다른가?
- 통합 테스트에서 Mockito를 사용하지 않은 이유는 무엇인가?
- `@IntegrationTest`는 어떤 어노테이션을 묶은 것인가?
- Testcontainers로 MySQL과 Redis를 실행하는 이유는 무엇인가?
- `@Transactional`이 테스트에서 하는 역할은 무엇인가?
- `EntityManager.flush()`와 `clear()`는 왜 함께 사용하는가?
- JPA 변경 감지와 트랜잭션 롤백은 어떻게 확인하는가?
- QueryDSL Repository 통합 테스트에서는 무엇을 검증해야 하는가?
- `Page`, `Pageable`, `PageImpl`은 어떤 관계인가?
- `AFTER_COMMIT` 이벤트를 일반적인 트랜잭션 테스트에서 확인할 수 없는 이유는 무엇인가?

---

## 2. 테스트 종류별 책임

모든 동작을 하나의 테스트 방식으로 확인할 필요는 없다.
검증하려는 대상에 따라 테스트를 나누는 것이 중요하다.

| 테스트 종류 | 주요 검증 대상 | 현재 프로젝트의 예시 |
|---|---|---|
| 단위 테스트 | 조건 분기, 예외, 상태 변경, 이벤트 발행 | 경기 수정 정책, 참가 취소, 강퇴 이벤트 |
| 통합 테스트 | Spring Bean 연결, 트랜잭션, JPA 매핑, 실제 쿼리 | 경기 생성·수정·삭제, 참가·취소·재참가 |
| Repository 통합 테스트 | 동적 조건, 정렬, 페이징, Count Query | 경기 목록 검색 및 정렬 |
| API 테스트 | URI, HTTP 상태 코드, JSON, 인증·인가 | Controller와 Security 검증 |
| 성능 테스트 | 처리량, 응답 시간, 실행 계획, 병목 | k6, MySQL `EXPLAIN` |

### 단위 테스트에서 확인할 내용

단위 테스트는 Repository와 외부 의존성을 Mock으로 대체한다.

```java
@Mock
private GameRepository gameRepository;

@Mock
private ApplicationEventPublisher eventPublisher;
```

이를 통해 다음 내용을 빠르게 검증할 수 있다.

```text
잘못된 요청이면 올바른 예외가 발생하는가?
경기 수정 정책이 의도한 분기로 동작하는가?
Repository의 올바른 메서드를 호출하는가?
이벤트에 올바른 데이터가 담기는가?
```

하지만 Mock은 실제 SQL, JPA 연관관계, 변경 감지까지 보장하지 않는다.

### 통합 테스트에서 확인할 내용

통합 테스트는 실제 Spring Bean과 MySQL을 사용한다.

```text
Service가 실제 Repository와 연결되는가?
Entity가 실제 테이블에 저장되는가?
변경 감지가 UPDATE SQL로 반영되는가?
연관된 경기와 참가 정보가 함께 변경되는가?
QueryDSL 조건과 정렬이 실제 MySQL에서도 동작하는가?
```

현재 경기 통합 테스트에서 Mockito를 사용하지 않은 이유도 여기에 있다.
Mock을 사용하면 서비스와 데이터베이스의 연결을 검증하는 통합 테스트의 목적이 약해진다.

---

## 3. `@IntegrationTest` 통합 어노테이션

### 적용 위치

```text
src/test/java/com/example/basketballmatching/support/IntegrationTest.java
```

현재 여러 통합 테스트에서 반복되는 설정을 하나의 어노테이션으로 묶었다.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(
        initializers = IntegrationTestInitializer.class
)
public @interface IntegrationTest {
}
```

테스트 클래스에서는 다음처럼 사용한다.

```java
@IntegrationTest
@Transactional
class GameServiceIntegrationTest {
}
```

### `@Target(ElementType.TYPE)`

이 어노테이션을 클래스와 인터페이스에 붙일 수 있다는 의미다.

### `@Retention(RetentionPolicy.RUNTIME)`

실행 중에도 어노테이션 정보가 유지된다.
Spring은 런타임에 이 정보를 읽어 테스트 컨텍스트를 구성한다.

### `@Documented`

JavaDoc에 이 어노테이션의 사용 정보가 포함될 수 있도록 한다.

### `@Inherited`

부모 테스트 클래스에 붙은 어노테이션을 자식 클래스가 상속할 수 있게 한다.
현재는 직접 붙여 사용하지만, 공통 부모 테스트 클래스를 만들 때도 활용할 수 있다.

### `@SpringBootTest`

애플리케이션의 전체 Spring Context를 실행한다.

따라서 다음 객체들이 실제 Spring Bean으로 주입된다.

```java
@Autowired
private GameService gameService;

@Autowired
private GameRepository gameRepository;
```

단위 테스트보다 실행 시간이 길지만 실제 애플리케이션 조립 상태를 검증할 수 있다.

### `@ActiveProfiles("test")`

테스트 실행 시 `application-test.yml`을 활성화한다.

현재 테스트 설정의 주요 역할은 다음과 같다.

```text
테스트 실행마다 스키마 생성 및 제거
OSIV 비활성화
메일 연결 검사 비활성화
테스트 전용 JWT와 OAuth 설정 사용
Asia/Seoul 시간대 적용
```

### `@ContextConfiguration`

Spring Context가 만들어지기 전에 `IntegrationTestInitializer`를 실행한다.
Initializer가 Testcontainers를 시작하고 실제 접속 정보를 Spring 환경 변수에 주입한다.

---

## 4. Testcontainers 공통 설정

### 적용 위치

```text
src/test/java/com/example/basketballmatching/support/IntegrationTestInitializer.java
```

현재 통합 테스트는 MySQL과 Redis 컨테이너를 사용한다.

```java
private static final MySQLContainer<?> MYSQL =
        new MySQLContainer<>(
                DockerImageName.parse("mysql:8.0")
        )
                .withDatabaseName("basketball_test")
                .withUsername("test")
                .withPassword("test");
```

여기의 아이디와 비밀번호는 Testcontainers 내부에서만 사용하는 테스트용 값이다.
실제 운영 데이터베이스 비밀번호를 넣지 않는다.

Redis도 같은 방식으로 실행한다.

```java
private static final GenericContainer<?> REDIS =
        new GenericContainer<>(
                DockerImageName.parse("redis:7.2-alpine")
        )
                .withExposedPorts(6379);
```

### `static` 컨테이너를 사용한 이유

테스트 메서드마다 컨테이너를 새로 실행하면 실행 시간이 매우 길어진다.

컨테이너를 `static`으로 만들면 같은 테스트 JVM 안에서 공통으로 재사용할 수 있다.

```java
static {
    Startables.deepStart(
            Stream.of(MYSQL, REDIS)
    ).join();
}
```

`Startables.deepStart`는 MySQL과 Redis를 병렬로 시작한다.
`join()`은 두 컨테이너가 준비될 때까지 기다린다.

### 동적 포트를 주입해야 하는 이유

Testcontainers는 호스트의 빈 포트를 자동으로 할당한다.
실행할 때마다 MySQL과 Redis의 실제 호스트 포트가 달라질 수 있다.

따라서 고정된 `3306`, `6379`를 테스트 설정에 작성하지 않고 실행된 컨테이너에서 값을 가져온다.

```java
TestPropertyValues.of(
        "spring.datasource.url=" + MYSQL.getJdbcUrl(),
        "spring.datasource.username=" + MYSQL.getUsername(),
        "spring.datasource.password=" + MYSQL.getPassword(),
        "spring.data.redis.host=" + REDIS.getHost(),
        "spring.data.redis.port=" + REDIS.getMappedPort(6379)
).applyTo(context.getEnvironment());
```

### H2 대신 MySQL을 사용한 이유

H2는 빠르고 설정이 단순하지만 운영 환경의 MySQL과 SQL 문법 및 동작이 완전히 같지 않다.

현재 프로젝트는 다음 내용을 실제 MySQL 기준으로 검증할 필요가 있다.

```text
JPA DDL 생성 결과
Enum 및 날짜 타입 매핑
QueryDSL이 생성한 SQL
정렬과 페이징
복합 인덱스와 실행 계획
```

따라서 통합 테스트는 MySQL Testcontainers를 사용한다.
대신 Docker가 필요하고 최초 이미지 다운로드와 컨테이너 시작 시간이 추가된다.

---

## 5. `@Transactional` 테스트의 동작

```java
@IntegrationTest
@Transactional
class GameParticipantServiceIntegrationTest {
}
```

테스트 클래스에 `@Transactional`을 붙이면 각 테스트 메서드가 하나의 트랜잭션 안에서 실행된다.

테스트가 끝나면 기본적으로 트랜잭션을 커밋하지 않고 롤백한다.

```text
테스트 시작
→ 데이터 저장 및 수정
→ 검증
→ 테스트 종료
→ 전체 롤백
```

이 때문에 테스트마다 직접 데이터베이스 테이블을 비우지 않아도 테스트 간 데이터가 격리된다.

### 롤백이 보장하지 않는 것

데이터베이스 트랜잭션 롤백은 Redis 데이터까지 되돌리지 않는다.

통합 테스트가 Redis Key를 직접 생성한다면 다음 방식 중 하나가 필요하다.

```text
테스트 전후에 해당 Key를 직접 삭제
테스트 전용 Prefix 사용
테스트 종료 시 Redis 데이터 정리
```

MySQL 롤백만 믿고 Redis 정리를 생략하면 다른 테스트에 영향을 줄 수 있다.

---

## 6. 영속성 컨텍스트와 1차 캐시

JPA의 `EntityManager`는 트랜잭션 안에서 엔티티를 관리한다.
이 관리 공간을 영속성 컨텍스트라고 한다.

같은 ID의 엔티티를 다시 조회하면 JPA는 SQL을 실행하지 않고 1차 캐시에 있는 객체를 반환할 수 있다.

```java
GameEntity savedGame = gameRepository.save(game);
GameEntity foundGame = gameRepository.findById(savedGame.getGameId())
        .orElseThrow();
```

이때 `foundGame`이 실제 데이터베이스에서 다시 조회된 객체라고 단정할 수 없다.
같은 영속성 컨텍스트의 1차 캐시에서 반환됐을 가능성이 있다.

이 상태로 검증하면 실제 INSERT 또는 UPDATE 결과가 아니라 메모리의 엔티티 상태만 확인할 수 있다.

---

## 7. `flush()`와 `clear()`를 함께 사용하는 이유

현재 경기 통합 테스트에는 다음 메서드가 있다.

```java
private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
}
```

### `flush()`

영속성 컨텍스트의 변경 내용을 데이터베이스에 SQL로 반영한다.

```text
새 엔티티 → INSERT
변경된 엔티티 → UPDATE
삭제 엔티티 → DELETE
```

`flush()`는 커밋이 아니다.
SQL을 데이터베이스에 전달하지만 현재 테스트 트랜잭션은 계속 유지된다.
테스트가 끝나면 전체 변경은 롤백된다.

### `clear()`

영속성 컨텍스트가 관리하던 모든 엔티티를 분리한다.
1차 캐시도 비워진다.

이후 Repository로 엔티티를 조회하면 데이터베이스를 대상으로 다시 SELECT가 실행된다.

### 왜 `clear()`만 호출하면 안 되는가?

아직 SQL로 반영되지 않은 변경이 있는 상태에서 `clear()`를 먼저 호출하면 관리 중이던 변경 내용이 사라질 수 있다.

따라서 다음 순서를 사용한다.

```text
flush: 변경 SQL 반영
clear: 영속성 컨텍스트 초기화
find: 데이터베이스에서 재조회
```

### 현재 테스트에서 증명하는 것

```java
gameService.updateGame(request, gameId, creatorId);

flushAndClear();

GameEntity updatedGame = gameRepository
        .findByGameIdAndDeletedDateTimeIsNull(gameId)
        .orElseThrow();

assertEquals("수정된 경기 제목", updatedGame.getTitle());
```

이 검증은 단순히 메모리 객체의 필드가 바뀌었다는 것보다 더 많은 내용을 확인한다.

```text
서비스 트랜잭션 안에서 엔티티가 변경됨
JPA 변경 감지가 UPDATE SQL을 생성함
변경값이 데이터베이스에 반영됨
Repository 재조회 결과에도 변경값이 존재함
```

---

## 8. 경기 서비스 통합 테스트에서 검증한 내용

### 경기 생성

경기 생성은 `game_entity`만 저장하면 끝나는 기능이 아니다.
생성자가 첫 번째 참가자로 함께 등록되어야 한다.

```text
경기 저장
생성자 참가 정보 저장
참가 상태 ACCEPT
경기 참가 인원 1명
```

두 테이블의 데이터를 함께 조회하여 하나의 트랜잭션에서 일관되게 저장됐는지 검증했다.

### 중복 일정 생성 실패

동일 장소에서 시간이 겹치는 경기를 생성하면 예외가 발생해야 한다.

예외 코드만 확인하지 않고 실패 전후의 경기 수와 참가 정보 수도 비교했다.

```java
long gameCountBefore = gameRepository.count();
long participationCountBefore = participantGameRepository.count();
```

이를 통해 검증 실패 이후 일부 데이터만 저장되는 문제가 없는지도 확인한다.

### 경기 수정

서비스가 조회한 영속 엔티티의 도메인 메서드를 호출하면 JPA 변경 감지가 UPDATE를 수행한다.

통합 테스트는 `flushAndClear()` 이후 재조회하여 실제 변경 감지 결과를 검증한다.

### 경기 삭제

현재 경기 삭제는 물리적인 행 삭제가 아니라 Soft Delete다.

```text
경기 deletedDateTime 기록
경기 참가 인원 0명 변경
모든 참가 상태 DELETE 변경
참가 정보 deletedDateTime 기록
일반 활성 경기 조회에서 제외
```

따라서 `findById()`로 삭제된 행의 상태를 확인하고,
활성 경기 전용 조회 결과에서는 제외됐는지 함께 검증한다.

---

## 9. 경기 참가 통합 테스트에서 검증한 내용

### 참가

참가 성공 시 두 Aggregate 상태가 함께 변경된다.

```text
ParticipantGameEntity 생성
상태 ACCEPT
acceptDateTime 기록
GameEntity participantCount 증가
```

### 참가 취소

참가 취소 시 참가 상태만 바꾸는 것이 아니라 경기 인원도 감소해야 한다.

```text
ACCEPT → CANCEL
canceledDateTime 기록
participantCount 감소
```

### 재참가

취소 후 다시 참가하면 참가 행을 계속 추가하지 않고 기존 참가 정보를 재사용한다.

현재 테스트는 다음 내용을 확인한다.

```text
최초 participationId와 재참가 participationId가 동일함
CANCEL 상태가 ACCEPT로 변경됨
canceledDateTime이 null로 초기화됨
참가 행의 전체 개수가 증가하지 않음
```

이 검증은 한 사용자가 같은 경기에 여러 참가 행을 만드는 중복 데이터 문제를 방지한다.

### 강퇴

경기 생성자가 참가자를 강퇴하면 다음 상태가 함께 반영되어야 한다.

```text
참가 상태 KICKOUT
kickoutDateTime 기록
경기 participantCount 감소
```

서비스 단위 테스트에서는 강퇴 이벤트 발행 내용을 확인하고,
통합 테스트에서는 데이터베이스 상태 변경을 중심으로 확인한다.

---

## 10. QueryDSL Repository 통합 테스트

### Repository를 별도로 테스트한 이유

동적 조회 쿼리는 Service 테스트만으로 모든 조합을 확인하기 어렵다.

현재 `GameQueryRepository.findGames()`에는 다음 책임이 있다.

```text
삭제 경기 제외
미래 경기 또는 선택 날짜 조회
제목·장소 키워드 검색
지역, 경기 형식, 장소 유형, 성별, 모집 상태 필터
시작 시간순 또는 최신순 정렬
페이징
전체 결과 수 조회
```

Repository 통합 테스트는 QueryDSL 코드가 실제 MySQL에서 의도한 결과를 반환하는지 확인한다.

### 동적 조건 검증

조건에 맞는 데이터와 비슷하지만 조건이 하나씩 다른 데이터를 함께 저장한다.

예를 들어 다음 데이터를 준비한다.

```text
키워드와 모든 조건이 일치하는 경기
지역만 다른 경기
경기 형식만 다른 경기
장소 유형만 다른 경기
```

이후 결과가 정확히 한 건인지 검증하면 각 조건이 AND로 올바르게 결합됐는지 확인할 수 있다.

### 정렬에 보조 기준이 필요한 이유

정렬 대상 값이 같은 행이 여러 개면 데이터베이스가 항상 같은 순서를 보장하지 않는다.

따라서 현재 쿼리는 ID를 두 번째 정렬 기준으로 사용한다.

```java
game.startDateTime.asc(),
game.gameId.asc()
```

```java
game.createdAt.desc(),
game.gameId.desc()
```

이렇게 하면 페이징 중 동일한 값이 존재해도 결과 순서가 안정적으로 유지된다.

---

## 11. `Pageable`, `Page`, `PageImpl`

### `Pageable`

조회하려는 페이지 번호와 한 페이지의 크기를 전달한다.

```java
PageRequest.of(0, 2)
```

의미는 다음과 같다.

```text
페이지 번호: 0
페이지 크기: 2
```

페이지 번호는 0부터 시작한다.

### Content Query

실제 화면에 표시할 데이터를 조회한다.

```java
.offset(pageable.getOffset())
.limit(pageable.getPageSize())
.fetch();
```

### Count Query

조건에 맞는 전체 데이터 수를 조회한다.

```java
jpaQueryFactory
        .select(game.count())
        .from(game)
        .where(builder)
        .fetchOne();
```

### `PageImpl`

조회 데이터, 페이지 요청 정보, 전체 데이터 수를 합쳐 `Page` 객체를 만든다.

```java
return new PageImpl<>(content, pageable, total);
```

이 정보로 다음 값을 계산할 수 있다.

```text
현재 페이지의 데이터 수
전체 데이터 수
전체 페이지 수
다음 페이지 존재 여부
```

따라서 페이징 테스트에서는 Content의 순서만 확인하지 않고 다음 값도 함께 확인한다.

```java
assertEquals(3, page.getTotalElements());
assertEquals(2, page.getTotalPages());
assertEquals(2, page.getNumberOfElements());
```

---

## 12. 과거 경기 Fixture 생성 방식

경기 도메인은 과거 시각에 새로운 경기를 생성하지 못하도록 검증한다.
하지만 완료 경기 조회 테스트에는 이미 종료된 경기 데이터가 필요하다.

도메인 검증을 Reflection으로 강제로 우회하면 실제 생성 규칙을 무시한 Fixture가 만들어진다.

따라서 기준 시각을 과거로 전달하여 당시에는 유효하게 생성된 경기로 표현할 수 있다.

```java
GameEntity.create(
        // 생략
        startDateTime,
        startDateTime.plusHours(2),
        // 생략
        startDateTime.minusDays(2)
);
```

의미는 다음과 같다.

```text
경기 생성 시점: 경기 시작 2일 전
현재 테스트 시점: 경기 종료 이후
```

이 방식은 도메인 정책을 깨지 않으면서 과거 경기 상태를 준비한다.

---

## 13. `AFTER_COMMIT` 이벤트 테스트 주의점

현재 경기 생성, 수정, 삭제, 강퇴 알림은 다음 형태의 리스너를 사용한다.

```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
```

이 리스너는 트랜잭션이 실제로 커밋된 뒤 실행된다.

하지만 `@Transactional` 테스트는 마지막에 커밋하지 않고 롤백한다.
따라서 일반적인 경기 통합 테스트에서는 `AFTER_COMMIT` 리스너가 실행되지 않는다.

```text
이벤트 발행
→ 테스트 트랜잭션 롤백
→ 커밋 없음
→ AFTER_COMMIT 리스너 실행 안 됨
```

### 현재 테스트의 역할 분리

서비스 단위 테스트에서는 `ApplicationEventPublisher`를 검증하여 이벤트 발행 여부와 내용을 확인한다.

```java
verify(eventPublisher).publishEvent(eventCaptor.capture());
```

리스너의 커밋 이후 동작까지 확인하려면 별도의 테스트가 필요하다.

### 실제 커밋을 발생시키는 방법

방법 1은 테스트 클래스에서 `@Transactional`을 제거하고 서비스 트랜잭션이 실제로 커밋되도록 하는 것이다.

방법 2는 Spring Test의 `TestTransaction`을 사용하는 것이다.

```java
TestTransaction.flagForCommit();
TestTransaction.end();
```

이 방식은 실제 데이터를 커밋하므로 테스트 종료 후 데이터를 직접 정리해야 한다.
알림과 Redis처럼 데이터베이스 롤백으로 복구되지 않는 외부 상태도 함께 정리해야 한다.

따라서 현재 경기 핵심 통합 테스트에는 억지로 포함하지 않고,
알림 패키지를 리팩터링할 때 이벤트 리스너 전용 통합 테스트로 분리하는 것이 적절하다.

---

## 14. Repository 통합 테스트가 증명하지 않는 것

Repository 통합 테스트가 성공해도 해당 쿼리가 빠르다는 의미는 아니다.

이 테스트가 증명하는 것은 기능적 정확성이다.

```text
조건에 맞는 결과가 반환됨
정렬 순서가 정확함
페이지 정보가 정확함
Count Query가 정확함
```

다음 내용은 별도의 성능 검증이 필요하다.

```text
인덱스가 실제로 사용되는가?
Full Table Scan이 발생하는가?
데이터가 많아져도 응답 시간이 유지되는가?
처리량과 지연 시간이 어느 정도인가?
```

현재 프로젝트에서는 이를 다음 도구로 분리해 확인한다.

```text
MySQL EXPLAIN / EXPLAIN ANALYZE
k6 부하 테스트
Prometheus 지표
Grafana 시각화
```

기능 테스트와 성능 테스트의 목적을 섞지 않는 것이 중요하다.

---

## 15. 현재 테스트 전략의 장점과 한계

### 장점

- 단위 테스트로 비즈니스 분기를 빠르게 검증한다.
- 실제 MySQL로 JPA와 QueryDSL 동작 차이를 줄였다.
- 공통 어노테이션으로 통합 테스트 설정 중복을 제거했다.
- `flushAndClear()`로 1차 캐시가 아닌 실제 저장 결과를 확인한다.
- 서비스 통합 테스트와 Repository 쿼리 테스트의 책임을 구분했다.
- 트랜잭션 롤백으로 테스트 데이터 격리를 유지한다.

### 한계

- Docker가 없으면 통합 테스트를 실행할 수 없다.
- 전체 Spring Context와 컨테이너 시작으로 단위 테스트보다 느리다.
- 현재 통합 테스트는 동시 참가 경쟁 상황을 검증하지 않는다.
- `AFTER_COMMIT` 알림 리스너의 실제 실행은 별도 테스트 대상이다.
- Repository 테스트 성공만으로 인덱스 사용과 성능을 보장할 수 없다.

---

## 16. 핵심 정리

```text
단위 테스트
→ 비즈니스 정책과 조건 분기를 빠르게 검증

서비스 통합 테스트
→ 여러 Entity와 Repository가 트랜잭션 안에서 함께 동작하는지 검증

Repository 통합 테스트
→ QueryDSL 조건, 정렬, 페이징, Count Query의 실제 동작 검증

API 테스트
→ URI, HTTP, JSON, Security 검증

성능 테스트
→ 실행 계획, 처리량, 응답 시간, 병목 검증
```

`flushAndClear()`의 핵심은 다음과 같다.

```text
flush
→ 영속성 컨텍스트의 변경을 SQL로 반영

clear
→ 1차 캐시를 비우고 엔티티를 분리

재조회
→ 데이터베이스에 실제 반영된 결과를 검증
```

통합 테스트를 많이 작성하는 것보다,
단위 테스트만으로 확인할 수 없는 경계에 통합 테스트를 배치하는 것이 중요하다.
