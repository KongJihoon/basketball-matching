# 🏀 농구 매칭 서비스

> 농구 취미를 공유하는 백엔드 프로젝트


- 농구는 우리나라에서 비교적 대중적인 스포츠가 아니기 때문에 농구 경기를 운영하기 위한 최소 인원을 모으기 어렵다.
개인이 농구를 하려면 농구장에 직접가서 사람들이 모이기를 기다려야 한다.
- 따라서 회원가입, 로그인, 경기 생성 및 신청, 매칭 관리, 평가/리뷰 등 농구 경기 매칭 서비스에 필요한 주요 기능을 구현하였습니다.

### 🗓️개발 기간
> - 2025/10/05 ~ 2025/11/23


---
## 1️⃣ ERD
![](https://github.com/KongJihoon/basketball-matching/blob/main/docs/erd/ERD02.png?raw=true)

---


## 🏛️ 시스템 아키텍쳐

![](https://github.com/KongJihoon/basketball-matching/blob/main/docs/erd/systemArchitecture.png?raw=true)

## 📄 API 명세서
> [👉 농구 매칭 서비스 API 명세서 (Notion)](https://functional-booth-4c4.notion.site/API-27bc3809c4ba811eb9d9e3905d7893b6?source=copy_link)

### 핵심 API 요약

| 도메인 | 핵심 기능 요약 |
|:---:|:---|
| **User (인증/인가)** | JWT 기반 로그인/인증 구조를 구현하고 Redis를 활용한 Refresh Token 관리로 보안성과 재발급 흐름을 분리<br/>JavaMailSender 기반 이메일 인증을 통해 회원 가입 및 계정 신뢰성 확보 |
| **Game (경기 생성·조회)** | QueryDSL 기반 동적 쿼리를 적용하여 지역, 날짜, 성별, 실력 등 복합 조건 경기 검색 구현<br/>조건별 분기 쿼리를 단일 QueryDSL 로직으로 통합하여 가독성과 유지보수성 개선 |
| **Report / Blacklist (신고·관리자)** | 관리자가 신고 내역을 검토한 후 유저를 블랙리스트로 수동 등록하는 관리 기능 구현<br/>블랙리스트 등록 시 연관된 경기 및 신청 데이터의 상태를 자동 정합성 처리하여 데이터 무결성 유지 |
| **Notification (알림)** | SSE(Server-Sent Events) 기반 실시간 알림 기능 구현<br/>경기 신청 수락·거절 결과를 클라이언트에 즉시 전송하여 사용자 경험 개선 |


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
- SSE (Server-Sent Events, 알림 이벤트 전송)  
- Redis Pub/Sub

### ⚙️ Infra & Database
- MySQL (주요 데이터 저장)  
- Redis (토큰 관리, 캐싱, 분산 락 적용)  
- AWS (EC2, RDS)  
- Docker (개발/배포 환경 관리)  

### 🧰 Tools
- IntelliJ IDEA Ultimate  
- Swagger / Postman (API 명세 및 테스트)
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
- [x] 회원탈퇴

---

### 🏀 경기 (Match)
- [x] 경기 등록 / 수정 / 삭제 (주최자 권한)
   - THREE_ON_THREE(최소인원 : 6명, 최대인원 : 9명)
   - FIVE__ON_FIVE(최소인원 : 10명, 최대인원 : 15명)
   - 경기 등록 시 생성자 랭크로 경기 랭크 표시
- [x] 경기 목록 / 상세 조회
- [x] 경기 신청 / 취소
- [x] 매칭 확정 (참가자 확정 및 상태 관리)
- [x] 경기 상태 관리 (RECRUITING, CLOSED)
- [x] 현재 예정 경기 조회(QueryDSL)
   - N+1 문제 발생 -> fetchJoin 적용
- [x] 지난 경기 조회(QueryDSL)
   - N+1 문제 발생 -> fetchJoin 적용

---

### 👥 매칭 & 참가자 관리
- [x] 참가자 상태 관리(APPLY/ ACCEPT/ CANCEL 등)
   - 경기 수락 시 참가자 랭크 평균 계산 후 경기 랭크 변경
- [x] 경기별 참가자 목록 조회
- [x] 


---

### 💬알림

- [x] 실시간 알림 (SSE 기반)
- [x] 알림 종류: 경기 확정/취소, 신청 결과, 신고 처리 등
- [ ] Redis Pub/Sub을 통한 멀 경기 종료 후 참가자 평가 기능
- [x] 자기 자신 평가 방지
- [x] 동일 경기 중복 평가 방지
- [x] 참가자 실력 평가 반영 (레벨/랭크 시스템)
- [x] 최근 10경기 평균 점수 기반 레벨 자동 업데이트
   - 평가 받은 경기 < 5경기 -> Level = NONE
   - 10경기 중 평가 존재하는 경기들 평균

---

### 📊 관리자 (Admin)
- [x] 신고 목록 조회
- [x] 신고 단건 처리 (블랙리스트 등록)
- [x] 블랙 유저 중복 검증 (DB + Redis)
- [x] 블랙 유저 예정 경기 자동 정리 (참가자 상태/참가자 수 반영)



