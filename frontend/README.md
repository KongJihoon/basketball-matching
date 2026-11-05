# Basketball Matching Frontend

Spring Boot 농구 매칭 API와 연결하는 React 프론트엔드입니다.

현재는 개발 환경, 공통 레이아웃, 라우팅, Query Provider를 구성한 상태입니다.
경기 API와 인증 기능은 아직 연결하지 않았습니다.

## 기술 구성

- React / TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query
- ESLint / Prettier

## 실행 환경

Node.js 22.12 이상을 사용합니다.

## 설치 및 실행

저장소 루트에서 실행합니다.

```bash
cd frontend
npm ci
npm run dev
```

개발 서버: http://localhost:5173

## 코드 검사

frontend 디렉터리에서 실행합니다.

```bash
npm run typecheck
npm run lint
npm run format:check
```

포맷을 수정하려면 다음 명령을 실행합니다.

```bash
npm run format
```

## 빌드 및 미리보기

```bash
npm run build
npm run preview
```

빌드 결과는 dist 디렉터리에 생성됩니다.
미리보기 서버: http://localhost:4173

## 개발 API 프록시

개발 서버의 /api 요청은 http://localhost:8080으로 전달됩니다.

- 프론트 요청: /api/v1/games
- 백엔드 전달: http://localhost:8080/api/v1/games

실제 API 연결에는 Spring Boot 서버 실행이 필요합니다.
운영 배포에서는 별도의 API 전달 설정이 필요합니다.

## 구조

- src/app: 전역 Provider, 라우터, 공통 레이아웃
- src/pages: 경로별 페이지
- src/features: 기능별 API와 로직을 추가할 위치
- src/shared: 공통 스타일과 유틸리티

## 현재 경로

- /: /games로 이동
- /games: 초기 경기 소개 화면
- 등록되지 않은 주소: 404 화면
