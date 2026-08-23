# 경기 목록 필터 인덱스 1차 후보 검증

## 1. 후보

```text
(deleted_date_time, city_name, game_status,
 match_format, start_date_time, game_id)
```

논리 삭제 조건을 모든 목록 조회에서 사용하므로 `deleted_date_time`을 선두에 배치했다. 지역과 복합 필터 COUNT의 전체 테이블 스캔을 줄이는 것이 목적이었다.

## 2. 측정 결과

아래 값은 `EXPLAIN ANALYZE` 5회 측정의 중앙값이다.

| 시나리오 | 적용 전 | 1차 후보 | 결과 |
|---|---:|---:|---:|
| 서울 COUNT | 133ms | 39.8ms | 70.1% 감소 |
| 서울 + 모집 중 + 3대3 COUNT | 138ms | 24.1ms | 82.5% 감소 |
| 기본 COUNT | 70.6ms | 83.3ms | 18.0% 증가 |
| 최신순 목록 | 244ms | 828ms | 약 3.39배 증가 |
| 최신순 COUNT | 73.1ms | 85.3ms | 16.7% 증가 |

지역 필터에는 효과가 있었지만 필터를 사용하지 않는 기본 및 최신순 조회에서 옵티마이저가 이 인덱스를 선택했다. 특히 최신순 목록은 많은 인덱스 엔트리와 테이블 행을 조회한 뒤 filesort까지 수행해 기존 전체 스캔보다 느려졌다.

## 3. 판단

이 후보는 목표 시나리오만 보면 성공했지만 다른 주요 조회에 큰 회귀를 만들었다. 하나의 인덱스로 모든 목록 조회를 해결하려는 설계가 오히려 옵티마이저의 비효율적인 선택을 유도할 수 있음을 확인하고 기각했다.

다음 후보에서는 필터 전용 인덱스라는 역할을 분명히 하기 위해 선택도가 있는 `city_name`을 선두로 이동했다. 기본·날짜 조회는 기존 `(deleted_date_time, start_date_time)` 인덱스에 맡겼다.

## 4. 근거 자료

- 적용 SQL: [`../../../sql/game-list-filter/30-add-candidate-01-deleted-first-index.sql`](../../../sql/game-list-filter/30-add-candidate-01-deleted-first-index.sql)
- 원본 실행계획: [`raw/`](./raw/)
- 최신순 회귀 실행계획: [`plan-candidate-01-latest-list-regression.png`](./plan-candidate-01-latest-list-regression.png)
- 서울 COUNT 실행계획: [`plan-after-city-count.png`](./plan-after-city-count.png)
- 복합 COUNT 실행계획: [`plan-after-city-status-format-count.png`](./plan-after-city-status-format-count.png)
