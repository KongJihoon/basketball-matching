# 경기 핵심 기능 리펙터링

## 1. 개요

기존 경기 기능은 `gmaeCreator`, `gameUser` 패키지로 분리되어있었다.

경기 생성자와 참가자라는 사용자 역할을 기준으로 패키지를 나눴지만,
하나의 경기 도메인과 참가 관계를 여러 패키지에서 함께 수정하면서 기능의 위치와 책임을 파악하기 어려웠다.

서비스도 인터페이스와 단일 구현체로 분리되어있었고,
경기 생성·조회·수정·삭제, 참가 신청과 승인, 평가와 랭크,
캐싱과 동시성 처리까지 여러 책임이 혼합되어 있었다.

이번 리팩터링의 목표는 경기 기능을 하나의 도메인으로 통합하고,
유스케이스별로 서비스 책임과 도메인 정책을 정리하는 것이다.


---

## 2. 기존 문제

### 역할을 기준으로 분리된 패키지

기존 경기 기능은 다음 패키지에 나뉘어 있었다. `gameCreator`, `gameUsers`

하지만 경기 생성과 참가 상태는 서로 독립적인 기능이 아니다.
경기 참가, 취소, 강퇴가 발생하면 다음 값이 함께 변경된다.
- 참가 상태
- 참가 처리 시각
- 경기의 현재 참가 인원
- 경기 모집 상태

하나의 도메인 규칙이 여러 패키지와 서비스에 분산되면서 변경할 때 확인해야 하는 범위가 넓어졌다.

### 단일 구현체를 위한 서비스 인터페이스

다음과 같은 인터페이스와 구현체 구조를 사용했다.

```text
GameService
└── GameServiceImpl

ParticipantGameService
└── ParticipantGameServiceImpl
```

실제 구현체는 하나뿐이었고 구현체 교체 요구사항도 존재하지 않는다.

인터페이스가 확장지점으로 사용되기보다 파일과 탐색 단계만 증가시키는 구조가 되었다.

### 서비스에 집중된 도메인 정책
기존 서비스는 다음 정책을 직접 판단하고 상태를 변경했다.
- 경기 생성 가능 시간
- 경기 진행 시간
- 경기 형식별 정원
- 참가 가능 여부
- 경기 수정 가능 시간
- 참가자가 존재할 때 변경할 수 없는 조건
- 경기 삭제 가능 시간
- 참가 인원과 모집 상태 변경

서비스에서 엔티티 필드를 직접 변경하면
다른 서비스가 동일한 규칙을 우회할 가능성이 있었다.

### DTO 명명규칙 불일치

기존 DTO는 하나의 클래스 안에 요청과 응답을 함께 두거나, 사용 목적을 명확히 알기 어려운 이름을 사용했다.
```text
CreateGameDto
EditGameDto
SearchGameDto
GameDto
```

DTO의 방향과 사용API를 확인하려면 내부 구현을 함께 읽어야 했다.

### 측정 전에 성능 최적화

기존 경기 조회에는 다음 기술이 적용되어 있었다.
- Redis 조회 캐시
- Redisson 분산 락
- N+1 문제를 해결하기 위한 조회 변경
- 경기 생성 동시성 제어

하지만 실제 병목을 측정하기 전에 기술을 먼저 적용하여
각 기술이 어떤 문제를 해결했고 어느 정도 개선했는지 설명하기 어려웠다.

---

## 3. 리펙터링 목표
- 경기 관련 코드를 game 패키지로 통합한다.
- 단일 구현체만 존재하는 서비스 인터페이스를 제거한다.
- 서비스는 유스케이스 흐름을 조정한다.
- 엔티티는 자신의 상태와 도메인 정책을 관리한다.
- Request와 Response DTO의 이름과 위치를 통일한다.
- 시간 의존 로직에 Clock을 사용한다.
- 경기 API를 리소스 중심 URI로 정리한다.
- 측정 근거가 없는 캐시와 락을 제거한다.
- 단위 테스트와 통합 테스트의 책임을 분리한다.

---

## 4. 변경된 패키지 구조

