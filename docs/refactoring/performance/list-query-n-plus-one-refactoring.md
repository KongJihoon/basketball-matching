# 목록 조회 N+1 리팩터링

## 1. 개요

내 경기, 경기 참가자, 관리자 신고, 관리자 블랙리스트 목록의 Repository 조회와 DTO 변환 경로를 연결해 N+1 발생 위치를 확인했다.

조회 쿼리만으로는 문제가 보이지 않았지만, 트랜잭션 안에서 `Page.map()`이나 `stream()`으로 DTO를 만들 때 `LAZY` 연관관계의 일반 필드에 접근하면 항목별 추가 SELECT가 실행됐다. 각 조회에 필요한 단건 연관관계만 Fetch해 쿼리 수를 상수로 고정했다.

---

## 2. 문제 탐지 방법

요청 시작에서 종료까지 Hibernate `StatementInspector`가 SQL 유형별 실행 횟수를 누적하고, Micrometer `DistributionSummary`로 요청 경로·HTTP 메서드·쿼리 유형 태그를 기록했다.

```text
HTTP 요청 시작
 → ThreadLocal 컨텍스트 생성
 → StatementInspector가 SELECT 횟수 누적
 → HTTP 요청 종료
 → app.query.per_request 메트릭 기록
 → ThreadLocal 정리
```

페이지 크기를 1, 10, 20, 100으로 변경해 SELECT 횟수가 선형으로 증가하는지 확인했다. Grafana percentile은 히스토그램 보간값이므로, 정확한 쿼리 수 판정에는 요청 단위 측정값과 페이지 크기별 결과를 사용했다.

---

## 3. 내 경기 목록

`ParticipantGameEntity` 목록을 조회한 뒤 `MyUpcomingGameResponse`와 `MyCompletedGameResponse`를 만들며 `gameEntity`에 접근했다.

```text
개선 전: 사용자 검증 1 + 목록 1 + COUNT 1 + GameEntity N
개선 후: 사용자 검증 1 + Fetch Join 목록 1 + COUNT 1
```

QueryDSL content 쿼리에 `ManyToOne` 연관관계를 Fetch Join했다.

```java
.selectFrom(participation)
.join(participation.gameEntity, game).fetchJoin()
```

| 페이지 크기 | 개선 전 | 개선 후 |
|---:|---:|---:|
| 1 | 4 | 3 |
| 10 | 13 | 3 |
| 20 | 23 | 3 |
| 100 | 103 | 3 |

세부 부하 결과는 [개선 전 기준선](../../../performance/results/my-game-list-n-plus-one/before/summary.md)과 [개선 후 검증](../../../performance/results/my-game-list-n-plus-one/after/summary.md)에 보관한다.

---

## 4. 경기 참가자 목록

`GameParticipantListResponse` 변환은 `ParticipantGameEntity.userEntity`의 닉네임과 포지션을 읽는다. 서로 다른 참가자로 구성한 페이지에서 항목별 사용자 SELECT가 발생했다.

```java
@EntityGraph(attributePaths = "userEntity")
Page<ParticipantGameEntity>
findByGameEntity_GameIdAndParticipantGameStatus(
        Long gameId,
        ParticipantGameStatus status,
        Pageable pageable
);
```

| 페이지 크기 | 개선 전 | 개선 후 |
|---:|---:|---:|
| 1 | 5 | 4 |
| 10 | 14 | 4 |
| 20 | 24 | 4 |

개선 후에는 페이지 크기와 관계없이 SELECT 4회를 유지했다.

---

## 5. 관리자 신고 목록

`ReportListResponse` 변환은 경기 제목, 신고자 닉네임, 신고 대상 닉네임을 읽는다. 따라서 `gameEntity`, `reportUser`, `targetUser` 세 개의 `LAZY` 연관관계가 목록 크기에 따라 추가 쿼리를 발생시킬 수 있었다.

```java
@EntityGraph(attributePaths = {
        "gameEntity",
        "reportUser",
        "targetUser"
})
Page<ReportEntity> findAllByReportStatusOrderByReportedDateTimeDesc(
        ReportStatus reportStatus,
        Pageable pageable
);
```

`reviewedBy`는 DTO에서 사용하지 않으므로 Fetch 대상에서 제외했다. 서로 다른 경기·신고자·대상자를 연결한 신고 20건을 조회했을 때 관리자 검증, 목록, COUNT로 SELECT 3회를 유지했다.

