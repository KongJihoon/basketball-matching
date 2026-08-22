# 경기 참가 리펙터링

## 1. 개요

기존 경기 참가 기능은 참가 신청 후 경기 생성자가 승인하거나 거절하는 방식이었다.

다음과 같은 상태와 서비스가 존재했다.
```text
APPLY
ACCEPT
CANCEL
REJECT
KICKOUT
DELETE
```

하지만 현재 프로젝트에는 결제, 참가비 정산, 별도 심사 기준이 없었다.

경기 생성자가 신청자를 일일이 승인해야 할 근거가 부족했고,
신청과 승인 기능이 서로 다른 Controller와 Service에 분산
단순히 참가 기능에 비해 구조가 복잡해졌다.

이번 리펙터링에서는 경기 참가 정책을 선착순 즉시 확정 방식으로 변경하고, 참가 상태 변경과 경기 인원 변경의 일관성을 도메인에서 관리하도록 개선했다.

---

## 2. 기존 문제

### 참가 신청과 승인의 분리

```text
사용자 참가 신청
→ APPLY 상태 저장
→ 경기 생성자가 신청 목록 조회
→ 승인 또는 거절
→ ACCEPT 또는 REJECT 상태 변경
→ 승인된 경우 경기 인원 증가
```

이 구조는 승인이 필요한 서비스에서는 적절할 수 있다.
하지만 현재 프로젝트에서는 다음 조건이 없었다.
- 참가비 결제
- 경기 생성자의 별도 심사 기준
- 사용자 자격 검증 서류
- 팀 전력 조정을 위한 승인 정책
- 참가 요청 만료 정책

실질적인 승인 기준 없이 승인 단계만 존재하여 사용자와 경기 생성자 모두 불필요한 작업을 수행해야 했다.

각 서비스는 현재 상태에 따라 다음을 반복해서 판단했다.
- 신청 가능한지
- 승인 가능한지
- 거절 가능한지
- 취소 가능한지
- 다시 신청할 수 있는지
- 강퇴할 수 있는지

상태가 많아질수록 잘못된 상태 전이를 허용할 가능성도 커졌다.


### 참가 상태와 경기 인원의 분리된 변경

참가 상태와 경기 인원을 서비스에서 각각 변경하면
한쪽만 변경되는 문제가 발생할 수 있다.

```text
참가 상태는 ACCEPT지만 경기 인원이 증가하지 않음
경기 인원은 감소했지만 참가 상태는 그대로 ACCEPT
```

경기 인원은 참가 상태에서 파생되는 값이므로
두 값은 하나의 상태 전이 안에서 함께 변경되어야 한다.

### 기능별 책임 위치 불명확

참가 신청, 취소, 승인, 강퇴, 참가자 목록 조회가
여러 Controller와 Service에 분산되어 있었다.
사용자 권한 기능과 경기 생성자 권한 기능도
명확하게 구분되지 않았다.


---

## 3. 정책 변경

경기 참가 정책을 다음과 같이 변경했다.

### 기존 정책

```text
참가 신청
→ APPLY
→ 경기 생성자 승인
→ ACCEPT
```

### 변경된 정책
```text
참가 요청
→ 참가 가능 여부 검증
→ 즉시 ACCEPT
```

참가 요청 시점에 정원이 남아 있고 참가 조건을 만족하면 즉시 참가가 확정

경기 생성자는 참가자를 사전에 승인하지 않지만
정책 위반 사용자나 함께 경기하기 어려운 사용자를 경기 시작 전에 강퇴할 수 있다.

---

## 4. 참가 상태 단순화

```java
public enum ParticipantGameStatus {
    ACCEPT,
    CANCEL,
    KICKOUT,
    DELETE
}
```

---

## 5. 상태 전이 규칙

허용된 상태 전이는 다음과 같다.

```text
신규 참가
null → ACCEPT

참가 취소
ACCEPT → CANCEL

취소 후 재참가
CANCEL → ACCEPT

경기 생성자 강퇴
ACCEPT → KICKOUT

경기 삭제 또는 탈퇴 정리
ACCEPT → DELETE
CANCEL → DELETE
```