```text
game
├── controller
│   ├── GameController
│   ├── GameParticipantController
│   └── MyGameQueryController
├── domain
│   ├── GameEntity
│   └── ParticipantGameEntity
├── dto
│   ├── request
│   │   ├── CreateGameRequest
│   │   ├── UpdateGameRequest
│   │   └── GameListCondition
│   ├── response
│   │   ├── CreateGameResponse
│   │   ├── GameDetailResponse
│   │   ├── GameListResponse
│   │   ├── GameParticipantResponse
│   │   ├── GameParticipantListResponse
│   │   ├── MyUpcomingGameResponse
│   │   └── MyCompletedGameResponse
│   ├── GameCancelNotificationDto
│   └── UserWithdrawalGameResultDto
├── event
│   ├── GameCreateEvent
│   ├── UpdateGameEvent
│   ├── GameDeletedEvent
│   └── GameParticipantKickedOutEvent
├── repository
│   ├── GameRepository
│   ├── ParticipantGameRepository
│   └── query
│       └── GameQueryRepository
├── service
│   ├── GameService
│   ├── GameParticipantService
│   ├── MyGameService
│   └── UserWithdrawalGameService
└── type
```

Request와 Response DTO는 API 경계에 사용한다.
`GameCancelNotificationDto`와 `UserWithdrawalGameResultDto`는
Controller 요청·응답이 아니라 도메인 간 내부 데이터 전달에 사용하므로
request 또는 response 패키지에 포함하지 않았다.

---

## 5. 서비스 책임 분리

### GameService

경기의 생명주기를 담당한다
- 경기 생성
- 경기 상세 조회
- 경기 목록 조회
- 경기 수정
- 경기 삭제

### GameParticipantService

경기 참가 관계를 담당한다.
- 경기 참가
- 경기 취소
- 참가자 목록 조회
- 참가자 강퇴

### MyGameService

현재 사용자 기준 경기 목록을 조회한다.
- 내 예정 경기 조회
- 내 지난 경기 조회

### UserWithdrawalGameService

사용자 탈퇴 과정에서 필요한 경기 정리를 담당한다.
- 탈퇴자가 생성한 예정 경기 취소
- 관련 참가 상태 변경
- 탈퇴자가 참가한 다른 경기 정리
- 알림에 필요한 데이터 반환

서비스를 기술 단위가 아니라 유스케이스와 변경 이유를 기준으로 분리했다.

---

## 6. 경기 생성 리펙터링

경기 생성 흐름은 다음과 같다.
```text
활성 사용자 조회
→ 주소에서 지역 정보 변환
→ GameEntity 생성
→ 장소 및 시간 중복 검증
→ 경기 저장
→ 생성자 참가 정보 저장
→ 경기 생성 이벤트 발행
```

경기 생성 정책은 GameEntity.create()에서 검증한다.
```java
public static GameEntity create(
        // 생성 데이터
        UserEntity creator,
        LocalDateTime now
) {
    validateSchedule(
            startDateTime,
            endDateTime,
            now
    );

    matchFormat.validateHeadCount(
            headCount
    );

    // 엔티티 생성
}
```

서비스는 생성 순서를 조정하고
엔티티는 유효한 경기만 생성할 수 있도록 규칙을 보호한다.

### 생성 시 적용되는 규칙
- 경기 시작은 생성 시점으로부터 최소 24시간 이후
- 종료 시각은 시작 시간보다 이후
- 경기 진행 시간은 60분 이상 240분 이하
- 3대3 경기 정원은 6명 이상 100명 이하
- 5대5 경기 정원은 10명 이상 100명 이하
- 경기 생성자는 생성과 동시에 참가 확정 상태
- 생성자를 포함한 최초 참가 인원은 1명

생셩 결과를 `CreateGameResponse`로 반환하고, Controller는 생성된 경기 URI를 `Location`헤더에 담아 `201 Created`를 반환한다.

---

## 7. 경기 상세 및 목록 조회

### 경기 상세 조회

경기 상세 조회는 활성 경기만 조회한다.
```java
gameRepository
        .findByGameIdAndDeletedDateTimeIsNull(
                gameId
        );
```

엔티티를 Controller에 직접 반환하지 않고 `GameDetailResponse`로 변환한다.

