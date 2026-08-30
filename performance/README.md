# Performance tests

이 디렉터리는 경기 목록 API의 성능 개선 과정을 재현하기 위한 자료를 관리한다.

## 대상 API

```http
GET /api/v1/games?page=0&size=10
```

## 디렉터리 구조

```text
performance/
├── README.md
├── grafana/
│   ├── k6-latency-after-index.png
│   ├── k6-latency-before-index.png
│   ├── k6-load-profile-after-index.png
│   ├── k6-load-profile-before-index.png
│   ├── spring-cpu-after-index.png
│   └── spring-cpu-before-index.png
├── k6/
│   └── game-list-index-test.js
├── sql/
│   ├── 10-seed-game-list-dataset.sql
│   ├── 20-baseline-game-list-before-index.sql
│   ├── 30-add-game-list-active-start-index.sql
│   └── 31-drop-game-list-active-start-index.sql
└── results/
    └── game-list/
        ├── before-index/
        │   ├── k6/
        │   │   └── run-01~03-summary.json
        │   ├── raw/
        │   │   ├── indexes-before.csv
        │   │   ├── list-explain.csv
        │   │   ├── count-explain.csv
        │   │   └── *-explain-analyze-run-01~05.csv
        │   ├── plan-before-count.png
        │   ├── plan-before-list.png
        │   └── summary.md
        └── after-index/
            ├── k6/
            │   └── run-01~03-summary.json
            ├── raw/
            │   ├── indexes-after.csv
            │   ├── list-explain.csv
            │   ├── count-explain.csv
            │   └── *-explain-analyze-run-01~05.csv
            ├── plan-after-count.png
            ├── plan-after-list.png
            └── summary.md
```

- `k6`: 인덱스 적용 전후에 동일하게 재사용할 API 부하 테스트 스크립트
- `grafana`: k6 부하 프로파일, API 지연시간, 서버 자원 지표 캡처
- `sql`: 테스트 데이터 생성 및 측정에 사용한 실행 가능한 SQL
- `results`: 단계별 측정 결과와 원본 증거
- `before-index`: 복합 인덱스 적용 전 Full Scan과 filesort가 발생한 기준선
- `after-index`: `(deleted_date_time, start_date_time)` 복합 인덱스 적용 후 실행 계획, DB 실행시간, k6 부하 테스트 및 Grafana 지표

## 인덱스 적용 결과

기본 경기 목록 조회 조건과 정렬에 맞춰 다음 복합 인덱스를 적용했다.

```sql
CREATE INDEX idx_game_list_active_start
    ON game_entity (deleted_date_time, start_date_time);
```

| 항목 | 적용 전 | 적용 후 | 개선 |
|---|---:|---:|---:|
| 목록 SQL 중앙값 | 254ms | 0.235ms | 99.91% 감소 |
| COUNT SQL 중앙값 | 123ms | 60.8ms | 50.57% 감소 |
| API 응답시간 p95 | 491.09ms | 44.83ms | 90.87% 감소 |

목록 SQL은 Index Range Scan으로 변경되어 Full Scan과 filesort가 제거됐다. COUNT SQL은 정확한 전체 개수를 계산하기 위해 조건에 맞는 인덱스 엔트리를 계속 읽지만, 테이블 전체 스캔에서 커버링 인덱스 스캔으로 개선됐다.

- [인덱스 적용 전 기준선](./results/game-list/before-index/summary.md)
- [인덱스 적용 후 검증 결과](./results/game-list/after-index/summary.md)
- [인덱스 적용 SQL](./sql/30-add-game-list-active-start-index.sql)
- [인덱스 롤백 SQL](./sql/31-drop-game-list-active-start-index.sql)

## 경기 목록 필터 인덱스

기본 목록 인덱스 적용 후 지역, 경기 상태, 경기 형식 조합을 대상으로 별도 실험을 진행했다. 삭제 여부를 선두에 둔 1차 후보의 최신순 회귀를 확인하고, 지역을 선두로 이동한 필터 전용 인덱스를 최종 선택했다.

```sql
CREATE INDEX idx_game_list_filter
    ON game_entity (
        city_name,
        game_status,
        match_format,
        deleted_date_time,
        start_date_time,
        game_id
    );
```

| 시나리오 | COUNT 적용 전 | 적용 후 | API p95 적용 전 | 적용 후 |
|---|---:|---:|---:|---:|
| 서울 | 133ms | 44.3ms | 101.68ms | 31.62ms |
| 서울 + 모집 중 + 3대3 | 138ms | 22.0ms | 133.88ms | 26.48ms |

- [필터 인덱스 실험 개요](./results/game-list-filter/README.md)
- [1차 후보 기각 근거](./results/game-list-filter/candidate-01-deleted-first/summary.md)
- [최종 인덱스 검증 결과](./results/game-list-filter/after-index/summary.md)
- [설계 결정 기록](../docs/adr/game/game-list-filter-index.md)

## 경기 목록 최신순 인덱스