초기 적용 전 측정은 빈 페이지에서 수행돼 유효한 기준선으로 사용하지 않았다. 따라서 신고 목록은 실측 감소율을 제시하지 않고, DTO 접근 경로 분석과 개선 후 쿼리 수 고정 검증으로 범위를 한정한다.

---

## 6. 관리자 블랙리스트 목록

`BlackListResponse` 변환은 제재 대상 사용자의 이메일과 닉네임을 읽는다. 반면 `reportEntity`와 `bannedBy`는 식별자만 읽으므로 Fetch 범위에서 제외했다.

```java
@EntityGraph(attributePaths = "userEntity")
Page<BlackListEntity> findAllByExpiresAtAfterOrderByBannedDateTimeDesc(
        LocalDateTime now,
        Pageable pageable
);

@EntityGraph(attributePaths = "userEntity")
Page<BlackListEntity> findAllByExpiresAtLessThanEqualOrderByBannedDateTimeDesc(
        LocalDateTime now,
        Pageable pageable
);
```

| 항목 | 개선 전 | 개선 후 |
|---|---:|---:|
| 페이지 크기 | 20 | 20 |
| SELECT | 23 | 3 |
| 항목별 사용자 SELECT | 20 | 0 |

활성·만료 목록이 서로 다른 파생 쿼리를 사용하므로 두 메서드 모두 동일한 `EntityGraph`를 적용했다.

---

## 7. 페이지네이션과 Fetch 범위

이번 변경은 컬렉션 연관관계를 Fetch Join하지 않는다. 목록의 기준 엔티티에서 단건으로 참조하는 `ToOne` 연관관계만 함께 조회하므로 다음 특성을 유지한다.

- 본문 결과 행이 연관 엔티티 수만큼 증가하지 않는다.
- DB에서 `offset`과 `limit`를 적용하는 기존 페이지네이션을 유지한다.
- COUNT 쿼리에 불필요한 Fetch Join을 추가하지 않는다.
- 엔티티의 전역 FetchType은 `LAZY`로 유지한다.

---

## 8. 검증 결과

| 대상 목록 | 개선 전 | 개선 후 | 판정 |
|---|---:|---:|---|
| 내 예정·지난 경기 100건 | 103 | 3 | N+1 제거 |
| 경기 참가자 20건 | 24 | 4 | N+1 제거 |
| 관리자 신고 20건 | 유효한 기준선 미확보 | 3 | 잠재 N+1 방지 |
| 관리자 블랙리스트 20건 | 23 | 3 | N+1 제거 |

경기 참가자와 블랙리스트는 페이지 항목 수만큼 추가되던 SELECT가 0회로 줄었다. 내 경기 목록은 요청당 SELECT가 103회에서 3회로 줄었고, 최대 200 RPS 스트레스 구간의 dropped iteration 4,445건이 0건으로 감소했다.

---

## 9. 트레이드오프와 후속 과제

`EntityGraph`는 파생 쿼리를 유지하면서 N+1을 제거할 수 있지만 응답 필드가 변경되면 Fetch 범위와 DTO 접근 경로를 함께 재검토해야 한다. 연관관계를 과도하게 포함하면 JOIN 행 크기와 DB·네트워크 전송량이 늘어날 수 있다.

후속으로 다음을 검토한다.

1. 페이지 크기가 증가해도 쿼리 수가 상수인지 통합 테스트로 고정한다.
2. 응답에 필요한 컬럼이 더 줄어들면 DTO Projection을 비교한다.
3. 컬렉션 Fetch Join이 필요해지면 2단계 ID 조회, Batch Fetch 또는 쿼리 분리를 먼저 검토한다.
4. 신고 목록은 적용 전 유효 기준선을 추가로 확보해 실측 감소폭을 보완한다.

---

## 10. 성과

- 목록 DTO의 연관관계 접근 경로를 기준으로 Fetch 범위를 최소화했다.
- 경기 참가자 20건의 SELECT를 24회에서 4회로 줄였다.
- 블랙리스트 20건의 SELECT를 23회에서 3회로 줄였다.
- 신고 20건 조회에서 세 개의 필요한 연관관계를 함께 조회하고 SELECT 3회를 유지했다.
- 페이지네이션과 전역 `LAZY` 정책을 유지하면서 조회 메서드별 N+1을 제거했다.
- 내 경기 목록의 기존 스트레스 테스트에서 200 RPS 처리와 dropped iteration 0건을 확인했다.

로딩 전략 선택 근거는 [Fetch Join과 EntityGraph 선택 ADR](../../adr/performance/n-plus-one-loading-strategy.md)에 별도로 기록한다.
