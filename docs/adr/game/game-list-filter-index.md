# 경기 목록 필터 전용 인덱스 도입

## 문제 상황

50만 건의 경기 데이터에서 기본 및 날짜 목록은 기존 `(deleted_date_time, start_date_time)` 인덱스로 빠르게 조회됐다. 반면 지역 COUNT는 133ms, 지역·모집 상태·경기 형식 COUNT는 138ms가 걸렸고 두 쿼리 모두 50만 건을 전체 스캔했다.

목록 첫 페이지는 `LIMIT 10`으로 빠르게 끝났지만 `Page` 응답의 전체 개수를 구하는 COUNT에는 LIMIT이 없어 API p95가 각각 101.68ms, 133.88ms까지 증가했다.

## 선택지

### 삭제 여부를 선두에 둔 하나의 범용 인덱스

```text
(deleted_date_time, city_name, game_status,
 match_format, start_date_time, game_id)
```

지역 COUNT는 개선됐지만 최신순 목록 중앙값이 `244ms → 828ms`로 증가했다. 옵티마이저가 많은 인덱스 엔트리와 테이블 행을 조회한 뒤 filesort를 수행했기 때문이다.

### 지역 필터 전용 인덱스

```text
(city_name, game_status, match_format,
 deleted_date_time, start_date_time, game_id)
```

선택도가 있는 지역을 선두에 두어 이 인덱스의 사용 범위를 지역 필터로 제한한다. 기본 및 날짜 조회는 기존 목록 인덱스가 담당한다.

## 결정

지역 필터 전용 인덱스를 선택한다.

```java
@Index(
        name = "idx_game_list_filter",
        columnList = "city_name, game_status, match_format, "
                + "deleted_date_time, start_date_time, game_id"
)
```

모든 조회를 하나의 인덱스로 처리하지 않고 접근 패턴별 역할을 분리한다.

## 결과

| 시나리오 | COUNT 적용 전 | 적용 후 | API p95 적용 전 | 적용 후 |
|---|---:|---:|---:|---:|
| 서울 | 133ms | 44.3ms | 101.68ms | 31.62ms |
| 서울 + 모집 중 + 3대3 | 138ms | 22.0ms | 133.88ms | 26.48ms |

기본 및 날짜 조회에는 의미 있는 회귀가 없었다. 최신순은 기존부터 발생하던 전체 스캔과 filesort가 남아 있으므로 별도 성능 개선 대상으로 분리한다.

보조 인덱스 추가로 저장공간과 쓰기 비용이 증가하지만, 실제 사용 빈도가 높은 지역 필터의 COUNT 및 API 응답시간 개선 효과가 더 크다고 판단했다.

세부 실행계획과 원본 결과는 [`performance/results/game-list-filter`](../../../performance/results/game-list-filter/README.md)에 보관한다.