이를 통해 API 응답 구조가 JPA엔티티 구조에 직접 의존하지 않도록 변경했다.

### 경기 목록 조회

목록 조건은 `GameListCondition`으로 통합했다.
```text
date
keyword
cityName
matchFormat
fieldStatus
matchGenderType
gameStatus
sortType
```

GET 요청의 조회 조건이므로 JSON Request Body 대신 `@ModelAttribute`를 이용해 Query Parameter로 전달한다.
```http request
GET /api/v1/games?keyword=잠실&cityName=SEOUL&sortType=START_TIME_ASC
```

`GameQueryRepositoty`는 값이 존재하는 조건만 `BooleanBuilder`에 추가

키워드는 다음 필드를 검색
- 경기 제목
- 경기 장소명

지원하는 정렬 방식은 다음과 같다.
- START_TIME_ASC: 경기 시작 시각 오름차순
- LATEST: 경기 생성 시각 내림차순

정렬 값이 없으면 `START_TIME_ASC`를 기본값으로 사용한다.
동일한 정렬 값을 가진 데이터의 페이지 순서가 변경되지 않도록
`gameId`를 보조 정렬 조건으로 사용한다.

---

## 8. 경기 수정 정책

경기 수정은 부분 변경 API이므로 `PATCH`를 사용한다.
```http request
PATCH /api/v1/games/{gameId}
```

### 서비스에서 검증하는 내용
- 요청자가 경기 생성자인지
- 경기 시작 24시간 전인지
- 하나 이상의 수정 필드가 존재하는지
- 시작과 종료 시각이 함께 전달되었는지
- 변경된 일정이 다른 경기와 겹치는지

### 엔티티에서 검증하는 내용
- 변경된 경기 시간이 유효한지
- 변경된 경기 형식과 정원이 유효한지
- 정원이 현재 참가 인원보다 작지 않은지
- 다른 참가자가 있을 때 경기 형식이나 참가 성별을 변경하는지

서비스는 사용자 권한과 Repository 조회가 필요한 검증을 담당하고,
엔티티는 자신의 현재 상태만으로 판단할 수 있는 규칙을 담당한다.

### 실제 변경이 있을 때만 이벤트 발행
요청에 값이 포함되어 있어도 기존 값과 같을 수 있다.

따라서 요청 필드의 존재 여부뿐 아니라
현재 엔티티 값과 비교해 실제 변경 여부를 판단한다.

```java
boolean actuallyChanged =
        isActuallyChanged(
                game,
                request
        );
```

실제 변경이 있고 알림 대상 참가자가 존재할 때만 `UpdateGameEvent`를 발행한다.

DB 트랜잭션 커밋 이후 이벤트 리스너가 참가자 알림을 저장한다.

---

## 9. 경기 삭제 정책

경기는 실제 행을 제거하지 않고 삭제 시각을 저장하는
Soft Delete 방식을 사용한다.

```java
this.deletedDateTime = deletedAt;
```

API에서는 리소스 삭제 요청이라는 의미에 맞춰 DELETE를 사용한다.

```http request
DELETE /api/v1/games/{gameId}
```

Soft Delete 저장 방식이고, HTTP Method는 클라이언트가 요청하는 행위의 의미를 표현하기 때문이다.

### 삭제 흐름
```text
활성 요청자 조회
→ 활성 경기 조회
→ 생성자 권한 검증
→ 삭제 가능 시간 검증
→ 경기 삭제 상태 변경
→ 참가 정보 DELETE 상태 변경
→ 경기 삭제 이벤트 발행
```

단, 경기 시작 1시간 이내에는 삭제할 수 없다.

삭제 시 생성자를 포함한 활성 참가자 정보를 모두 `DELETE`로 변경하고

알림 대상에서는 경기 생성자를 제외한다.

DB 커밋에 성공한 이후 `GameDeleteEventListener`가 참가자에게 경기 삭제 알림을 저장한다.

---

## 10. 시간 생성 방식
기존에는 서비스와 엔티티에서 LocalDateTime.now()를 직접 호출했다.
현재는 서비스가 주입받은 Clock으로 기준 시각을 생성한다.

```java
LocalDateTime now =
        LocalDateTime.now(clock);
```

