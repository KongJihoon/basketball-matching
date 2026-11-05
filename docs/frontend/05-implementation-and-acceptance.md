# 구현·검증·인수 계획서

작성일: 2026-11-04 · 상태: 구현 기준 초안 v1

## 1. 개발 기본 결정

| 항목 | 기본안 |
| --- | --- |
| 저장소 | 기존 루트의 frontend/에 독립 npm 프로젝트 |
| 화면 | React·TypeScript·Vite SPA |
| 스타일 | Tailwind CSS Vite 플러그인, 화이트 계열 라이트 테마·그린(#00c471) 강조 |
| 탐색 | React Router, 필터·탭·페이지는 URL |
| 서버 상태 | TanStack Query |
| 폼 | React Hook Form·Zod |
| HTTP | fetch 공통 래퍼, SSE는 별도 모듈 |
| 인증 | 검증판은 메모리 토큰, 새로고침 시 재로그인 |
| 개발 연동 | localhost:5173의 /api를 localhost:8080에 프록시 |
| 운영 연동 | 동일 출처, /api는 Spring, 나머지 앱 경로는 SPA |
| 버전 | 초기화 시 호환 안정 버전 확인·lockfile 고정 |

현재 설치·설정 완료 상태를 뜻하지 않는다. 자동 로그인/운영 토큰 저장 방식 변경이 필요하면 구현 전에 API 명세의 인증 계약을 갱신한다. 문서에 없는 정책을 화면 구현 중 암묵적으로 추가하지 않는다.

## 2. 패키지·스크립트 계획

| 구분 | 패키지 |
| --- | --- |
| 실행 | react, react-dom, react-router, @tanstack/react-query, react-hook-form, zod, @hookform/resolvers |
| 빌드·타입 | vite, @vitejs/plugin-react, typescript, @types/react, @types/react-dom, @types/node |
| 스타일 | tailwindcss, @tailwindcss/vite |
| 코드 검사 | eslint, @eslint/js, typescript-eslint, eslint-plugin-react-hooks, eslint-plugin-react-refresh, prettier |
| 의미 있는 테스트 | vitest, jsdom, @testing-library/react, @testing-library/jest-dom, @testing-library/user-event, msw, @playwright/test |
| 선택 | clsx, tailwind-merge, lucide-react, @tanstack/react-query-devtools |

SSE 라이브러리와 Dialog/Sheet primitive는 실제 도입 전에 헤더·중단·접근성·현재 유지보수 상태를 확인해 선정한다. 채택 시 이유와 버전을 이 문서에 추가한다. Redux/Zustand·지도 SDK·테이블 라이브러리를 먼저 설치하지 않는다.

| npm script | 역할 |
| --- | --- |
| dev | Vite 개발 서버 |
| build | TypeScript 검사 후 Vite production build |
| preview | 빌드 미리보기 |
| typecheck | 타입 검사 단독 실행 |
| lint | ESLint |
| format:check | 포맷 확인 |
| test / test:run | Vitest watch / CI 실행 |
| test:e2e | Playwright 핵심 사용자 흐름 |

프론트 CI는 frontend/ 작업 디렉터리에서 npm ci→검사→단위/상호작용 테스트→build를 실행한다. MSW 기반 브라우저 검증과 실제 API 연동 테스트를 구분한다. 기존 Java CI에 프론트 실패를 숨기거나 테스트 전체를 생략하는 변경을 하지 않는다.

## 3. 코드 경계

```text
frontend/src/
  app/          router, providers, layouts, error boundary
  pages/        games, auth, mypage, admin
  features/
    auth/       세션·갱신·OAuth
    users/      가입·프로필·비밀번호
    games/      검색·상세·생성·수정
    participations/
    notifications/
    reports/
    moderation/
  shared/
    api/        fetch·오류 정규화·공통 계약
    ui/         Button, Input, Dialog, DateStrip 등
    lib/        KST 날짜·주소·enum 표시
    styles/     디자인 토큰
  test/         MSW·공통 테스트 설정
frontend/e2e/
```

feature 내부의 api/queries/schema와 페이지 조합을 분리한다. shared가 feature를 import하지 않는다. 토큰과 개인 데이터를 Query devtools 외 로그·에러 수집에 무분별하게 출력하지 않는다. API 타입은 OpenAPI 또는 shared/feature 타입 중 한 곳에서 관리하고 중복 선언하지 않는다.

## 4. 작업 순서와 의존성

| 작업 ID | 작업 | 선행 | 완료 기준 |
| --- | --- | --- | --- |
| F-01 | Vite/TS/Tailwind/Router/Query·스크립트 | 문서 기본안 | dev·build·typecheck·lint 성공 |
| F-02 | 토큰 없는 fetch/응답 모델·프록시 | F-01 | 공개 목록 API 연동, K/C/Page 오류 처리 |
| F-03 | 공통 UI·레이아웃·날짜·필터·목록 | F-02 | 플랩 참고 구조·모바일 대응·URL 복원 |
| F-04 | 상세 읽기 | F-03 | 404·모집/진행 상태·참가자 비공개 처리 |
| F-05 | 이메일 가입/로그인/계정 복구·Refresh | F-02, B-07 | 단일 갱신·에러·메모리 세션 종료 |
| F-06 | 생성/수정/삭제 | F-04/05, B-09 | 생성자 자동 참가·제약·시간 경계 |
| F-07 | 참가·취소·명단·강퇴 | F-04/05, B-02/08 | 권한·재참가·정원 경쟁·캐시 갱신 |
| F-08 | 내 경기·프로필·비밀번호·탈퇴 | F-05/07, B-03/05 | 진행 중 누락 해소·계정 전환 정리 |
| F-09 | unread·읽음·SSE | F-05 | 재연결·payload 분기·중복·클린업 |
| F-10 | 카카오 로그인/추가 정보 | F-05, B-01 | state·복귀·티켓 만료/실패·중복 호출 |
| F-11 | 신고·관리자 검토·별도 제재 | F-07/08, B-06 | 접수→검토→별도 제재 전체 흐름 |
| F-12 | 운영 배포·접근성·최종 인수 | 앞 단계 | 아래 인수 기준·실API·배포 검증 통과 |

B-01~B-09는 [API 보완 작업](03-api-integration.md#8-백엔드-보완-작업)이다. B-04는 알림 상세 이동을 제공할 때만 선행하며 메시지 읽기·SSE 연결의 필수 의존성은 아니다. B-08/09 정책이 바뀌면 폼·오류·테스트 문서를 함께 갱신한다.

프론트는 B 보완 완료 전에도 공개 탐색·공통 UI·API 모킹 개발을 진행할 수 있다. 미구현 B API를 MSW로만 통과시킨 상태를 실제 연동 완료라고 표시하지 않는다.

## 5. 인수 시나리오

모든 항목의 초기 상태는 **미실행**이다. 아래 조건은 통과 결과가 아니라 구현 후 확인할 기준이다.

| ID | 시나리오 | 기대 결과 | 검증 방식 |
| --- | --- | --- | --- |
| A-01 | 비회원이 날짜·지역·형식 변경 후 새로고침/뒤로 가기 | 같은 검색 조건·페이지 복원 | Playwright·실API |
| A-02 | 날짜/enum/페이지가 잘못된 URL 진입 | 정규화된 URL과 안전한 요청, 흰 화면 없음 | 상호작용 테스트 |
| A-03 | 목록 0건·오류·느린 재조회 | 초기화/재시도 제공, 재조회 중 기존 목록 유지 | MSW |
| A-04 | 삭제된 경기 상세 접속 | 404 안내·목록 복귀 | 실API |
| A-05 | 이메일 요청→코드 확인→가입 | 202를 접수로 표시, 가입 후 로그인 이동 | 테스트 메일·실API |
| A-06 | 이메일 변경·만료 코드·중복 닉네임 | 인증 UI 갱신·필드 오류·입력 보존 | MSW·실API |
| A-07 | 여러 보호 API가 동시에 만료 | 한 번만 갱신, 새 토큰 쌍 적용, 각 요청 1회 재시도 | 제어된 인증 테스트 |
| A-08 | 갱신 중 로그아웃/다른 사용자 로그인 | 늦은 결과가 이전 세션을 복원하지 않음 | 모킹 경합 테스트 |
| A-09 | 갱신 실패/로그아웃 후 재진입 | 사용자 캐시·토큰·SSE 정리, 로그인 안내 | 브라우저 |
| A-10 | 새로고침 | 메모리 세션 정책에 따라 재로그인, 공개 탐색 정상 | 브라우저 |
| A-11 | 경기 생성 | 정원에 생성자 포함·상세 이동·내 경기 반영 | 실API |
| A-12 | 생성자 외 참가자가 생긴 후 형식/성별 수정 | 화면 제한, 강제 호출도 서버 거절 | 서버+UI |
| A-13 | 마지막 자리에 두 사용자가 참가 | 서버가 허용한 사용자만 확정, 다른 사용자는 최신 상태 안내 | 실API 다중 세션 |
| A-14 | 취소→재참가 / 강퇴→재참가 | 취소자는 조건 충족 시 성공, 강퇴자는 거절 | 실API |
| A-15 | 미참가자·비회원의 명단 요청 | 명단 노출 없음·권한 안내 | 서버+UI |
| A-16 | 강퇴 버튼 사용 | userId와 다른 participationId를 정확히 전송 | 통합 테스트 |
| A-17 | 경기 진행 중 | 내 경기에서 사라지지 않고 진행 중 표시 | Clock 고정 서버 테스트 |
| A-18 | 프로필 변경·탈퇴·계정 전환 | 올바른 사용자 표시, 이전 데이터/연결 잔존 없음 | 실API |
| A-19 | SSE connect·두 notification 모델·동일 이벤트 재전송 | 파싱 성공·중복 없음·DB/실시간 안내 분리 | 스트림 모킹·실API |
| A-20 | SSE 단절·캐시 기간 초과·다른 인스턴스 | 재연결·unread 재조회, 완전 복구를 과장하지 않음 | 실API/통합 환경 |
| A-21 | 카카오 기존/신규·만료/소비 티켓·가입 실패 | 정확한 분기·재시도/재시작·중복 교환 방지 | 카카오 테스트 계정 |
| A-22 | 종료 경기 신고·본인/미참가/중복/기한 초과 | 허용 요청만 접수, 불가 이유 안내 | 서버+UI |
| A-23 | 승인 후 아직 제재하지 않은 신고 | 승인 상태만 변경, 별도 제재 액션 필요 | 실API |
| A-24 | 제재 생성·중복·이미 처리된 신고 | 만료 시각 확인·중복 안내·목록 갱신 | 실API |
| A-25 | USER의 관리자 접근·ADMIN 로그아웃 | 서버 인가·접근 불가 안내·B-06 반영 | 서버+UI |
| A-26 | 잘못된 JSON·enum·누락 인증·타인 알림 읽음 | 올바른 4xx·오류 fallback·정보 노출 없음 | 계약 테스트 |
| A-27 | 390/768/1440px·200% 확대·키보드 | 주요 기능 사용 가능·초점 가림/가로 넘침 없음 | 시각·수동 검수 |
| A-28 | 운영의 상세 URL 새로고침·SSE 장시간 연결 | SPA fallback·API 오류 분리·스트림 정상 | 배포 환경 |

### 시간 경계 검증

시스템 시계를 바꾸지 말고 서버의 주입된 Clock 또는 격리된 테스트 fixture를 사용한다.

| 행위 | 검증 지점 |
| --- | --- |
| 생성 | now+24시간 직전 / 정확히 / 직후 |
| 수정 | start-24시간 직전 / 정확히 / 직후 |
| 참가·취소 | start-30분 직전 / 정확히 / 직후 |
| 삭제·강퇴 | start-1시간 직전 / 정확히 / 직후 |
| 경기 조회 | start 직전/정확히/이후, end 직전/정확히/이후 |
| 신고 | end 직전/정확히, end+7일 정확히/직후 |

## 6. 환경·배포 준비

로컬 MySQL/Redis는 기존 compose.local.yml과 application-local.yml을 사용한다. 호스트 포트는 MySQL 3307, Redis 6380이며 Spring은 기본8080을 전제로 한다. 실제 시작 전 환경 변수와 포트 점유를 확인한다. SMTP와 카카오 설정은 실제 연동 검증 시 필요하며 모킹 통과와 구분한다.

프론트의 예시 환경 변수는 공개 가능한 API 경로만 두고, VITE 접두 변수는 브라우저 번들에 노출된다는 전제로 관리한다. localhost:8080 같은 백엔드 개발 target은 Vite의 개발 프록시 설정에 둔다. 프록시는 /api 경로를 보존한다.

운영은 정적 build 산출물을 배포하고 /api 요청을 Spring으로 전달한다. 존재하지 않는 JS/CSS 경로와 /api 오류를 SPA HTML로 바꾸지 않는다. 현재 Spring의 static/index.html은 프론트 빌드를 제공하는 완성 배포 구성이 아니며 기존 Dockerfile/CI도 프론트 빌드를 자동 포함하지 않는다. 운영 도메인·호스팅 서비스 선택과 실제 배포는 별도 구현 작업이다.

SSE는 프록시 버퍼링·read timeout·연결 수·인스턴스 분산을 확인한다. 현재 영속 알림의 실시간 전송·재전송 캐시는 인스턴스 로컬이므로 다중 인스턴스 전체 전달을 이미 보장한다고 기록하지 않는다. 재연결·창 focus 시 DB 알림 재조회로 보완하고, 더 강한 실시간 보장이 필요하면 서버 라우팅/메시징을 확장한다.

## 7. 완료 판정과 변경 관리

- F 작업별 코드·검증 결과·남은 제약을 기록한다. 테스트 결과는 실행 날짜·환경·명령·결과로 남긴다.
- mock UI만 완성, build만 성공, 서버 API만 성공인 상태는 전체 기능 완료로 표시하지 않는다.
- 실제 API 연결·정책 경계·오류 복구·반응형 확인을 마쳐야 해당 화면을 완료로 표시한다.
- API/정책 변경 시 관련 B 작업, 화면 명세, 오류 매핑, 테스트를 같은 변경에서 수정한다.
- 기존 작업 중인 인증 소스·테스트는 별도 변경으로 존중하고 덮어쓰거나 되돌리지 않는다.

본 문서 작성 단계에서는 패키지 설치·앱 구현·백엔드 변경·테스트·배포를 실행하지 않았다.
