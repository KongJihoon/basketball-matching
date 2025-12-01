# 🏀 농구 매칭 서비스

> 농구 취미를 공유하는 백엔드 프로젝트


- 농구는 우리나라에서 비교적 대중적인 스포츠가 아니기 때문에 농구 경기를 운영하기 위한 최소 인원을 모으기 어렵다.
개인이 농구를 하려면 농구장에 직접가서 사람들이 모이기를 기다려야 한다.
- 따라서 회원가입, 로그인, 경기 생성 및 신청, 매칭 관리, 평가/리뷰 등 농구 경기 매칭 서비스에 필요한 주요 기능을 구현하였습니다.

### 🗓️개발 기간
> - 2025/10/05 ~ 2025/11/23


---
## 1️⃣ ERD
![](https://github.com/KongJihoon/basketball-matching/blob/main/docs/erd/erd01.png?raw=true)

---


## 📄 API 명세서

<details>
  <summary><strong>🙋‍♂️ 유저 API</strong></summary>

  <hr>

  <details>
    <summary>✅ 회원가입</summary>

   ### [POST] /api/v1/user/signup  
   <hr>

   ### 🔸 Request

   ```json
   {
      "email": "test@test.com",
      "password": "Test@1234",
      "checkPassword": "Test@1234",
      "nickname": "커리",
      "name": "서장훈",
      "birth": "2025-12-01",
      "phone": "010-1111-0000",
      "address": "서울특별시 강남구",
      "position": "NONE",
      "genderType": "MALE",
      "loginProvider": "LOCAL"
    }
   ```
   ### 🔸 Response (200)
   
   ```json
   {
      "message" : "회원가입에 성공하였습니다."
      "data" : {
         "email": "test@test.com",
         "nickname": "커리",
         "name": "서장훈",
         "birth": "2025-12-01",
         "phone": "010-1111-0000",
         "address": "서울특별시 강남구",
         "position": "NONE",
         "genderType": "MALE",
         "userType": "USER",
         "createdAt": "2025-12-01T11:11:15.614Z"
         
      }
   }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "EMAIL_NOT_VERIFIED",
         "errorMessage": "이메일 인증을 먼저 진행해주세요.",
         "details": null
      }   
   ```
    
  </details>

  <details>
  <summary>✅ 이메일 중복 확인</summary>
   
   ### [POST] /api/v1/user/check-email 
   
   <hr>
   
   ### 🔸 Query Parameters
   - **email**: test@test.com
     
   ### 🔸 Response (200)
     
   ```json
        {
           "success" : true,
           "message" : "사용가능한 이메일입니다."   
        }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "ALREADY_EXIST_EMAIL",
         "errorMessage": "이미 존재하는 이메일입니다.",
         "details": null
      }   
   ```
  </details>

  <details>
  <summary>✅ 닉네임 중복 확인</summary>
   
   ### [POST] /api/v1/user/check-nickname 
   
   <hr>
   
   ### 🔸 Query Parameters
   - **nickname**: testNickname 
     
   ### 🔸 Response (200)
     
   ```json
        {
           "success" : true,
           "message" : "사용가능한 닉네임입니다."   
        }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "ALREADY_EXIST_NICKNAME",
         "errorMessage": "이미 존재하는 닉네임입니다.",
         "details": null
      }   
   ```
  </details>
  
  <details>
  <summary>✅ 회원가입 이메일 전송</summary>
   
   ### [POST] /api/v1/user/send-mail
   
   <hr>
   
   ### 🔸 Query Parameters
   - **email**: test@test.com
     
   ### 🔸 Response (200)
     
   ```json
        {
           "success" : true,
           "message" : "이메일 인증번호가 전송되었습니다."   
        }
   ```
   ### 🔸 Response (500)
   ```json
      {
         "statusCode": 500,
         "errorCode": "INTERNAL_SERVER_ERROR",
         "errorMessage": "내부 서버 오류입니다.",
         "details": null
      }   
   ```
  </details>
  <details>
  <summary>✅ 회원가입 이메일 인증번호 확인</summary>

   
   ### [POST] /api/v1/user/verify-email

   <hr>

   ### 🔸 Request Body
   ```json
      {
         "email" : "test@test.com"
         "code" : "123456"
      }
   ```

   ### 🔸 Response (200)
   ```json
      {
         "success" : true,
         "message" : "이메일 인증에 성공하였습니다."
      }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "INVALID_CODE",
         "errorMessage": "유효하지 않은 인증번호입니다.",
         "details": null
      }   
   ```
  </details>
  <details>
  <summary>✅ 회원 정보 조회</summary>

   ### [GET] /api/v1/user/user-info

   <hr>

   **인증된 사용자만 접근 가능 (`@PreAuthorize`)**

   ### 🔸 Response (200)

   ```json
      {
        "확인 메시지": "회원정보 조회에 성공하였습니다.",
        "data": {
          "userId": 1,
          "email": "test@test.com",
          "nickname": "커리",
          "name": "서장훈",
          "birth": "2025-12-01",
          "phone": "010-1111-0000",
          "address": "서울특별시 강남구",
          "position": "NONE",
          "genderType": "MALE",
          "userType": "USER",
          "createdAt": "2025-12-01T12:08:57.090Z",
          "updatedAt": "2025-12-01T12:08:57.090Z"
         }
      }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "USER_NOT_FOUND",
         "errorMessage": "사용자를 찾을 수 없습니다.",
         "details": null
      }   
   ```  
  </details>
  <details>
  <summary>✅ 회원 정보 수정</summary>

   ### [PATCH] /api/v1/user/edit-info

   <hr>

   ### 🔸 Request Body
   ```json
      {
        "nickname": "커리",
        "phone": "010-1111-0000",
        "address": "서울특별시 강남구",
        "genderType": "MALE",
        "position": "NONE"
      }
   ```

   ### 🔸 Response (200)
   ```json
      {
         {
           "확인 메시지": 회원 정보 수정에 성공하였습니다.",
           "data": {
             "userId": 1,
             "email": "test@test.com",
             "nickname": "커리",
             "name": "서장훈",
             "birth": "2025-12-01",
             "phone": "010-1111-0000",
             "address": "서울특별시 강남구",
             "position": "NONE",
             "genderType": "MALE",
             "userType": "USER",
             "createdAt": "2025-12-01T12:11:42.649Z",
             "updatedAt": "2025-12-01T12:11:42.649Z"
           }
         }
      }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "USER_NOT_FOUND",
         "errorMessage": "사용자를 찾을 수 없습니다.",
         "details": null
      }   
   ```  
  </details>
  <details>
  <summary>✅ 비밀번호 찾기 인증번호 전송</summary>
   
   ### [POST] /api/v1/user/password/send-auth
   
   <hr>
   
   ### 🔸 Query Parameters
   - **email**: test@test.com
     
   ### 🔸 Response (200)
     
   ```json
        {
           "success" : true,
           "message" : "이메일 인증번호가 전송되었습니다."   
        }
   ```
   ### 🔸 Response (500)
   ```json
      {
         "statusCode": 500,
         "errorCode": "INTERNAL_SERVER_ERROR",
         "errorMessage": "내부 서버 오류입니다.",
         "details": null
      }   
   ```
  </details>
  <details>
  <summary>✅ 비밀번호 변경 인증번호 확인</summary>

   
   ### [POST] /api/v1/user/password/verify-code

   <hr>

   ### 🔸 Request Body
   ```json
      {
         "email" : "test@test.com"
         "code" : "123456"
      }
   ```

   ### 🔸 Response (200)
   ```json
      {
         "success" : true,
         "message" : "이메일 인증에 성공하였습니다."
      }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "INVALID_CODE",
         "errorMessage": "유효하지 않은 인증번호입니다.",
         "details": null
      }   
   ```
  </details>
  <details>
  <summary>✅ 비밀번호 찾기 비밀번호 변경</summary>

   ### [PATCH] /api/v1/user/password/reset

   <hr>

   ### 🔸 Request Body

   ```json
      {
         "email" : "test@test.com"
         "password" : "Test@12"
         "checkPassword" : "Test@12"
      }
   ```

   ### 🔸 Response (200)
   ```json
      {
         "success" : true,
         "message" : "비밀번호 변경을 완료하였습니다."
      }
   ```

   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "EMAIL_NOT_VERIFIED",
         "errorMessage": "이메일 인증을 먼저 진행해주세요.",
         "details": null
      }   
   ```
   
      
  </details>
  <details>
  <summary>✅ 비밀번호 변경</summary>

   ### [PATCH] /api/v1/user/password/change

   <hr>

   ### 🔸 Request Body

   ```json
      {
         "currentPassword" : "Test@123"
         "password" : "Test@12"
         "checkPassword" : "Test@12"
      }
   ```

   ### 🔸 Response (200)
   ```json
      {
         "success" : true,
         "message" : "비밀번호 변경을 완료하였습니다."
      }
   ```

   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "PASSWORD_NOT_MATCH",
         "errorMessage": "비밀번호가 일치하지 않습니다.",
         "details": null
      }   
   ```
   
      
  </details>
  <details>
  <summary>✅ 회원 탈퇴</summary>

   ### [PATCH] api/v1/user/delete

   **인증된 사용자만 접근 가능 (`@PreAuthorize`)**

   ### 🔸 Response (200)
   ```json
      {
         "success" : true,
         "message" : "회원탈퇴에 성공하였습니다."
      
      }
   ```
   ### 🔸 Response (401)
   ```json
      {
         "statusCode": 401,
         "errorCode": "NOT_FOUND_TOKEN",
         "errorMessage": "토큰이 존재하지 않습니다.",
         "details": null
      }   
   ```
   
  </details>



