# 경기 수정 알림: 비동기 개선 전 성능 측정 및 연결 풀 고갈 분석

## 1. 측정 목적

경기 수정 요청에 포함된 동기 알림 처리 비용과 DB 연결 대기를 관측하고, 전용 비동기 Executor 도입 전 기준 자료를 확보한다.

```http
PATCH /api/v1/games/{gameId}
```

현재 처리 흐름은 다음과 같다.

```text
경기 수정 트랜잭션
→ 커밋
→ 같은 요청 스레드에서 AFTER_COMMIT 이벤트 처리
→ REQUIRES_NEW 트랜잭션 시작
→ 수신자별 사용자 조회 및 알림 저장·SSE 처리
→ HTTP 응답
```

이번 작업은 테스트 환경 구성과 개선 전 분석까지다. 비동기 처리 및 Executor 튜닝은 아직 적용하지 않았다.

## 2. 측정 환경

| 항목 | 값 |
|---|---|
| 측정일 | 2026-09-09 |
| 환경 | 로컬, Spring Profile `local,performance` |
| 도구 | k6, Prometheus, Grafana, jcmd |
| Hikari 최대 / 최소 유휴 연결 | 10 / 5 |
| Hikari 연결 획득 제한시간 | 30초 |
| k6 PATCH 제한시간 | 10초 |
| 수신자별 측정 | 1·10·50·99명, 1 RPS, 30초 |
| 부하 대상 경기 | 268~277, 경기별 수신자 99명 |
| 부하 단계 | 5 RPS 30초 → 10·20·30 RPS 각 60초 |
| 단계 간 간격 / gracefulStop | 15초 / 15초 |
| 부하 시나리오 VU | 각 시나리오 사전 할당 50, 최대 200 |
| 정량 결과 원본 | k6 summary JSON |

서로 다른 경기 10개에 순환 요청하며, 실행·시나리오·iteration별 고유 제목으로 알림 검증 대상을 구분한다. 부하 테스트의 계획 요청 수는 3,750건이다. 단계 간 간격은 서버의 잔여 작업 완료를 보장하지 않는다.

관련 스크립트:

- [데이터 생성 SQL](../../../sql/game-update-notification-async/10-seed-game-update-notification-dataset.sql)
- [k6 시나리오](../../../k6/game-update-notification-async-test.js)
- [실행 스크립트](../../../scripts/run-game-update-notification-async-test.sh)
- [알림 검증 SQL](../../../sql/game-update-notification-async/90-verify-notification-result.sql)

## 3. 수신자 수별 탐색 결과

| 수신자 | HTTP 성공 | 평균 | p95 | 알림 저장 | SQL 검증 |
|---|---:|---:|---:|---:|---|
| 1명 | 30 | 54.52ms | 77.86ms | 30 | PASS |
| 10명 | 30 | 80.27ms | 97.49ms | 300 | PASS |
| 50명 | 31 | 183.69ms | 244.69ms | 1,550 | PASS |
| 99명 | 31 | 212.68ms | 265.19ms | 3,069 | PASS |

수신자 수가 커질수록 요청 응답시간이 증가했다. 이 결과는 각 조건 1회 탐색이며, 실행 중 SQL 로그 비활성화 여부를 사전에 검증하지 않았으므로 최종 로그 OFF 기준선과 구분한다.

50·99명 측정에서는 시간 기반 실행으로 실제 31건이 기록됐다. 기존의 정확히 30건 조건은 실패했지만 HTTP 및 SQL 검증은 통과했다. 이후 스크립트는 실제 시도·성공 건수를 별도로 기록하고 계획 건수 하한 및 시도/성공 일치 여부를 검증하도록 변경했다.

원본은 [k6 결과](./k6/)와 [수신자별 SQL 검증 CSV](./raw/)에 보관한다.

## 4. 부하 테스트 결과

### 4.1 Run 01: SQL 상세 로그가 활성화된 탐색

