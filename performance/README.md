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
│   ├── k6-latency-before-index.png
│   ├── k6-load-profile-before-index.png
│   └── spring-cpu-before-index.png
├── k6/
│   └── game-list-index-test.js
├── sql/
│   ├── 10-seed-game-list-dataset.sql
│   └── 20-baseline-game-list-before-index.sql
└── results/
    └── game-list/
        └── before-index/
            ├── k6/
            │   ├── run-01-summary.json
            │   ├── run-02-summary.json
            │   └── run-03-summary.json
            ├── raw/
            ├── plan-before-count.png
            ├── plan-before-list.png
            ├── summary.md
```

- `k6`: 인덱스 적용 전후에 동일하게 재사용할 API 부하 테스트 스크립트
- `grafana`: k6 부하 프로파일, API 지연시간, 서버 자원 지표 캡처
- `sql`: 테스트 데이터 생성 및 측정에 사용한 실행 가능한 SQL
- `results`: 단계별 측정 결과와 원본 증거
- `before-index`: 인덱스 적용 전 기준선
- `after-index`: 추후 동일한 구조로 추가할 인덱스 적용 후 결과

## 측정 원칙

1. 인덱스 적용 전후에 같은 데이터와 같은 Hibernate 바인딩 값을 사용한다.
2. 목록 SQL과 COUNT SQL을 별도로 측정한다.
3. `EXPLAIN`과 `EXPLAIN ANALYZE` 원본을 모두 보관한다.
4. `EXPLAIN ANALYZE`는 워밍업 후 5회 측정하고 중앙값을 비교한다.
5. DB 측정과 k6 API 측정 결과를 혼동하지 않는다.
6. API 부하 테스트는 적용 전후 모두 20 RPS, 3분, 3회 조건으로 수행한다.
7. k6 JSON을 정량 비교 원본으로 사용하고 Grafana 캡처는 자원 추세를 설명하는 시각 자료로 사용한다.