허용되지 않는 상태전이

```text
ACCEPT → ACCEPT
CANCEL → CANCEL
KICKOUT → ACCEPT
DELETE → ACCEPT
KICKOUT → 다른 상태
DELETE → 다른 상태
```

상태전이 검증은 `ParticipantGameEntity`가 담당한다.

```java
private void validateTransition(
        ParticipantGameStatus oldStatus,
        ParticipantGameStatus newStatus
) {
    if (oldStatus == newStatus) {
        throw new CustomException(
                ALREADY_PRECESSED_STATUS
        );
    }

    switch (oldStatus) {
        case ACCEPT -> {
            if (newStatus != CANCEL
                    && newStatus != KICKOUT
                    && newStatus != DELETE) {
                throw new CustomException(
                        INVALID_STATUS_TRANSITION
                );
            }
        }

        case CANCEL -> {
            if (newStatus != ACCEPT
                    && newStatus != DELETE) {
                throw new CustomException(
                        INVALID_STATUS_TRANSITION
                );
            }
        }

        case KICKOUT, DELETE ->
                throw new CustomException(
                        ALREADY_FINAL_STATUS
                );
    }
}
```

서비스가 엔티티 필드를 직접 변경하지 않도록 하여
모든 상태 변경이 동일한 검증을 통과하게 했다.

---

## 6. 참가 인원 일관성
경기 인원을 차지하는 상태는 ACCEPT뿐이다.

```java
private boolean isOccupied(
        ParticipantGameStatus status
) {
    return status == ACCEPT;
}
```

상태를 변경하기 전과 변경한 후를 비교해
경기 참가 인원을 함께 변경한다.

```java
private void transitionTo(
        ParticipantGameStatus newStatus,
        LocalDateTime now
) {
    ParticipantGameStatus oldStatus =
            participantGameStatus;

    if (oldStatus != null) {
        validateTransition(
                oldStatus,
                newStatus
        );
    }

    boolean wasOccupied =
            isOccupied(oldStatus);

    boolean willBeOccupied =
            isOccupied(newStatus);

    if (!wasOccupied && willBeOccupied) {
        gameEntity.increaseParticipantCount();
    }

    if (wasOccupied && !willBeOccupied) {
        gameEntity.decreaseParticipantCount();
    }

    participantGameStatus = newStatus;

    applyTimestamp(newStatus, now);
}
```

이 구조를 통해 다음 변경이 하나의 도메인 동작으로 처리된다.

| 상태 변경 | 참가 인원 |
|---|---:|
| 신규 → `ACCEPT` | 1 증가 |
| `ACCEPT` → `CANCEL` | 1 감소 |
| `CANCEL` → `ACCEPT` | 1 증가 |
| `ACCEPT` → `KICKOUT` | 1 감소 |
| `ACCEPT` → `DELETE` | 1 감소 |

참가 인원 변경 후 `GameEntity`는 정원과 현재 인원을 비교해
모집 상태도 함께 갱신한다.

---

## 7. 경기 참가 흐름

```text
활성 사용자 조회
→ 블랙리스트 여부 확인
→ 활성 경기 조회
→ 기존 참가 관계 조회
→ 기존 참가 상태 검증
→ 경기 참가 조건 검증
→ 신규 참가 또는 재참가
→ 응답 DTO 반환
```

### 참가 조건

GameEntity.validateJoin()은 다음 정책을 검증한다.
- 경기 생성자는 자신의 경기에 다시 참가할 수 없음
- 모집 마감 경기는 참가할 수 없음
- 정원이 모두 찬 경기는 참가할 수 없음
- 경기 시작 30분 이내에는 참가할 수 없음
- 여성 전용 경기에 남성 사용자는 참가할 수 없음
- 남성 전용 경기에 여성 사용자는 참가할 수 없음
  

