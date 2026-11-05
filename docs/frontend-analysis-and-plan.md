# React·Tailwind CSS 프론트엔드 도입 분석

작성일: 2026-11-04 · 기준: 현재 작업 디렉터리의 백엔드 소스

후속 문서: [프론트엔드 문서 모음](frontend/README.md). 사용자 지정 디자인 참고 사이트는 [플랩풋볼 구장 예약 화면](https://www.plabfootball.com/?date=2026-11-09&tab=stadium)이며 적용 기준은 [디자인 가이드](frontend/04-design-system.md)에 기록했다.

## 1. 결론과 분석 범위

현재 서버에 가장 잘 맞는 출발점은 **동일 저장소의 `frontend/`에 React + TypeScript + Vite + Tailwind CSS 기반 SPA를 추가하는 것**이다. Spring Boot는 REST API와 인증·정책 검증을 담당하고 React는 화면·입력·서버 상태를 담당한다. 검색 노출과 서버 렌더링 요구가 확정되지 않은 현재 단계에서는 별도 프론트 서버 프레임워크를 추가할 필요가 크지 않다. 추후 공개 경기 페이지의 검색 노출이 핵심 요구가 되면 렌더링 전략을 재검토한다.

기능 기획을 처음부터 새로 만드는 것보다, 구현된 서버 정책을 화면 흐름으로 옮기고 연동에 부족한 계약을 먼저 정하는 것이 효율적이다. **MVP/정책 → 화면·권한 → API 연동 → 디자인 규칙 → 구현·검증** 순서를 권장한다.

프로젝트에는 메인 Java 파일 163개, 테스트 Java 파일 34개, 컨트롤러 12개와 HTTP 엔드포인트 38개가 있다. 빌드·설정·배포 구성, 도메인별 Controller/DTO/Service/Entity/Repository, 인증 필터와 OAuth, 알림 흐름, 테스트 구조 및 기존 ADR·리팩터링·성능 문서를 대상으로 프론트엔드 관점의 정적 분석을 수행했다. 프론트 패키지는 없고 `src/main/resources/static/index.html`은 카카오 로그인 링크를 제공하는 간단한 페이지다.

이 문서는 실행 검증 보고서나 확정 PRD가 아닌 **도입 분석 및 명세 작성 제안서**다. 서버 기동, 실제 메일·카카오 인증, 테스트 실행과 라이브 OpenAPI 추출은 수행하지 않았다. 작업 중인 인증 관련 변경도 현재 파일 내용 기준으로 반영했으며 기존 코드는 수정하지 않았다.

## 2. 백엔드 기능과 화면 대응

| 영역 | 현재 구현 | 필요한 UI | 설계에서 지킬 점 |
| --- | --- | --- | --- |
| 회원 | 이메일 인증, 중복 확인, 가입, 프로필 수정, 비밀번호 변경/재설정, 탈퇴 | 단계형 가입, 계정 설정, 비밀번호 재설정 | 가입 완료 응답은 JWT가 아니므로 로그인 화면으로 이동 |
| 인증 | 이메일 로그인, JWT, Refresh 회전, 로그아웃 | 로그인, 세션 복구, 세션 종료 안내 | 응답의 새 Access/Refresh를 함께 교체 |
| OAuth | 카카오 인가, state 쿠키, 콜백, 일회용 티켓, 추가 정보 가입 | 카카오 시작·완료·추가 정보 입력 | 현재 콜백은 JSON 응답이므로 SPA로 복귀하는 연결 작업 필요 |
| 경기 검색 | 공개 목록/상세, 날짜·키워드·지역·형식·실내외·성별·모집 필터, 정렬·페이지 | 검색 목록, 상세 | 서버 필터명 그대로 매핑하고 URL에 보존 |
| 경기 운영 | 생성, 수정, 삭제, 생성자 자동 참가 | 생성 폼, 수정 폼, 운영 액션 | 장소·주소·실내외·좌표 수정 API는 없음 |
| 참가 | 선착순 확정, 취소, 강퇴, 확정 참가자 목록 | 참가/취소 버튼, 참가자 패널, 강퇴 확인 | 승인 대기나 승인/거절 화면을 만들지 않음 |
| 내 경기 | 예정·종료 경기 목록 | 내 경기 탭 | 현재 진행 중 경기 조회 공백이 있음 |
| 신고 | 종료 후 참가자 신고 | 지난 경기의 신고 모달 | 신고 대상은 다른 확정 참가자 |
| 관리자 | 신고 상태 조회·승인/거절, 승인 신고 기반 제재, 활성/만료 제재 조회 | 신고 검토, 제재 확인, 제재 목록 | 신고 승인과 7일 제재는 별도 액션 |
| 알림 | SSE, 읽지 않은 목록, 읽음 처리 | 알림 패널, 연결 복구, 실시간 안내 | 전체 알림 이력·읽은 목록·전체 읽음·총 미확인 수 API는 없음 |

채팅·친구·리뷰/평점·결제·경기장 예약·이미지 업로드·거리순 검색 API는 확인되지 않았다. 일부 알림 enum에 친구 요청/참가 승인 이름이 남아 있어도 해당 사용자 기능이 구현되었다고 해석하면 안 된다. 위도·경도 필드만으로 지도 검색이나 주소 자동 완성이 제공되는 것도 아니다.

## 3. 화면 명세에 들어가야 하는 실제 정책

| 항목 | 현재 코드의 규칙 | 화면 반영 |
| --- | --- | --- |
| 경기 생성 | 시작 시각은 현재로부터 최소 24시간 이후 | 일시 선택 제한 및 서버 오류 표시 |
| 경기 시간 | 60~240분 | 시작·종료 조합 검증 |
| 정원 | 3대3은 6~100명, 5대5는 10~100명 | 6명/10명 고정으로 만들지 않음 |
| 생성자 | 생성 시 자동으로 확정 참가, 정원에 포함 | 생성자에게 참가/참가 취소 버튼 없음 |
| 참가 | 시작 30분 전보다 이전, 모집 중, 여석·성별 조건 충족 | 요청 중 버튼 비활성화, 실패 시 인원 재조회 |
| 참가 취소 | 시작 30분 전보다 이전 | 마감 이후 취소 불가 이유 표시 |
| 수정 | 기존 시작 시각의 24시간 전보다 이전 | 수정 가능 시간 안내 |
| 일정 수정 | 시작·종료를 함께 전송, 새 시작도 최소 24시간 이후 | 두 입력을 묶어 처리 |
| 조건 수정 | 생성자 외 확정 참가자가 있으면 형식·성별 변경 불가 | 해당 필드 비활성화 |
| 정원 수정 | 현재 참가 인원보다 작아질 수 없음 | 최소값에 현재 참가 인원 반영 |
| 삭제·강퇴 | 시작 1시간 전보다 이전 | 확인 모달과 제한 설명 |
| 재참가 | 취소자는 조건 충족 시 가능, 강퇴자는 불가 | 취소와 강퇴 상태를 구분 |
| 참가자 조회 | 확정 참가자만 가능 | 비회원·미참가자에게 명단 요청/표시 제한 |
| 신고 | 종료 후 7일까지, 본인 제외, 양쪽 모두 확정 참가자, 같은 경기·신고자·대상 중복 불가 | 대상 선택·기한 안내·중복 오류 처리 |
| 제재 | 승인된 신고를 근거로 7일, 중복 활성 제재/신고 재사용 불가 | 기간 자유 입력 UI를 만들지 않음 |
| 주소 | 첫 단어가 `서울특별시`, `경기도` 등 지원 시도명과 일치해야 지역 추출 가능 | `서울 강남구` 같은 축약 주소 입력 방지 |

참가·취소·수정 등의 시간 경계는 서로 다르다. 예를 들어 정확히 시작 30분 전에는 참가와 취소가 모두 거절된다. 서버를 최종 판정 기준으로 두고 프론트 시간 제한은 사용자 안내로 사용한다.

`GameStatus`는 `RECRUITING`/`CLOSED`만 있다. 경기의 예정/진행 중/종료는 시작·종료 시각에서 파생되는 별도 표시 상태다. 모집 마감과 경기 종료를 같은 배지나 enum으로 취급하지 않는다.

## 4. 먼저 해결하거나 합의할 연동 사항

### 4.1 실행 경로·인증 계약: 인증 화면 구현 전 확정

1. **개발 프록시와 운영 경로**
   - 현재 Java 설정에서 별도 CORS 설정은 확인되지 않았다. `localhost:5173`에서 `localhost:8080`으로 직접 API를 호출하는 구성을 전제로 구현하면 교차 출처 요청 처리가 필요하다.
   - 권장안: 개발은 Vite의 `/api` 프록시, 운영은 동일 출처에서 `/api`를 Spring으로 전달한다. 운영에서 출처를 분리한다면 허용 Origin·메서드·Authorization/Last-Event-ID 헤더·쿠키 정책을 별도 명세한다.
   - SPA의 `/games/123` 새로고침은 프론트 `index.html`로 연결하고 `/api`와 실제 정적 파일 오류는 이 fallback에 포함하지 않는다. SSE 경로는 프록시 버퍼링 해제와 연결 제한 시간도 검토한다.

2. **토큰 저장·복구와 Refresh 회전**
   - 현재 로그인/재발급 응답은 `{ accessToken, refreshToken, user }`이며 토큰을 쿠키가 아닌 JSON으로 받는다. Access 만료는 설정상 1시간, Refresh는 최초 발급 기준 14일이다.
   - `AuthService.reissue`는 Refresh를 일회성으로 교체하며 기존 Refresh의 만료 시각을 유지한다. 갱신할 때마다 14일이 연장되는 계약이 아니다.
   - 동시에 여러 요청이 만료되면 하나의 갱신 요청을 공유한다. 새 토큰 쌍을 저장한 뒤 대기 요청을 최대 한 번 재시도한다. 로그인·Refresh 자체와 일반 업무 오류에는 이 로직을 적용하지 않는다.
   - Refresh 저장 키가 이메일 단위이므로 다른 로그인으로 이전 Refresh가 대체될 수 있다. 탭/기기별 독립 세션을 전제하지 않는다.
   - 초기 검증판은 메모리 저장과 새로고침 시 재로그인으로 단순화할 수 있다. 지속 로그인이 필요하면 구현 전에 계약을 확정한다. 운영 권장안은 HttpOnly Refresh 쿠키 또는 BFF이며 이는 서버 변경, 쿠키·CSRF 설계를 수반한다. `sessionStorage`도 JS 접근이 가능하므로 HttpOnly 쿠키와 동일한 보호를 제공하지 않는다.
   - 여러 탭에서 Refresh를 공유하는 방식을 선택하면 갱신 조정/토큰 교체 알림도 설계한다. 한 탭 내 요청 공유만으로는 해결되지 않는다.

3. **카카오 인증 이후 SPA 복귀**
   - 현재 `/api/v1/auth/oauth2/kakao/callback`은 `flowType`, `ticket`, `email`, `nickname`을 JSON으로 반환한다. 기존 링크를 눌러 인증하면 브라우저가 이 JSON에 도착하며 React가 자동으로 결과를 받지 못한다.
   - 권장안: 서버 콜백에서 state 검증을 유지하고 허용된 고정 프론트 완료 경로로 일회용 티켓을 전달하는 복귀 계약을 추가한다. 장기 JWT는 URL로 전달하지 않는다.
   - 티켓 전달 위치, 즉시 URL 정리, referrer/로그 노출 제한, `LOGIN`/`SIGNUP` 구분 방법, 신규 사용자 기본 정보 조회 방식을 함께 정한다. 다른 안으로는 state 쿠키 전송 조건을 충족하는 프론트 콜백 중계가 있으나 현재 쿠키 Path와 카카오 등록 redirect URI를 포함한 검증이 필요하다.
   - OAuth 티켓은 10분 수명이며 서버 비즈니스 검증 전에 소비된다. 신규 가입의 닉네임 중복처럼 검증 실패 시 같은 티켓으로 재시도할 수 없는 흐름도 명세해야 한다. 사용자 입력 수정 후 재시도가 필요하면 소비 시점을 서버에서 재설계한다.
   - `OAUTH_ACCOUNT_LINK_REQUIRED` 오류는 있지만 계정 연결 API는 없다. “기존 계정으로 로그인” 안내까지만 제공하고 동작하지 않는 연결 버튼을 만들지 않는다.

4. **인증이 필요한 SSE**
   - 알림 구독은 USER 권한과 `Authorization: Bearer ...`가 필요하다. 브라우저 기본 `EventSource`만으로 임의의 Authorization 헤더를 지정할 수 없다.
   - Fetch 기반 SSE 클라이언트를 사용해 인증 헤더, `Last-Event-ID`, 중단·재연결을 처리한다. 직접 스트림을 읽는다면 청크 경계·여러 줄 data까지 처리하는 파서가 필요하다.
   - 연결 timeout은 30분, 재전송 캐시는 인스턴스 메모리의 5분이다. 재접속 시 읽지 않은 DB 알림도 다시 조회해야 한다. 인스턴스가 바뀌거나 시간이 지난 경우 캐시만으로 누락 복구를 보장할 수 없다.
   - 같은 `notification` 이벤트에 영속 `NotificationResponse`와 경기 생성용 `GameCreatedNotificationMessage`가 들어온다. 필드 기준 분기와 중복 제거를 설계하거나 서버에 discriminator를 추가한다. 경기 생성 메시지는 전체 사용자 방송이 아니라 생성자 본인 대상이다.
   - 로그인된 USER 레이아웃에서 연결을 하나 관리하고 로그아웃/사용자 변경 시 종료한다. 연결 오류만으로 로그아웃시키지 않는다. 제재 응답은 갱신 반복 대신 접근 제한 안내로 전환한다.

### 4.2 데이터 보완: 해당 화면의 상세 설계 전 확정

| 발견한 계약 공백 | 영향 | 제안 |
| --- | --- | --- |
| 공개 상세에 `creatorId`는 있지만 내 참가 상태 없음 | 참가/취소/강퇴 제한 버튼을 안정적으로 결정하기 어려움 | 인증 전용 `GET /games/{gameId}/participations/me` 추가 제안. 미참가 상태·참가 관계 ID·가능 액션 계약 정의 |
| 내 예정 경기는 `start > now`, 종료 경기는 `end <= now` | 시작~종료 사이의 경기가 내 경기 두 목록에서 모두 사라짐 | 예정 목록에 진행 중을 포함하거나 진행 중 조회 추가 |
| 영속 알림 응답에 `gameId`/이동 대상 없음 | 알림에서 경기 상세 이동 불가 | `targetType`, `targetId` 또는 안전한 이동 경로 추가. 삭제된 경기는 별도 안내 |
| 알림 목록은 `Page`가 아닌 `List` | 전체 미확인 수·다음 페이지 여부를 정확히 알 수 없음 | 초기는 목록만 표시. 정확한 배지가 필요하면 count/hasNext 계약 추가 |
| 프로필 응답에 `loginProvider` 없음 | 새로고침 후 카카오 사용자의 비밀번호 설정 화면 분기 어려움 | `loginProvider` 또는 `canChangePassword` 추가 |
| ADMIN은 USER 권한을 자동 상속하지 않음 | 사용자용 API·알림·로그아웃도 ADMIN에는 거부될 수 있음 | 관리자도 공통 로그아웃을 허용할지 정책과 메서드 권한 정리 |

내 참가 상태를 확인하려고 모든 내 경기 페이지를 순회하거나 참가자 목록의 403을 일반적인 상태 조회 수단으로 사용하는 방식은 피한다. 위 신규 API/필드는 제안이며 현재 구현되어 있지 않다.

### 4.3 정책·오류 응답의 일치 확인

- **성별 `NONE` 처리:** 가입 enum에 `NONE`이 있지만 참가 검증은 MALE_ONLY에서 FEMALE만, FEMALE_ONLY에서 MALE만 차단한다. 현재는 `NONE`이 두 제한을 통과할 수 있다. 프론트에서만 막지 말고 서버 정책을 확정한다.
- **입력 제약:** 생성 DTO는 제목/내용 최대 길이가 없으나 수정 DTO는 제목 100자·내용 255자다. 폼 명세에서 생성/수정 제약을 맞추고 서버도 일치시킨다. 생년월일은 현재 DTO상 NotNull만 있으므로 화면에서 과거 날짜를 강제하려면 정책 변경임을 기록한다.
- **오류 포맷:** CustomException·본문 Bean Validation·인증 필터는 공통 오류 모델을 사용한다. 다만 잘못된 enum/JSON, 쿼리 바인딩/메서드 검증, Security의 인증 진입·권한 부족에 대한 개별 처리까지 일관된지는 실행 확인이 필요하다. 광범위한 Exception handler만으로 일부 입력 오류가 500이 되지 않도록 점검한다.
- **메일 발송:** 발송은 `@Async void`라 요청 성공과 실제 메일 전달 성공이 같지 않다. UI는 “요청 접수”와 수신 확인을 구분하고 재전송/만료 동작을 정한다. 인증번호는 6자리·3분, 회원가입 인증 완료 상태는 10분이다.
- **신고 중복·제재 적용 여부:** 사용자 본인의 신고 목록이나 개별 신고의 제재 완료 표시 API가 없다. 초기는 서버 중복 오류를 처리하고, 이력/정확한 버튼 상태가 필요할 때만 API를 확장한다.

## 5. 추천 npm 패키지 구성

프론트는 npm과 하나의 `package-lock.json`으로 시작한다. 정확한 버전은 실제 초기화 시 호환되는 안정 버전을 선택하고 lockfile로 고정한다. 아래는 설치를 수행한 결과가 아닌 권장 목록이다.

| 구분 | 패키지 | 이 프로젝트에서의 역할 |
| --- | --- | --- |
| 핵심 | `react`, `react-dom` | UI |
| 개발 | `typescript`, `@types/react`, `@types/react-dom`, `@types/node`, `vite`, `@vitejs/plugin-react` | 타입 검사·개발 서버·빌드 |
| 스타일 | `tailwindcss`, `@tailwindcss/vite` | Tailwind CSS와 Vite 통합 |
| 라우팅 | `react-router` | 공개/USER/ADMIN 경로, 쿼리 문자열 |
| 서버 상태 | `@tanstack/react-query` | 검색·페이지 캐시, 변경 후 무효화, 요청 상태 |
| 폼 | `react-hook-form`, `zod`, `@hookform/resolvers` | 가입·경기·신고 폼과 필드 검증 |
| 품질 | `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `prettier` | Vite 템플릿 기반 검사와 포맷 |
| 테스트 | `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `msw`, `@playwright/test` | 주요 상호작용·API 모킹·핵심 E2E |
| 필요 시 | `@tanstack/react-query-devtools` | 개발 중 캐시 확인 |
| UI 확장 시 | `clsx`, `tailwind-merge`, `lucide-react` | 조건부 스타일, 클래스 충돌 정리, 아이콘 |
| SSE 구현 시 | Fetch 기반 SSE 라이브러리 검토 | 임의 헤더·파싱·재연결 지원 여부 확인 후 선정 |

HTTP는 우선 네이티브 `fetch`와 공통 래퍼로 충분하다. Axios를 선호하면 그 자리에 교체할 수 있지만 두 개를 병행할 필요는 없다. SSE는 일반 JSON 래퍼와 별도 전송 계층이 필요하다. `@microsoft/fetch-event-source`는 헤더를 지원하는 구현 참고/후보이며 도입 시 유지보수·브라우저 호환성을 확인한다.

처음에는 React Context/작은 메모리 저장소로 인증 상태를 관리할 수 있다. Zustand/Redux는 실제 클라이언트 상태 복잡성이 생길 때 검토한다. 지도 SDK, 날짜 UI, 테이블 전용 라이브러리, 대규모 UI 키트는 화면 요구가 정해진 뒤 추가한다.

Tailwind CSS의 현재 Vite 통합 방식은 `@tailwindcss/vite` 플러그인과 CSS의 `@import "tailwindcss";`다. 예전 `tailwindcss init -p` 중심 설정을 그대로 섞지 않는다. React 공식 문서도 직접 구성하는 앱에 Vite의 `react-ts` 템플릿을 안내한다. 실행 환경의 Node 버전은 실제 선택한 Vite의 요구 범위를 확인해 고정한다.

공식 참고: [React 앱 구성](https://react.dev/learn/build-a-react-app-from-scratch), [Vite 시작](https://vite.dev/guide/), [Tailwind Vite 통합](https://tailwindcss.com/docs/installation/using-vite), [React Router 설치](https://reactrouter.com/start/declarative/installation), [TanStack Query](https://tanstack.com/query/latest/docs/framework/react/overview), [React Hook Form](https://react-hook-form.com/get-started), [Fetch SSE 구현 참고](https://github.com/Azure/fetch-event-source).

## 6. 디렉터리와 상태 관리

```text
frontend/
  src/
    app/
      router.tsx
      providers.tsx
      layouts/
    pages/
      games/
      auth/
      mypage/
      admin/
    features/
      auth/              # 로그인·갱신·OAuth·인증 상태
      users/             # 가입·프로필·비밀번호
      games/             # 검색·상세·생성/수정
      participations/    # 참가·취소·강퇴·참가자
      notifications/     # 목록·읽음·SSE
      reports/
      moderation/        # 관리자 검토·제재
    shared/
      api/               # HTTP 래퍼·공통 오류·계약 타입
      ui/                # Button/Input/Select/Dialog/Badge 등
      lib/               # 날짜 직렬화·주소/enum 표시
      styles/
    test/
  e2e/
  .env.example
  package.json
  package-lock.json
  vite.config.ts
```

각 feature에는 필요한 범위에서 `api.ts`, `queries.ts`, `types.ts`, `schemas.ts`, `components/`를 둔다. 페이지는 기능을 조합하고 shared는 feature를 참조하지 않는다. 생성 타입과 수기 타입을 이중 관리하지 않도록 API 타입 소유 위치를 하나로 정한다. 프론트 규모에 비해 과도한 계층은 만들지 않는다.

| 상태 | 위치 |
| --- | --- |
| 경기/프로필/참가자/신고/알림 응답 | TanStack Query |
| 검색 필터·페이지·정렬 | URL search params |
| 폼 값·입력 오류 | React Hook Form |
| 로그인 사용자·토큰·인증 복구 상태 | 인증 모듈 |
| 모달·선택 탭 등 일시적 UI | 컴포넌트 state |

참가 성공/실패, 취소·강퇴·수정·삭제 후에는 관련 상세·목록·참가자·내 경기 캐시를 선택적으로 무효화한다. 정원 경쟁은 서버가 판정하므로 참가 성공 인원이나 자리를 먼저 확정 표시하는 낙관적 처리는 초기에는 사용하지 않는다. 계정 전환 시 사용자별 캐시·SSE·진행 중 인증 요청을 정리한다.

## 7. 먼저 작성할 문서 5개

별도 대형 문서를 많이 만들기보다 다음 5개로 시작한다. 아래 파일은 후속 요청에 따라 구현 기준 초안 v1으로 작성했다. 미구현 API와 실제 구현은 각 문서에서 구분한다.

| 작성 순서 | 문서 | 필수 내용 | 완료 기준 |
| --- | --- | --- | --- |
| 1 | `docs/frontend/01-product-scope.md` | 사용자 문제, 방문자/회원/관리자, 핵심 여정, MVP·후속·제외 범위, 성공 조건 | “검색→가입/로그인→참가→내 경기”가 1차 범위로 명확함 |
| 2 | `docs/frontend/02-screens-and-policies.md` | 사이트맵, 경로, 화면 항목, 권한/상태별 버튼, 시간 제한, 폼 검증, 로딩/빈값/오류 | 모든 화면이 실제 API·정책과 연결됨 |
| 3 | `docs/frontend/03-api-integration.md` | 메서드·URL·파라미터·DTO·enum·null·응답 예시·오류, 토큰 갱신, OAuth 복귀, SSE, 필요한 서버 변경 | 정상·실패·재시도 계약과 미구현 항목이 구분됨 |
| 4 | `docs/frontend/04-design-system.md` | 색상/타이포/간격, 반응형, 공통 컴포넌트와 상태, 키보드/포커스/오류 안내 | 검색·상세·폼 3개 화면의 와이어프레임과 공통 UI 규칙 있음 |
| 5 | `docs/frontend/05-implementation-and-acceptance.md` | 폴더·패키지·환경·배포, 단계별 작업, 인수 조건, 핵심 테스트 | 각 작업의 완료 여부를 시나리오로 판단할 수 있음 |

기존 README의 API 목록과 Swagger를 다시 수기로 복제하지 않는다. 실행 가능한 환경에서 `/v3/api-docs`를 추출해 API 계약의 기준으로 삼고, 문서 3에는 화면 매핑·상태 처리·인증·SSE처럼 OpenAPI만으로 설명되지 않는 내용을 보완한다. OpenAPI 타입 생성은 응답/페이지 스키마를 확인한 뒤 도입하며 처음부터 필수 패키지로 추가할 필요는 없다.

### 화면·라우팅 초안

| 권한 | 경로 제안 | 역할 |
| --- | --- | --- |
| 공개 | `/`, `/games` | 경기 탐색, 검색·필터·페이지 |
| 공개 | `/games/:gameId` | 경기 상세, 권한별 액션 |
| 공개 | `/login`, `/signup`, `/password-reset` | 이메일 인증·가입·로그인·계정 복구 |
| 인증 진행 | `/auth/kakao/complete`, `/auth/kakao/signup` | 서버 복귀 계약 추가 후 티켓 교환/추가 정보 |
| USER | `/games/new`, `/games/:gameId/edit` | 경기 생성/수정, edit는 생성자 확인 |
| USER | `/mypage/games`, `/mypage/profile` | 내 경기·계정 설정 |
| USER | 전역 알림 패널 또는 `/notifications` | 읽지 않은 알림·읽음 처리 |
| ADMIN | `/admin/reports`, `/admin/blacklists` | 검토·제재·이력 |

참가자 조회·강퇴·신고는 상세/내 경기의 패널·모달에서 처리할 수 있다. 로그인 후 원래 경로로 돌아가되 돌아갈 경로는 내부 URL만 허용한다. 프론트 라우트 가드는 화면 진입을 돕고 실제 인가는 서버에서 수행한다.

### API 연동 명세에 사용할 최소 예시

```ts
type CommonResponse<T> = { message: string; data: T };
type CheckResponse = { success: boolean; message: string };
type ApiError = {
  statusCode: number;
  errorCode: string;
  errorMessage: string;
  details: { field: string; code: string; message: string }[] | null;
};
// Page 응답에서는 content, number, size, totalElements, totalPages 등
// 실제 사용하는 필드를 실행 중인 서버의 OpenAPI/응답으로 확인한다.
```

모든 성공 응답이 `data`를 갖는 것은 아니다. 취소/삭제 등은 `CheckResponse`, SSE는 스트림이며 오류/비 JSON 응답도 별도 처리한다. 목록의 `page`는 0부터 시작한다. 화면의 1페이지와 API의 `page=0`을 매핑한다. 알림 목록은 `CommonResponse<NotificationResponse[]>`다.

날짜는 서버의 `LocalDateTime`과 서비스 시간대 Asia/Seoul 기준으로 직렬화한다. 사용자의 기기 시간대에 의존해 `toISOString()`으로 변환하면 시각이 밀릴 수 있다. 예시는 `2026-11-10T19:00:00`, 날짜 필터는 `2026-11-10`이다. 일부 Swagger 예시의 `15:00:00:00` 문자열은 실제 전송 예시로 사용하지 않는다.

## 8. 단계별 구현 순서와 완료 조건

| 단계 | 작업 | 완료 조건 |
| --- | --- | --- |
| 0 | 문서 1~3 초안, 인증/콜백/프록시/참가 상태 계약 결정 | 미결정 사항에 담당 작업·선행 화면이 연결됨 |
| 1 | `frontend/`, Vite·Tailwind·라우터·Query·HTTP 오류 처리·공통 UI | dev/build/typecheck/lint, 공개 목록 API 연결 |
| 2 | 경기 검색·필터·상세 | 검색 URL 복원, 페이지 이동, 빈 결과·404·서버 오류 확인 |
| 3 | 이메일 가입·로그인·비밀번호 재설정·Refresh | 인증 만료 동시 요청에서 갱신 1회, 실패 시 세션 종료 안내 |
| 4 | 생성·수정·참가·취소·강퇴·내 경기·프로필 | 역할별 버튼, 마감 경계, 정원 경쟁 오류, 캐시 갱신 확인 |
| 5 | 알림 목록·읽음·SSE, 신고, 카카오 인증 | 재접속·중복 수신·티켓 만료/소비 실패·신고 기한 확인 |
| 6 | 관리자 신고 검토·별도 제재, 운영 배포 | 권한 분리, 승인≠제재 확인, SPA 새로고침·SSE 운영 경로 확인 |

카카오와 SSE의 계약 설계는 단계 0에서 진행하되 UI 구현은 기본 사용자 흐름이 안정된 뒤 연결한다. 신고를 사용자에게 공개하는 시점에는 실제 신고를 처리할 관리자 수단도 준비되어 있어야 한다.

핵심 인수 시나리오는 비회원 탐색→이메일 인증→가입→로그인→경기 참가→내 경기 확인, 생성자 생성→참가자 유입→수정 제한/강퇴, 동시 참가로 정원 초과 시 정상 안내, 만료 요청의 단일 갱신, 다른 사용자 로그인 후 이전 데이터 제거, 종료 경기 신고→관리자 승인→별도 제재, SSE 재연결 후 영속 알림 복구다. 모든 컴포넌트를 기계적으로 테스트하기보다 이 사용자 흐름과 시간·권한 경계를 우선 검증한다.

## 9. 주요 코드 근거

- `build.gradle`: Java 17·Spring Boot 3.2.5, JPA/Redis/Security/QueryDSL/OpenAPI, 단위/통합 테스트 태스크.
- `src/main/java/com/example/basketballmatching/global/config/SecurityConfig.java`: 공개 API, 인증 경로와 필터.
- `src/main/java/com/example/basketballmatching/auth/service/AuthService.java`: 로그인, 토큰 회전·기존 Refresh 만료 유지.
- `src/main/java/com/example/basketballmatching/auth/service/AuthTokenStore.java`: 이메일 기준 Refresh 저장·회전.
- `src/main/java/com/example/basketballmatching/auth/oauth2/controller/OAuth2Controller.java`: 현재 JSON 콜백 응답.
- `src/main/java/com/example/basketballmatching/auth/oauth2/service/OAuthTicketStore.java`: 10분 티켓과 소비 동작.
- `src/main/java/com/example/basketballmatching/auth/oauth2/support/OAuthStateCookieManager.java`: 콜백 Path·HttpOnly·SameSite 설정.
- `src/main/java/com/example/basketballmatching/game/domain/GameEntity.java`: 경기 일정·정원·시간 제한·성별 정책.
- `src/main/java/com/example/basketballmatching/game/service/GameParticipantService.java`: 선착순 참가·권한·참가 관계 ID를 이용한 강퇴.
- `src/main/java/com/example/basketballmatching/game/repository/query/GameQueryRepository.java`: 검색 조건·정렬·내 경기 시간 범위.
- `src/main/java/com/example/basketballmatching/game/dto/request/UpdateGameRequest.java`: 수정 가능 필드와 제약.
- `src/main/java/com/example/basketballmatching/game/dto/response/GameDetailResponse.java`: 현재 상세 응답의 공개 필드.
- `src/main/java/com/example/basketballmatching/user/dto/UserProfileResponse.java`: 프로필 필드와 로그인 방식 정보 부재.
- `src/main/java/com/example/basketballmatching/global/service/MailService.java`: 비동기 전송·인증 유효기간.
- `src/main/java/com/example/basketballmatching/report/service/ReportService.java`: 신고 대상·기한·검토 규칙.
- `src/main/java/com/example/basketballmatching/blacklist/service/BlackListService.java`: 승인 신고 기반 별도 7일 제재.
- `src/main/java/com/example/basketballmatching/notifications/service/NotificationService.java`: 스트림 이벤트·알림·30분 timeout.
- `src/main/java/com/example/basketballmatching/notifications/repository/EmitterRepository.java`: 인스턴스 로컬 5분 캐시.
- `src/main/java/com/example/basketballmatching/notifications/redis/listener/GameCreatedAfterCommitListener.java`: 생성자 대상 경기 생성 알림.
- `src/main/java/com/example/basketballmatching/global/exception/GlobalExceptionHandler.java`: 공통 예외·검증 오류 처리.
- `src/main/resources/application-local.yml`, `compose.local.yml`, `.github/workflows/ci.yml`: 로컬 의존 서비스와 현재 백엔드 CI.
