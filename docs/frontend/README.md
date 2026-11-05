# 프론트엔드 구현 문서

React·TypeScript·Vite·Tailwind CSS 기반 농구 매칭 웹의 구현 기준 초안이다. 디자인은 사용자가 지정한 [플랩풋볼 구장 예약 화면](https://www.plabfootball.com/?date=2026-11-09&tab=stadium)의 탐색 구조를 참고한다.

2026-11-05 색상 기준: 배경 #ffffff, 본문 #212529, 대표 그린 #00c471, hover #00a760. 로고·선택 상태·주요 버튼에 대표색을 적용하며 상세 토큰은 디자인 가이드를 따른다.

| 순서 | 문서 | 역할 |
| --- | --- | --- |
| 1 | [서비스 범위·MVP](01-product-scope.md) | 사용자·핵심 여정·출시 범위 |
| 2 | [화면·정책](02-screens-and-policies.md) | 라우팅·필드·버튼 권한·시간 제한 |
| 3 | [API 연동](03-api-integration.md) | 현재 38개 API·인증/SSE·보완 작업 B-01~09 |
| 4 | [디자인 가이드](04-design-system.md) | 참고 적용·토큰·반응형·와이어프레임 |
| 5 | [구현·인수 계획](05-implementation-and-acceptance.md) | 패키지·작업 F-01~12·검증 A-01~28 |

[최초 백엔드 분석](../frontend-analysis-and-plan.md)에서 도출한 제안을 구체화했다. 실제 구현 사실과 제안·미구현 API를 구분해서 읽는다. 이번 단계는 문서 작성이며 앱 구현과 테스트 통과를 뜻하지 않는다.