블랙리스트 검증은 별도 Repository 조회가 필요하므로
GameParticipantService가 담당한다.

---

## 8. 취소 후 재참가

참가를 취소한 사용자가 다시 참가할 때, 새로운 참가 엔티티를 생성하지 않는다.
기존 참가관계를 조회하고 `CANCEL`상태라면, 동일한 엔티티를 다시 `ACCEPT`로 변경한다.

```java
private ParticipantGameEntity joinOrRejoin(
        GameEntity game,
        UserEntity participant,
        ParticipantGameEntity existingParticipation,
        LocalDateTime joinedAt
) {
    if (existingParticipation == null) {
        ParticipantGameEntity participation =
                ParticipantGameEntity
                        .createParticipation(
                                game,
                                participant,
                                joinedAt
                        );

        return participantGameRepository.save(
                participation
        );
    }

    existingParticipation.join(joinedAt);

    return existingParticipation;
}
```

---

## 9. 참가 취소 정책

참가자는 자신의 참가 관게를 취소할 수 있다.

```http request
PATCH /api/v1/games/{gameId}/participations/me/cancel
```

취소는 참가 리소스를 물리적으로 제거하는 작업이 아니라 `ACCEPT`에서 `CANCEL`상태로 변경하는 작업이므로 `PATCH`사용

### 취소 조건
- 활성 사용자여야 함
- 활성 경기여야 함
- 참가 관계가 존재해야 함
- 경기 생성자는 참가 취소를 할 수 없음
- 경기 시작 30분 전까지만 취소 가능
- 현재 상태가 ACCEPT여야 함

경기 생성자는 참가만 취소하는 것이 아니라
경기 수정 또는 삭제 유스케이스를 사용해야 한다.


---

## 10. 참가자 목록 조회

경기 생성자는 현재 참가가 확정된 사용자 목록을 조회할 수 있다.
```http request
GET /api/v1/games/{gameId}/participants
```
조회 대상은 ACCEPT 상태로 제한한다.
응답에는 다음 정보가 포함된다.
- 참가 관계 ID
- 사용자 ID
- 닉네임
- 성별
- 포지션
- 참가 확정 시각
- 경기 생성자 여부

경기 참가자 목록은 개인정보가 포함될 수 있으므로 현재는 경기 생성자만 조회할 수 있다.
페이지 크기는 최대 100으로 제한하고, 참가 확정 시각 오름차순으로 조회한다.

---

## 11. 참가자 강퇴 정책

```http request
PATCH /api/v1/games/{gameId}/participants/{participantId}/kickout
```
강퇴 역시 참가 관계를 삭제하는 것이 아니라
ACCEPT에서 KICKOUT으로 상태를 변경하므로 PATCH를 사용했다.

### 강퇴 조건
- 요청자가 활성 사용자여야 함
- 요청자가 경기 생성자여야 함
- 활성 경기여야 함
- 참가 관계가 존재해야 함
- 경기 생성자 자신은 강퇴할 수 없음
- 경기 시작 1시간 전까지만 강퇴 가능
- 현재 참가 상태가 ACCEPT여야 함

경기 시작 직전에 강퇴당하면 참가자가 이미 이동 중일 수 있으므로
강퇴 제한 시간을 참가 취소보다 긴 1시간으로 설정했다.

### 강퇴 후 알림

```text
참가 상태 KICKOUT 변경
→ 경기 인원 감소
→ GameParticipantKickedOutEvent 발행
→ 트랜잭션 커밋
→ 강퇴 알림 저장
```

이벤트 리스너는 커밋 이후 새로운 트랜잭션에서 실행된다.
```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
@Transactional(
        propagation = Propagation.REQUIRES_NEW
)
```

---

## 12. 내 경기 조회 책임 분리

사용자 기준 조회는 `MyGameService`로 분리했다.

API 경로는 다음과 같다.
```http request
GET /api/v1/mypage/games/upcoming
GET /api/v1/mypage/games/completed
```

