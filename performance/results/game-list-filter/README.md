# 경기 목록 필터 인덱스 성능 테스트

## 1. 목적

50만 건의 경기 데이터를 기준으로 경기 목록 API의 필터와 정렬 조합별 실행계획을 확인하고, 실제 병목에 맞는 인덱스를 설계한 뒤 같은 조건으로 개선 효과를 검증한다.

```http
GET /api/v1/games?page=0&size=10
```

일반 B-Tree 인덱스를 활용하기 어려운 `%keyword%` 검색은 이번 실험에서 제외하고 별도 검색 실험으로 분리한다.

## 2. 측정 시나리오

| 구분 | 조건 | 목적 |
|---|---|---|
| A | 기본 시작 시간순 | 기존 목록 인덱스 기준선 확인 |
| B | 날짜 필터 | 시간 범위 조건의 인덱스 효율 확인 |
| C | 서울 + 시작 시간순 | 지역 필터와 COUNT 병목 확인 |
| D | 서울 + 모집 중 + 3대3 | 복합 필터 선택도와 COUNT 병목 확인 |
| E | 최신순 | `created_at` 정렬과 filesort 병목 확인 |

## 3. 디렉터리 구조

```text
game-list-filter/
├── README.md
├── before-index/
│   ├── grafana/
│   │   └── 시나리오별 run-02 캡처
│   ├── k6/
│   │   ├── smoke/
│   │   ├── warmup/
│   │   └── baseline/
│   ├── raw/
│   │   ├── indexes-before.csv
│   │   ├── *-explain.csv
│   │   └── *-explain-analyze-run-01~05.csv
│   ├── plan-before-*.png
│   └── summary.md
└── after-index/
    ├── grafana/
    ├── k6/
    ├── raw/
    ├── plan-after-*.png
    └── summary.md
```

관련 실행 파일:

- k6: [`../../k6/game-list-filter-test.js`](../../k6/game-list-filter-test.js)
- 실행 스크립트: [`../../scripts/run-game-list-filter-test.sh`](../../scripts/run-game-list-filter-test.sh)
- 데이터 생성: [`../../sql/game-list-filter/10-seed-filter-dataset.sql`](../../sql/game-list-filter/10-seed-filter-dataset.sql)
- 적용 전 SQL: [`../../sql/game-list-filter/20-before-index-baseline.sql`](../../sql/game-list-filter/20-before-index-baseline.sql)

## 4. 공통 측정 조건

| 항목 | 값 |
|---|---:|
| 경기 데이터 | 500,000건 |
| 페이지 | 0 |
| 페이지 크기 | 10 |
| DB 측정 | 워밍업 후 `EXPLAIN ANALYZE` 5회 |
| API executor | `constant-arrival-rate` |
| API 부하 | 20 RPS |
| API 측정시간 | 회차당 3분 |
| API 반복 | 시나리오별 3회 |
| 워밍업 | 5 RPS, 30초 |
| 관측 도구 | k6, Prometheus, Grafana |

## 5. 측정 원칙

1. 인덱스 적용 전후에 같은 데이터와 같은 요청 조건을 사용한다.
2. `Page` 조회의 Content Query와 COUNT Query를 분리해서 측정한다.
3. 일반 `EXPLAIN`과 `EXPLAIN ANALYZE` 원본을 모두 보관한다.
4. DB 실행시간은 5회 측정 후 중앙값으로 비교한다.
5. API 지연시간은 k6 3회 결과로 비교한다.
6. SQL과 Bind TRACE 로그는 SQL 수집 때만 활성화하고 부하 테스트에서는 비활성화한다.
7. JSON과 CSV를 정량 근거로 사용하고 Grafana 이미지는 부하 및 자원 추세의 시각 자료로 사용한다.
8. 인덱스는 후보를 한 번에 모두 추가하지 않고, 실행계획으로 사용 여부를 검증한다.

## 6. 현재 진행 상태

- [x] 50만 건 필터 데이터셋 구성
- [x] 5개 시나리오 스모크 테스트
- [x] 적용 전 DB 실행계획 및 5회 측정
- [x] 적용 전 k6 20 RPS, 3분, 3회 측정
- [x] 적용 전 Grafana 증거 수집
- [ ] 후보 인덱스 적용
- [ ] 적용 후 동일 조건 재측정
- [ ] 전후 결과 및 트레이드오프 정리

적용 전 분석 결과는 [`before-index/summary.md`](./before-index/summary.md)에서 확인한다.
