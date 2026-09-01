# 목록 조회 형태에 따라 Fetch Join과 EntityGraph 사용

## 문제 상황

목록 조회 쿼리는 대상 엔티티만 가져오지만, 응답 DTO 변환 과정에서 `LAZY` 연관관계의 일반 필드를 읽고 있었다. 이로 인해 내 경기, 경기 참가자, 관리자 신고, 관리자 블랙리스트 목록에서 페이지 항목 수에 비례하는 추가 SELECT 위험이 있었다.

목록 API는 페이지네이션을 유지해야 하며, 다른 사용 상황에서는 지연 로딩을 계속 사용해야 한다. 따라서 엔티티 연관관계를 전역 `EAGER`로 변경하지 않고 조회 메서드별 로딩 전략이 필요했다.

## 선택지

### 연관관계를 EAGER로 변경

구현은 간단하지만 해당 엔티티를 사용하는 모든 조회에 로딩 정책이 전파된다. 연관 데이터가 필요하지 않은 API에서도 불필요한 조회를 유발하므로 제외했다.

### Batch Fetch 설정

추가 SELECT를 배치 단위로 묶어 횟수를 줄일 수 있다. 그러나 N+1 접근 구조 자체는 남고 전역 설정의 영향 범위와 적정 배치 크기를 별도로 관리해야 한다.

### DTO Projection

응답에 필요한 컬럼만 직접 조회할 수 있어 읽기 비용을 더 줄일 수 있다. 다만 현재 DTO 변환 구조와 Repository 반환 타입의 변경 범위가 크고, 우선 해결할 문제는 불필요한 반복 SELECT였다.

### Fetch Join과 EntityGraph

모두 특정 조회에서만 필요한 연관관계를 함께 가져올 수 있다. Fetch Join은 QueryDSL 조회의 조인 의도를 명시적으로 표현하기 좋고, `EntityGraph`는 Spring Data JPA 파생 쿼리를 유지하면서 로딩 범위만 선언하기 좋다.

## 결정

조회 구조에 따라 두 방식을 구분해 사용한다.

- QueryDSL로 content·count 쿼리를 직접 구성하는 내 경기 목록은 `gameEntity` Fetch Join을 사용한다.
- Spring Data JPA 파생 쿼리를 사용하는 경기 참가자, 신고, 블랙리스트 목록은 `EntityGraph`를 사용한다.
- DTO가 일반 필드를 읽는 연관관계만 Fetch 대상에 포함한다.
- 프록시의 식별자만 읽는 연관관계는 실제 추가 SELECT가 확인되지 않는 한 Fetch 대상에서 제외한다.
- COUNT 쿼리에는 Fetch Join을 적용하지 않는다.

이번 대상은 모두 `ManyToOne` 또는 식별자 기반의 단건 연관관계이다. 컬렉션 Fetch Join을 사용하지 않으므로 본문 결과 행이 증가하지 않고 DB 페이지네이션을 유지할 수 있다.

## 결과

| 대상 목록 | 개선 방식 | 검증 결과 |
|---|---|---|
| 내 예정·지난 경기 | QueryDSL Fetch Join | 100건 조회 `103 → 3` |
| 경기 참가자 | `EntityGraph(userEntity)` | 20건 조회 `24 → 4` |
| 관리자 신고 | `EntityGraph(gameEntity, reportUser, targetUser)` | 연관 데이터 20건 조회 SELECT 3회 |
| 관리자 블랙리스트 | `EntityGraph(userEntity)` | 20건 조회 `23 → 3` |

목록 크기가 커져도 요청당 쿼리 수가 상수로 유지되는 구조로 변경했다. 다만 `EntityGraph`에 연관관계를 추가할수록 JOIN 크기와 전송 데이터가 커지므로, DTO가 실제로 사용하는 필드와 쿼리 수를 함께 확인해야 한다.

세부 구현과 검증 내용은 [목록 조회 N+1 리팩터링](../../refactoring/performance/list-query-n-plus-one-refactoring.md)에 기록한다.
