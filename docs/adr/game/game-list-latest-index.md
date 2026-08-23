# 경기 목록 최신순 전용 인덱스 도입

## 문제 상황

50만 건 데이터에서 최신순 목록은 전체 테이블을 스캔하고 미래 경기 약 33만 건을 filesort했다. 20 RPS 부하 테스트에서 p95 중앙값이 5,461.72ms였고 총 484건의 iteration 누락과 1건의 HTTP 실패가 발생했다.

## 선택지

### 정렬 컬럼만 포함

```text
(deleted_date_time, created_at DESC, game_id DESC)
```

Content는 `228ms → 0.128ms`로 개선됐지만 COUNT가 새 인덱스를 선택한 뒤 `start_date_time` 확인을 위해 테이블에 접근하면서 `75.1ms → 1,195ms`로 회귀했다.

### 시작 시각을 정렬 컬럼 뒤에 포함

```text
(deleted_date_time, created_at DESC,
 game_id DESC, start_date_time)
```

최신순 정렬 순서를 유지하면서 COUNT가 인덱스만으로 시작 시각 조건을 확인할 수 있다.

## 결정

두 번째 후보를 선택한다.

```java
@Index(
        name = "idx_game_list_latest",
        columnList = "deleted_date_time, created_at DESC, "
                + "game_id DESC, start_date_time"
)
```

`start_date_time`은 범위 탐색용이 아니라 COUNT의 테이블 접근을 막는 커버링 컬럼이다. 이를 `created_at` 앞에 배치하면 범위 조건 이후의 정렬 컬럼을 활용하기 어려우므로 마지막에 둔다.

## 결과

| 지표 | 적용 전 | 적용 후 |
|---|---:|---:|
| Content SQL 중앙값 | 228ms | 0.103ms |
| COUNT SQL 중앙값 | 75.1ms | 86.7ms |
| API 평균 중앙값 | 1,231.68ms | 48.28ms |
| API p95 중앙값 | 5,461.72ms | 51.32ms |
| dropped iteration | 484건 | 0건 |

COUNT에는 약 15.4%의 회귀가 있지만 Content와 COUNT 합계는 약 71.4% 감소했고 API 포화 상태가 해소됐다. 전체 요청 비용을 기준으로 인덱스를 채택한다.

보조 인덱스의 저장공간과 쓰기 비용은 증가한다. 또한 첫 페이지 중심의 결과이므로 깊은 페이지는 커서 기반 페이지네이션과 별도로 비교한다.

세부 결과와 원본 자료는 [최신순 최종 검증 문서](../../../performance/results/game-list-filter/after-latest-index/summary.md)에 보관한다.