</details>

</br>

<details>
<summary>✍️ 인증 인가</summary>

   <hr>

   <details>
   <summary>✅ 유저 로그인</summary>

   ### [POST] /api/v1/user/login

   <hr>

   ### 🔸 Request Body

   ```
      {
         "email" : "test@test.com",
         "password" : "Test@12"
      }
   ```

   ### 🔸 Response (200)
   ```
      {
         {
           "확인 메시지": "로그인에 성공하였습니다.",
           "data": {
             "accessToken": "string",
             "refreshToken": "string",
             "userDto": {
               "userId": 1,
               "email": "test@test.com",
               "nickname": "커리",
               "name": "서장훈",
               "birth": "2025-12-01",
               "phone": "010-1111-0000",
               "address": "서울특별시 강남구",
               "position": "NONE",
               "genderType": "MALE",
               "userType": "USER",
               "createdAt": "2025-12-01T12:32:34.331Z",
               "updatedAt": "2025-12-01T12:32:34.331Z"
             }
           }
         }
      }
   ```
   ### 🔸 Response (400)
   ```json
      {
         "statusCode": 400,
         "errorCode": "PASSWORD_NOT_MATCH",
         "errorMessage": "비밀번호가 일치하지 않습니다.",
         "details": null
      }   
   ```
   
   
   </details>
   <details>
   <summary>✅ 유저 로그아웃</summary>

   ### [PATCH] /api/v1/user/logout

   **인증된 사용자만 접근 가능 (`@PreAuthorize`)**

   <hr>
   
   ### 🔸 Response (200)
   ```json
      {
         "success" : true,
         "message" : "로그아웃에 성공하였습니다."
      
      }
   ```

   ### 🔸 Response (401)
   ```json
      {
         "statusCode": 401,
         "errorCode": "NOT_FOUND_TOKEN",
         "errorMessage": "토큰이 존재하지 않습니다.",
         "details": null
      }   
   ```
   
   
   </details>
   <details>
   <summary>✅ 토큰 재발급</summary>

   ### [POST] /api/v1/user/reissue

   ### 🔸 Request Body
   ```
      {
         "email" : "test@test.com",
         "refreshToken" : "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9......"
      }
   ```

   ### 🔸 Response (200)
   ```
      {
         {
           "확인 메시지": "토큰 재발급에 성공하였습니다.",
           "data": {
             "accessToken": "string",
             "refreshToken": "string",
             "userDto": {
               "userId": 1,
               "email": "test@test.com",
               "nickname": "커리",
               "name": "서장훈",
               "birth": "2025-12-01",
               "phone": "010-1111-0000",
               "address": "서울특별시 강남구",
               "position": "NONE",
               "genderType": "MALE",
               "userType": "USER",
               "createdAt": "2025-12-01T12:32:34.331Z",
               "updatedAt": "2025-12-01T12:32:34.331Z"
             }
           }
         }
      }
   ```
   ### 🔸 Response (401)
   ```json
      {
         "statusCode": 401,
         "errorCode": "NOT_FOUND_TOKEN",
         "errorMessage": "토큰이 존재하지 않습니다.",
         "details": null
      }   
   ```
     
   </details>


