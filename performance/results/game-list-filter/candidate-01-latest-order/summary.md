# 최신순 인덱스 1차 후보 검증

## 1. 후보

```text
(deleted_date_time, created_at DESC, game_id DESC)
```

논리 삭제 조건을 동등 조건으로 사용한 뒤 정렬 컬럼 순서에 맞춰 전체 스캔과 filesort를 제거하려 했다.

## 2. 결과

| 쿼리 | 적용 전 중앙값 | 1차 후보 중앙값 | 변화 |
|---|---:|---:|---:|
| Content | 228ms | 0.128ms | 99.94% 감소 |
| COUNT | 75.1ms | 1,195ms | 약 15.9배 증가 |

Content Query는 인덱스 순서대로 처음 10건을 읽고 종료하면서 크게 개선됐다. 하지만 옵티마이저가 COUNT에도 새 인덱스를 선택했고, 인덱스에 없는 `start_date_time`을 확인하기 위해 대량의 테이블 행에 접근했다.

`Page` 응답은 Content와 COUNT를 함께 실행하므로 목록만 빠른 상태를 최종 개선으로 볼 수 없다. API 부하 테스트로 넘어가지 않고 후보를 기각했다.

## 3. 후속 결정

정렬 순서를 유지하면서 COUNT의 테이블 접근을 제거하기 위해 `start_date_time`을 마지막 컬럼으로 추가한다.

```text
(deleted_date_time, created_at DESC,
 game_id DESC, start_date_time)
```

`start_date_time`을 정렬 컬럼 앞에 두면 범위 조건 이후 컬럼으로 정렬을 지원할 수 없으므로 마지막에 배치한다. 범위 탐색보다는 커버링을 위한 컬럼이다.

## 4. 근거 자료

- 적용 SQL: [`../../../sql/game-list-filter/41-add-candidate-01-latest-order-index.sql`](../../../sql/game-list-filter/41-add-candidate-01-latest-order-index.sql)
- 실행계획 및 5회 측정: [`raw/`](./raw/)
- Content 실행계획: [`plan-after-latest-list.png`](./plan-after-latest-list.png)
- COUNT 실행계획: [`plan-after-latest-count.png`](./plan-after-latest-count.png)
