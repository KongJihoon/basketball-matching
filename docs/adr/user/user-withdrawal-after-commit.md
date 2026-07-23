# 회원 탈퇴 후 처리를 AFTER_COMMIT 이벤트로 실행

## 문제 상황

회원 탈퇴는 다음 작업을 포함한다.
- 사용자 탈퇴 상태 저장
- 예정 경기 및 참가 상태 변경
- RefreshToken 삭제
- AccessToken 블랙리스트 등록
- 경기 취소 알림 저장 및 전송

DB 변경과 Redis 및 알림 처리를 하나의 서비스 메서드에서 순차적으로 처리하면, 외부 후처리 실패가 핵심 탈퇴 트랜잭션에 영향을 줄 수 있다.

반대로 DB 트랜잭션이 롤백되었는데 토큰이나 알림이 먼저 처리되는 문제도 발생할 수 있다.

---

## 결정
탈퇴 사용자와 경기 상태 변경은 하나의 DB 트랜잭션으로 처리한다.

트랜잭션 커밋 성공 이후 다음 작업을 이벤트 리스너에서 수행한다.
- 사용자 세션 폐기
- 경기 취소 알림 발송

```java
@TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT
)
@Transactional(
        propagation = Propagation.REQUIRES_NEW
)
```

### 장점
- 탈퇴 DB 트랜잭션이 성공한 경우에만 후처리를 수행한다.
- UserWithdrawalService가 Redis와 알림 구현을 직접 알지 않아도 된다.
- 핵심 탈퇴 처리와 부가 작업의 책임이 분리된다.
- 이벤트 리스너를 독립적으로 확장할 수 있다.