- Test ID: `20260909T062317-99-52659`
- [JSON 원본](./k6/load-run-01-summary.json)
- [Grafana 캡처](./grafana/)

| 목표 요청률 | 시도 | HTTP 성공 | p95 |
|---|---:|---:|---:|
| 5 RPS | 151 | 151 | 150.71ms |
| 10 RPS | 562 | 376 | 10,001.08ms |
| 20 RPS | 1,010 | 0 | 10,001.17ms |
| 30 RPS | 1,173 | 0 | 10,001.00ms |

총 2,896건 시도, 527건 성공, dropped iteration 818건이다. 저장된 Hikari 캡처 구간에서 Active 10, Idle 0, Pending 최대 196을 관측했다. 캡처 범위가 전체 실행 구간과 같지 않으므로 전체 실행 최대값으로 단정하지 않는다.

서버 로그에서 `org.hibernate.SQL=DEBUG`, `org.hibernate.orm.jdbc.bind=TRACE` 출력이 확인됐다. 설정 파일의 OFF와 실제 동작이 달랐으며 정확한 덮어쓰기 원인은 미확정이다. 원본은 삭제하지 않고 로그 ON 탐색 자료로 보존한다.

### 4.2 Run 02: SQL 상세 로그 OFF 재측정

- Test ID: `20260909T064821-99-56159`
- [JSON 원본](./k6/load-run-02-summary.json)
- 제목 접두어: `PERF_UPDATE_20260909T064821-99-56159_`

다음 VM 옵션으로 재시작한 뒤 경기 목록 HTTP 200 및 SQL 상세 로그 미출력을 확인했다.

```text
-Dlogging.level.org.hibernate.SQL=OFF
-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF
-Dspring.jpa.show-sql=false
```

| 목표 요청률 | 시도 | HTTP 성공 | p95 |
|---|---:|---:|---:|
| 5 RPS | 150 | 150 | 275.75ms |
| 10 RPS | 569 | 424 | 10,000.33ms |
| 20 RPS | 1,051 | 0 | 10,000.45ms |
| 30 RPS | 1,173 | 0 | 10,001.02ms |

총 2,943건 시도, 574건 성공, dropped iteration 809건이다. SQL 상세 로그를 끈 상태에서도 연결 획득 실패가 재현됐으므로 로그 출력만으로 장애를 설명할 수 없다.

두 실행은 재시작·누적 데이터·워밍업 상태도 다르므로 수치 차이를 로그 비활성화의 순수 개선 효과로 계산하지 않는다. 약 10초 p95는 클라이언트 제한시간에 도달한 관측값이며 정상 처리 완료 지연이 아니다. 20·30 RPS는 앞 단계 잔여 요청의 영향을 받으므로 독립적인 한계 처리량 측정값으로 해석하지 않는다.

## 5. 서버 로그 및 스레드 덤프 분석

Run 02 서버 로그에서 다음 오류가 확인됐다.

```text
CannotCreateTransactionException: Could not open JPA EntityManager for transaction
SQLTransientConnectionException:
HikariPool-1 - Connection is not available, request timed out after 30003ms.
```

`GameService.updateGame`의 트랜잭션 시작뿐 아니라 `UpdateGameEventListener.handle`의 커밋 후 새 트랜잭션 시작에서도 실패했다. 별도로 나타난 `Broken pipe`는 응답 쓰기 실패이며, 선행 지연의 직접 원인으로 해석하지 않는다.

2026-09-09 15:54:29에 PID 55636의 `jcmd Thread.print -l` 덤프를 분석했다. HTTP 요청 스레드 200개 모두 `ConcurrentBag.borrow → HikariPool.getConnection`에서 대기했다.

| 호출 경로 | 스레드 수 |
|---|---:|
| 경기 수정 트랜잭션 시작 | 176 |
| 인증 관련 DB 접근 | 14 |
| 커밋 후 알림의 새 트랜잭션 시작 | 10 |
| 합계 | 200 |