최신순 목록의 전체 스캔과 filesort를 제거하고 COUNT까지 커버링하도록 다음 인덱스를 적용했다.

```sql
CREATE INDEX idx_game_list_latest
    ON game_entity (
        deleted_date_time,
        created_at DESC,
        game_id DESC,
        start_date_time
    );
```

| 지표 | 적용 전 | 적용 후 |
|---|---:|---:|
| Content SQL 중앙값 | 228ms | 0.103ms |
| API 평균 중앙값 | 1,231.68ms | 48.28ms |
| API p95 중앙값 | 5,461.72ms | 51.32ms |
| dropped iteration | 484건 | 0건 |

- [적용 전 기준선](./results/game-list-filter/before-latest-index/summary.md)
- [1차 후보 기각 근거](./results/game-list-filter/candidate-01-latest-order/summary.md)
- [최종 검증 결과](./results/game-list-filter/after-latest-index/summary.md)
- [설계 결정 기록](../docs/adr/game/game-list-latest-index.md)

## 측정 원칙

1. 인덱스 적용 전후에 같은 데이터와 같은 Hibernate 바인딩 값을 사용한다.
2. 목록 SQL과 COUNT SQL을 별도로 측정한다.
3. `EXPLAIN`과 `EXPLAIN ANALYZE` 원본을 모두 보관한다.
4. `EXPLAIN ANALYZE`는 워밍업 후 5회 측정하고 중앙값을 비교한다.
5. DB 측정과 k6 API 측정 결과를 혼동하지 않는다.
6. 경기 목록 API 부하 테스트는 적용 전후 모두 20 RPS, 3분, 3회 조건으로 수행한다.
7. k6 JSON을 정량 비교 원본으로 사용하고 Grafana 캡처는 자원 추세를 설명하는 시각 자료로 사용한다.

## 경기 참가 동시성 개선

정원 6명인 경기에 생성자 1명이 참가한 상태에서 일반 사용자 100명이 동시에 참가하는 시나리오를 검증했다. 락이 없을 때는 초과 승인과 집계 불일치, HTTP 500이 발생했다. 낙관적 락 무재시도는 DB 정합성을 회복했지만 충돌 요청 39건이 시스템 오류로 끝났고, 비관적 쓰기 락은 모든 초과 요청을 정상적인 도메인 응답으로 처리했다.

| 항목 | 락 적용 전 | 낙관적 락 무재시도 | 비관적 락 적용 후 |
|---|---:|---:|---:|
| 성공한 일반 참가 | 12건 | 5건 | 5건 |
| 정원 초과 정상 거절 | 52건 | 56건 | 95건 |
| 시스템 오류 | 36건 | 39건 | 0건 |
| 실제 `ACCEPT` | 13명 | 6명 | 6명 |
| 초과 승인 | 7명 | 0명 | 0명 |
| DB 정합성 판정 | `FAIL` | `PASS` | `PASS` |
| 최종 선택 | 기각 | 기각 | **채택** |

- [락 적용 전 기준선](./results/game-participation-concurrency/before-lock/summary.md)
- [낙관적 락 무재시도 비교 실험](./results/game-participation-concurrency/optimistic-lock-no-retry/summary.md)
- [비관적 락 적용 후 검증](./results/game-participation-concurrency/pessimistic-lock/summary.md)
- [비관적 락 200·500·1,000명 부하 한계 탐색](./results/game-participation-concurrency/pessimistic-lock-load-limit/summary.md)
- [동시성 제어 방식 결정 기록](../docs/adr/game/game-participation-concurrency-lock.md)
- [k6 동시 참가 스크립트](./k6/game-participation-concurrency-test.js)
- [테스트 실행 스크립트](./scripts/run-game-participation-concurrency-test.sh)
- [테스트 경기 준비 SQL](./sql/game-participation-concurrency/20-prepare-participation-game.sql)
- [정합성 검증 SQL](./sql/game-participation-concurrency/90-verify-participation-result.sql)

### 비관적 락 단계별 부하 결과

비관적 락을 최종 선택한 뒤 같은 경기의 남은 5자리에 요청을 집중시키는 규모를 200명, 500명, 1,000명으로 높였다.

| VU | 참가 성공 | 정상 거절 | 시스템 오류 | p95 | DB 판정 |
|---:|---:|---:|---:|---:|---|
| 200 | 5 | 195 | 0 | 502.17ms | `PASS` |
| 500 | 5 | 495 | 0 | 817.57ms | `PASS` |
| 1,000 | 5 | 995 | 0 | 2,024.20ms | `PASS` |

HikariCP 최대 크기 10에서 1,000 VU 테스트의 Active Connection은 최대 10, Pending Connection은 최대 198이었고 Connection Timeout은 0건이었다. 요청 규모에 따라 꼬리 지연은 증가했지만 검증한 1,000 VU 범위에서 정합성과 정상 응답을 유지했다. 이는 확인한 상한이며 실제 시스템 파괴 지점이나 운영 수용량을 의미하지 않는다.