생성된 시각은 엔티티 메서드에 전달한다.
```java
game.updateGame(
        // 변경 데이터
        now
);
```

이를 통해 엔티티가 시스템 시계에 의존하지 않고, 단위 테스트에서 `Clock.fixed`를 이용해 경계 시간을 재현할 수 있다.

---

## 11. 기존 최적화 제거와 재측정
기존에는 성능 병목을 확인하기 전에 캐시와 분산 락이 적용되어 있었다.
이번 리팩터링에서는 다음 설정을 제거했다.
- 경기 검색 Redis 캐시
- Redis Cache 설정
- 경기 생성 Redisson 분산 락
- Redisson 실행 유틸리티
  
최적화가 없는 상태를 다시 기준선으로 만들고,
실제 부하 테스트와 실행 계획을 이용해 병목을 확인했다.

경기 목록 조회는 50만 건 환경에서
전체 테이블 스캔과 filesort가 발생하는 것을 확인했다.

측정 이후 조회 조건과 정렬에 맞는 복합 인덱스를 적용했다.
상세한 측정 조건과 결과는 다음 문서에서 관리한다.
- [`performance/README.md`](../../../performance/README.md)

---

## 12. 테스트 전략

### 단위 테스트
Mockito와 고정 Clock을 사용해 서비스의 정책 분기와
협력 객체 호출을 검증했다.
주요 검증 대상:
- 경기 생성 및 일정 중복
- 경기 수정과 변경 이벤트
- 수정 필드 누락
- 경기 삭제와 참가 상태 변경
- 참가·취소·재참가·강퇴
- 권한 및 시간 정책
- 참가 인원과 상태 전이

### 통합 테스트
Testcontainers의 실제 MySQL을 사용해 다음을 검증했다.
- 경기와 생성자 참가 정보의 동시 저장
- 일정 중복 JPQL 쿼리
- JPA Dirty Checking을 통한 수정 반영
- 경기 Soft Delete와 참가 상태 변경
- 참가 취소 후 기존 참가 행 재사용
- QueryDSL 동적 검색과 필터
- 시작 시각 및 최신순 정렬
- 페이징과 COUNT 결과

단위 테스트는 정책과 분기를 검증하고,
통합 테스트는 JPA와 실제 쿼리의 동작을 검증하도록 구분했다.

---

## 13. 트레이드 오프와 남은 과제

### 현재 구조의 장점
- 경기 관련 코드의 위치를 쉽게 찾을 수 있다.
- 서비스별 변경 이유가 명확해졌다.
- 엔티티가 유효하지 않은 상태 변경을 차단한다.
- API DTO와 JPA 엔티티의 결합이 줄었다.
- 시간 경계 정책을 반복 가능한 테스트로 검증할 수 있다.
- 성능 개선 결과를 측정값으로 설명할 수 있다.

### 남은 과제
- 경기 참가 동시 요청에 대한 정원 초과 가능성 검증
- 실제 병목 확인 후 동시성 제어 방식 결정
- 목록 조회의 Page COUNT 비용과 Slice 적용 가능성 검토
- LATEST 정렬과 검색 조건에 필요한 추가 인덱스 검토
- N+1 문제 재현 후 조회별 해결 방식 결정
- 스키마 변경을 ddl-auto가 아닌 마이그레이션 도구로 관리
- 알림 실패에 대한 재시도 및 보상 전략 검토

---

## 💡성과
- gameCreator, gameUsers 패키지를 game 도메인으로 통합했다.
- 단일 구현체만 존재하던 서비스 인터페이스와 Impl 클래스를 제거했다.
- 경기 생명주기, 참가, 내 경기 조회 책임을 서비스별로 분리했다.
- Request와 Response DTO 명명 규칙을 통일했다.
- 경기 생성·수정·삭제 정책을 도메인 메서드로 이동했다.
- Clock을 이용해 시간 의존 로직을 테스트 가능한 구조로 변경했다.
- 측정 근거가 없던 캐시와 분산 락을 제거했다.
- 단위 테스트와 실제 MySQL 기반 통합 테스트를 복구했다.
- 경기 목록 병목을 실행 계획과 부하 테스트로 확인하고 개선했다.


