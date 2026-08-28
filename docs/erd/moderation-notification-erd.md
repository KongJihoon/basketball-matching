# 신고·블랙리스트·알림 ERD와 데이터 관계

## 1. 문서 목적

이 문서는 신고, 관리자 검토, 블랙리스트 제재, 영속 알림에 사용되는 테이블 관계를 설명한다. 기존 `erd01.png`, `ERD02.png`를 대체하지 않고 이번 리팩터링에서 추가·정리된 도메인을 보충한다.

---

## 2. 논리 ERD

```mermaid
erDiagram
    USER_ENTITY ||--o{ REPORT_ENTITY : reports
    USER_ENTITY ||--o{ REPORT_ENTITY : is_targeted
    USER_ENTITY ||--o{ REPORT_ENTITY : reviews
    GAME_ENTITY ||--o{ REPORT_ENTITY : is_reported_in
    REPORT_ENTITY ||--o| BLACK_LIST_ENTITY : becomes_basis_of
    USER_ENTITY ||--o{ BLACK_LIST_ENTITY : is_banned
    USER_ENTITY ||--o{ BLACK_LIST_ENTITY : bans
    USER_ENTITY ||--o{ NOTIFICATION_ENTITY : receives

    REPORT_ENTITY {
        bigint report_id PK
        bigint report_user_id FK
        bigint target_user_id FK
        bigint game_entity_game_id FK
        varchar report_type
        varchar content
        datetime reported_date_time
        varchar report_status
        bigint reviewed_by_user_id FK
        varchar review_reason
        datetime reviewed_date_time
    }

    BLACK_LIST_ENTITY {
        bigint black_list_id PK
        bigint report_id FK_UK
        bigint user_id FK
        bigint banned_by_user_id FK
        datetime banned_date_time
        datetime expires_at
    }

    NOTIFICATION_ENTITY {
        bigint notification_id PK
        bigint receiver_id FK
        varchar notification_type
        varchar content
        boolean is_read
        datetime read_date_time
        datetime created_at
        datetime updated_at
    }
```

---

## 3. `report_entity`

하나의 신고는 신고자, 신고 대상, 근거 경기를 가진다. 검토 전에는 `reviewed_by_user_id`, `review_reason`, `reviewed_date_time`이 비어 있고 검토 후 관리자와 판단 근거가 저장된다.

`report_user_id`, `target_user_id`, `game_entity_game_id` 조합에는 `uq_report_reporter_target_game` 유일 제약조건이 있다. 같은 신고자가 같은 경기에서 같은 대상을 반복 신고하지 못하게 한다. 서로 다른 신고자가 같은 대상을 신고하는 것은 허용된다.

---

## 4. `black_list_entity`

블랙리스트는 승인된 `report_entity` 한 건을 근거로 생성한다. `report_id`는 `OneToOne` 관계이며 `uq_black_list_report` 유일 제약조건으로 같은 신고의 제재 재사용을 방지한다.

`user_id`는 실제 제재 대상이고 신고의 `target_user_id`에서 결정된다. `banned_by_user_id`는 제재를 적용한 관리자를 기록한다. `banned_date_time`과 `expires_at`으로 기간을 표현하며 별도의 상태 컬럼은 없다.

---

## 5. `notification_entity`

영속 알림은 수신자 한 명과 알림 종류, 내용, 읽음 상태를 저장한다. 생성 시 `is_read=false`이고 사용자가 읽으면 `true`와 최초 `read_date_time`을 기록한다.

Redis Pub/Sub으로만 전달되는 경기 생성 실시간 이벤트는 이 테이블에 저장되지 않는다. 따라서 이 ERD는 영속 알림 관계만 표현한다.

---

## 6. 관계의 업무 의미

```text
경기 참가 관계
  → 신고 자격 검증
  → report_entity(PENDING)
  → 관리자 검토(APPROVED 또는 REJECTED)
  → 승인된 report_entity 선택
  → black_list_entity 생성
  → notification_entity 저장 및 SSE 전송
```

신고와 블랙리스트가 분리되어 있으므로 승인된 모든 신고가 반드시 제재로 이어지는 것은 아니다. 알림 역시 제재의 결과를 사용자에게 전달하지만 제재 데이터의 원본은 아니다.

---

## 7. 삭제와 이력 관리

신고와 블랙리스트는 운영 판단과 제재 이력으로 사용되므로 만료됐다는 이유로 행을 자동 삭제하지 않는다. 블랙리스트 활성 여부는 `expires_at > 현재 시각` 조건으로 계산한다.

사용자 탈퇴나 경기 삭제 정책을 변경할 때는 이력 보존 요구와 외래 키 관계를 함께 검토해야 한다. 신고 근거를 유지할 필요가 있다면 물리 삭제보다 익명화 또는 별도 보관 정책이 필요하다.

---

## 8. 정합성 보호 장치

- 서비스 검증: 신고 자격, 승인 상태, 활성 제재 여부를 사용자 친화적 예외로 차단한다.
- 엔티티 검증: 신고 상태 전이와 제재 기간 같은 객체 불변식을 보호한다.
- DB 제약조건: 중복 신고 조합과 신고 재사용을 동시 요청에서도 최종 차단한다.
- 트랜잭션: 블랙리스트 저장과 경기 정리를 함께 커밋하거나 함께 롤백한다.

애플리케이션 검증만으로는 동시 요청 사이의 경쟁을 완전히 막지 못하므로 유일 제약조건을 함께 사용하는 것이 중요하다.

---

## 9. 조회 관점과 향후 인덱스

현재 주요 조회 조건은 다음과 같다.

- 관리자 신고 목록: `report_status`, 최신 신고 시각
- 활성 블랙리스트 검증: 사용자 또는 이메일, `expires_at > now`
- 블랙리스트 목록: 만료 시각 기준 활성/만료 분리
- 읽지 않은 알림: `receiver_id`, `is_read`, 최신 생성 시각

인덱스는 추정으로 추가하지 않고 실제 데이터 규모, 실행계획, 요청 빈도를 측정한 후 결정한다. 특히 낮은 카디널리티의 상태 컬럼 단독 인덱스보다 사용자 조건과 정렬 컬럼을 포함한 복합 인덱스를 후보로 검증한다.

---

## 10. 면접 확인 질문

- 신고와 블랙리스트를 한 테이블로 합치지 않은 이유는 무엇인가?
- 애플리케이션 중복 검증이 있는데 유일 제약조건도 둔 이유는 무엇인가?
- 블랙리스트 상태를 컬럼으로 저장하지 않은 이유는 무엇인가?
- 만료된 제재 행을 삭제하지 않는 이유는 무엇인가?
- 실시간 경기 생성 알림이 `notification_entity`에 없는 이유는 무엇인가?
- 각 외래 키를 지연 로딩으로 설정했을 때 목록 조회에서 어떤 성능 문제를 확인해야 하는가?
