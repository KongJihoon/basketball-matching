# 🏀 농구 매칭 서비스

> 농구 취미를 공유하는 백엔드 프로젝트


- 농구는 우리나라에서 비교적 대중적인 스포츠가 아니기 때문에 농구 경기를 운영하기 위한 최소 인원을 모으기 어렵다.
개인이 농구를 하려면 농구장에 직접가서 사람들이 모이기를 기다려야 한다.
- 따라서 회원가입, 로그인, 경기 생성 및 신청, 매칭 관리, 평가/리뷰 등 농구 경기 매칭 서비스에 필요한 주요 기능을 구현하였습니다.

### 🗓️개발 기간
> - 2025/10/05 ~ 2025/11/15


---
## 1️⃣ ERD
![](https://github.com/KongJihoon/basketball-matching/blob/main/docs/erd/erd01.png?raw=true)


## 🛠 기술 스택

### 👨‍💻 Backend
- Java 17  
- Spring Boot 3.x  
- Spring Data JPA  
- Spring Security & JWT (Access/Refresh Token)  
- QueryDSL (동적 검색/조건 쿼리)  
- JavaMailSender (이메일 인증)  
- OAuth2 (Kakao Login)  

### 💬 Real-time Communication
- Spring WebSocket + STOMP (경기방 채팅)  
- SSE (Server-Sent Events, 알림 이벤트 전송)  
- Redis Pub/Sub (멀티 인스턴스 확장 시 메시지 브로커)  

### ⚙️ Infra & Database
- MySQL (주요 데이터 저장)  
- Redis (토큰 관리, 세션/매칭 대기열 캐싱)  
- AWS (EC2, RDS, S3)  
- Docker (개발/배포 환경 관리)  

### 🧰 Tools
- IntelliJ IDEA Ultimate  
- Postman / IntelliJ HTTP Client (API 테스트)  
- Git & GitHub  
- Notion (기획, 일정 관리)  

---

## 🧩 주요 기능

### 👤 사용자 (User)
- [x] 회원가입 (로그인 ID, 이메일, 닉네임 중복 검증)
- [x] 이메일 인증 (JavaMailSender) -> 이메일 인증 확인 후 Redis에 데이터 저장 -> 검증 후 회원가입
- [x] 로그인 (JWT 토큰 발급)
- [x] 카카오 소셜 로그인 (OAuth2)
- [x] 마이페이지 (회원 정보 조회/수정/탈퇴)

---

### 🏀 경기 (Match)
- [ ] 경기 등록 / 수정 / 삭제 (주최자 권한)
- [ ] 경기 목록 / 상세 조회
- [ ] 경기 신청 / 취소
- [ ] 매칭 확정 (참가자 확정 및 상태 관리)
- [ ] 경기 상태 관리 (`SCHEDULED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED` 등)

---

### 👥 매칭 & 참가자 관리
- [ ] 경기별 참가자 목록 조회
- [ ] 노쇼 신고 및 블랙리스트 처리
- [ ] 경기 결과 반영 (참가자 통계 업데이트)

---

### 💬 채팅 & 알림
- [ ] 경기방 채팅 (WebSocket + STOMP)
- [ ] 실시간 알림 (SSE 기반)
- [ ] 알림 종류: 경기 확정/취소, 신청 결과, 신고 처리 등
- [ ] Redis Pub/Sub을 통한 멀티 인스턴스 확장

---

### ⭐ 평가 & 랭크
- [ ] 경기 종료 후 평점 작성 (1인 1회)
- [ ] 참가자 실력 평가 반영 (레벨/랭크 시스템)
- [ ] 리뷰/평점 기반 사용자 통계 제공

---

### 📊 관리자 (Admin)
- [ ] 회원 관리 (정지/탈퇴/역할 변경)
- [ ] 경기 관리 (부적절한 경기 삭제)
- [ ] 신고 처리 및 로그 관리