알림 대기 스레드는 exec-14, 56, 69, 83, 97, 148, 149, 171, 174, 195다. 대표 스택을 호출 방향으로 정리하면 다음과 같다.

```text
GameService.updateGame
→ AbstractPlatformTransactionManager.processCommit
→ TransactionalApplicationListenerSynchronization.afterCompletion
→ UpdateGameEventListener.handle
→ AbstractPlatformTransactionManager.handleExistingTransaction
→ JpaTransactionManager.doBegin
→ HikariPool.getConnection
→ ConcurrentBag.borrow
```

원본 덤프는 분석 당시 `/private/tmp/game-update-run-02-threads.txt`에 있었다. 임시 경로이므로 장기 보관을 보장하지 않으며, 이 문서에는 집계 및 대표 경로를 남긴다. 전체 덤프·서버 로그를 저장소에 추가할 경우 민감정보를 점검한다.

## 6. 원인 판단 및 검증 한계

현재 리스너는 동기 `AFTER_COMMIT + REQUIRES_NEW`다. 커밋과 원래 트랜잭션 리소스 반환은 같은 시점이 아니며, 원래 연결 정리 전에 리스너가 추가 연결을 요구할 수 있다.

연결 풀 최대치와 같은 10개 요청이 커밋 후 새 연결을 기다리고 나머지 190개 요청도 연결을 얻지 못한 덤프는, 기존 연결을 보유한 요청들이 추가 연결을 기다리는 **커넥션 풀 수준의 교착성 고갈 패턴**을 강하게 뒷받침한다. MySQL 행 잠금 교착을 확인한 것은 아니며, 획득 타임아웃으로 일부 대기가 해소될 수 있다.

Spring도 REQUIRES_NEW가 외부 트랜잭션의 리소스를 유지하면서 새 연결을 요구해 풀 고갈 및 교착을 일으킬 수 있음을 설명한다.

- [Spring 트랜잭션 전파 공식 문서](https://docs.spring.io/spring/reference/7.1/data-access/transaction/declarative/tx-propagation.html)

추가 주의사항:

- HTTP 타임아웃은 서버 작업 취소나 롤백을 보장하지 않는다.
- 경기 수정 커밋 후 알림 트랜잭션 시작 실패가 확인되어 알림 누락 위험이 존재한다.
- Run 02의 `56,826`은 HTTP 성공 574건 × 99명으로 계산한 값이지 실제 알림 저장 확인값이 아니다.
- 두 부하 실행의 실제 알림 누락·중복 SQL 검증은 아직 완료하지 않았다. 성공 응답만을 분모로 정합성을 단정하지 않는다.
- dropped iteration은 시작하지 못한 작업이며 HTTP 실패나 알림 누락과 구분한다.
- 계획 요청 수와 실제 시도 수의 차이를 dropped 건수와 임의로 맞추지 않고 JSON 값을 각각 보존한다.

## 7. 다음 단계

1. Run 02 결과·로그·덤프 증거 보존 및 실제 알림 저장 상태 검증
2. AFTER_COMMIT 이후 전용 비동기 Executor로 작업을 넘기는 구조 설계
3. 요청 트랜잭션과 알림 저장 트랜잭션 분리, 불변 이벤트 데이터 전달 확인
4. 제한된 worker·queue 및 작업 거부·실패 관측 정책 결정
5. 롤백 시 미발송, 커밋 후 저장, 실패 및 중복 관련 기능 검증
6. 동일 로그 OFF 조건으로 재측정하고 HTTP p95뿐 아니라 알림 완료 지연·큐 대기·누락·Hikari Pending 비교
7. DB 연결 여유를 유지하며 Executor 동시 실행 수를 단계적으로 조정

현재는 **개선 전 병목 재현 및 분석 완료, 비동기 구현 미착수** 상태다. HTTP 응답만 빨라지고 알림 큐가 계속 쌓이는 결과를 개선 완료로 판단하지 않는다.
