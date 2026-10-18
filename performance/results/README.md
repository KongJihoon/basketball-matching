# 성능 개선 본 테스트 결과

이 문서는 포트폴리오에서 직접 링크할 수 있는 최신 성능 테스트 증빙 목록이다. 정량 판정은 k6 summary JSON을 기준으로 하고, Grafana 캡처는 부하·응답·자원 추세를 보여주는 보조 근거로 사용한다.

## 요약

| 주제 | 개선 전 | 개선 후 | 핵심 결과 |
|---|---|---|---|
| 경기 목록 기본 정렬 | [Before](./game-list-index/before/README.md) | [After](./game-list-index/after/README.md) | p95 `9,999.95ms → 39.10ms`, 누락 `4,980 → 0` |
| 복합 필터 | [Before](./game-list-filter-index/before/README.md) | [After](./game-list-filter-index/after/README.md) | p95 `4,772.04ms → 21.06ms`, 누락 `528 → 0` |
| 최신순 정렬 | [Before](./game-list-latest-index/before/README.md) | [After](./game-list-latest-index/after/README.md) | p95 `9,839.66ms → 40.70ms`, 누락 `4,404 → 0` |
| 경기 참가 동시성 | [Before](./game-participation-concurrency/before/README.md) | [After](./game-participation-concurrency/after/README.md) · [1,000 VU](./game-participation-concurrency/1000-vu/README.md) | 정합성 보장, 1,000 VU 정상 응답 `1,000/1,000` |
| 내 경기 목록 N+1 | [Before](./my-game-list-n-plus-one/before/README.md) | [After](./my-game-list-n-plus-one/after/README.md) | 요청당 SELECT `103 → 3`, 스트레스 p95 `4,744.71ms → 14.70ms` |

## 공통 측정 원칙

- 개선 전·후에 동일한 데이터와 부하 조건을 사용했다.
- 스모크 테스트와 워밍 후 본 테스트를 3회 수행했다.
- 3회 결과의 중앙값을 대표값으로 사용했다. 단, N+1 스트레스 테스트는 병목 탐색 1회 결과다.
- JSON에는 `setup_data`, JWT, Bearer 토큰을 남기지 않았다.
- 로컬 단일 서버 환경의 결과이며 운영 최대 처리량을 의미하지 않는다.
