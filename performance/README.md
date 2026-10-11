# Performance tests

경기 목록 인덱스, 경기 참가 동시성, 목록 조회 N+1 개선을 k6·Prometheus·Grafana로 검증한다.

## 포트폴리오용 최신 결과

최신 본 테스트 수치, 개선 전·후 README, Grafana 캡처와 k6 JSON 링크는 다음 문서에 모아 두었다.

- [성능 개선 본 테스트 결과 허브](./results/README.md)

| 주제 | 개선 전 | 개선 후 |
|---|---|---|
| 기본 경기 목록 | [Before](./results/game-list-index/before/README.md) | [After](./results/game-list-index/after/README.md) |
| 복합 필터 | [Before](./results/game-list-filter-index/before/README.md) | [After](./results/game-list-filter-index/after/README.md) |
| 최신순 정렬 | [Before](./results/game-list-latest-index/before/README.md) | [After](./results/game-list-latest-index/after/README.md) |
| 경기 참가 동시성 | [Before](./results/game-participation-concurrency/before/README.md) | [After](./results/game-participation-concurrency/after/README.md), [1,000 VU](./results/game-participation-concurrency/1000-vu/README.md) |
| 내 경기 목록 N+1 | [Before](./results/my-game-list-n-plus-one/before/README.md) | [After](./results/my-game-list-n-plus-one/after/README.md) |

## 재현 자료

- k6 스크립트: [`k6`](./k6)
- 실행 스크립트: [`scripts`](./scripts)
- 데이터 생성·인덱스·정합성 검증 SQL: [`sql`](./sql)
- 인덱스 전·후 본 테스트: [기본 목록](./results/game-list-index), [복합 필터](./results/game-list-filter-index), [최신순](./results/game-list-latest-index)

## 측정 원칙

1. 개선 전·후에 동일한 데이터와 부하 조건을 사용한다.
2. 스모크 테스트와 워밍 후 본 테스트를 3회 수행한다.
3. API 비교는 k6 summary JSON을, DB 실행계획은 `EXPLAIN ANALYZE`를 정량 근거로 사용한다.
4. Grafana는 응답시간, 처리량, HikariCP, CPU 추세를 설명하는 시각 증빙으로 사용한다.
5. 동시성 테스트는 성능보다 DB 불변식과 비즈니스 응답 정확성을 우선한다.
6. 로컬 테스트 결과를 운영 최대 처리량으로 표현하지 않는다.