### 내 예정 경기
다음 조건을 만족하는 경기를 조회한다.
- 요청 사용자의 참가 상태가 ACCEPT
- 삭제되지 않은 경기
- 시작 시각이 현재보다 이후
- 시작 시각 오름차순

참가를 취소하거나 강퇴된 사용자의 경기는 조회되지 않는다.

### 내 지난 경기
다음 조건을 만족하는 경기를 조회한다.
- 요청 사용자의 참가 상태가 ACCEPT
- 삭제되지 않은 경기
- 종료 시각이 현재보다 이전 또는 같음
- 종료 시각 내림차순

응답 DTO에는 경기 정보와 함께
요청자가 경기 생성자인지도 포함한다.

---

## 13. Controller 책임 분리

경기 API는 다음 Controller로 분리했다.

### GameController

경기의 생명주기를 담당한다.
```text
POST   /api/v1/games
GET    /api/v1/games
GET    /api/v1/games/{gameId}
PATCH  /api/v1/games/{gameId}
DELETE /api/v1/games/{gameId}
```

### GameParticipantController

경기 참가 관계를 담당한다.
```text
POST  /api/v1/games/{gameId}/participations
PATCH /api/v1/games/{gameId}/participations/me/cancel
GET   /api/v1/games/{gameId}/participants
PATCH /api/v1/games/{gameId}/participants/{participantId}/kickout
```

### MyGameQueryController

현재 사용자의 경기 조회를 담당한다.
```text
GET /api/v1/mypage/games/upcoming
GET /api/v1/mypage/games/completed
```

---

## 14. 테스트 전략

### 단위 테스트
GameParticipantServiceUnitTest에서 다음을 검증했다.
- 신규 참가 성공
- 참가 인원 증가
- 취소 후 재참가
- 참가 취소와 인원 감소
- 경기 생성자의 참가 취소 실패
- 경기 생성자의 참가자 강퇴
- 생성자가 아닌 사용자의 강퇴 실패
- 이벤트 발행 내용

### 통합 테스트
GameParticipantServiceIntegrationTest에서는
실제 MySQL을 이용해 다음을 검증했다.
- 참가 관계 실제 저장
- 참가 상태와 경기 인원의 동시 반영
- 참가 취소 상태와 처리 시각 저장
- 취소 후 기존 참가 행 재사용
- 강퇴 상태와 처리 시각 저장
- JPA Dirty Checking을 통한 변경 반영

통합 테스트에서는 flushAndClear() 이후 다시 조회하여
영속성 컨텍스트의 1차 캐시가 아닌 실제 DB 결과를 확인했다.

---

## 15. 트레이드오프

### 선착순 참가 방식의 장점
- 참가 흐름이 단순하다.
- 불필요한 승인 대기 시간이 없다.
- 승인과 거절 API및 상태가 제거된다.
- 정원 경쟁 상황을 명확하게 재현할 수 있다.

### 선착순 참가 방식의 단점
- 경기 생성자가 참가자를 사전에 선별할 수 없다.
- 마지막 정원에 동시 요청이 들어오면 정원 초과 가능성이 있다.
- 강퇴 기능에 대한 정책과 알림이 중요해진다.
- 악성 사용자의 반복 참가 요청을 제한할 정책이 필요하다.

---

## 💡 성과
- 승인 근거가 없던 APPLY → ACCEPT 흐름을 제거했다.
- 참가 요청 즉시 ACCEPT되는 선착순 정책을 적용했다.
- 참가 상태를 네 가지로 단순화했다.
- 참가 상태와 경기 인원 변경을 하나의 도메인 동작으로 묶었다.
- 취소 후 새로운 행을 생성하지 않고 기존 참가 관계를 재사용했다.
- 참가자 목록과 강퇴 기능을 GameParticipantService로 통합했다.
- 사용자 기준 경기 조회를 MyGameService로 분리했다.
- 강퇴 상태 변경과 알림 저장의 트랜잭션 경계를 분리했다.
- 실제 MySQL 통합 테스트로 참가 상태와 경기 인원 정합성을 검증했다.