</details>


---

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
- [x] 회원탈퇴

---

### 🏀 경기 (Match)
- [x] 경기 등록 / 수정 / 삭제 (주최자 권한)
   - THREE_ON_THREE(최소인원 : 6명, 최대인원 : 9명)
   - FIVE__ON_FIVE(최소인원 : 10명, 최대인원 : 15명)
- [x] 경기 목록 / 상세 조회
- [x] 경기 신청 / 취소
- [x] 매칭 확정 (참가자 확정 및 상태 관리)
- [x] 경기 상태 관리 (RECRUITING, CLOSED)
- [x] 현재 예정 경기 조회(QueryDSL)
- [x] 지난 경기 조회(QueryDSL)

---

### 👥 매칭 & 참가자 관리
- [x] 참가자 상태 관리(APPLY/ ACCEPT/ CANCEL 등)
- [x] 경기별 참가자 목록 조회


---

### 💬 채팅 & 알림
- [ ] 경기방 채팅 (WebSocket + STOMP)
- [x] 실시간 알림 (SSE 기반)
- [x] 알림 종류: 경기 확정/취소, 신청 결과, 신고 처리 등
- [ ] Redis Pub/Sub을 통한 멀티 인스턴스 확장

---

### ⭐ 평가 & 랭크
- [x] 경기 종료 후 참가자 평가 기능
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



