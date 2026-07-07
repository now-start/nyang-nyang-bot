# HTTP 라우트 명세

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 관련 문서:
  - [클린 아키텍처](architecture.md)
  - [요구사항 추적표](requirements-traceability.md)
  - [데이터 모델/마이그레이션 계획](data-model-migration.md)
  - [웹 UI 명세](web-ui.md)
  - [OBS 오버레이 디자인 명세](overlay-design.md)
  - [관리자 명령어 관리 PRD](command-management.md)
  - [테스트 전략](test-strategy.md)

## 1. 범위

이 문서는 Nyang-Nyang Bot의 HTTP 라우트, 권한, 화면 fragment 계약, form action, 페이지네이션 기준을 정의한다.
웹 관리 화면은 Spring MVC, Thymeleaf, htmx, Bootstrap 조합으로 구성하며, 화면 조작만을 위한 JSON API는 두지 않는다.
상세 구현은 Spring MVC adapter가 담당하고, 비즈니스 처리는 application use case로 위임한다.

## 2. 공통 규칙

### 2.1 인증과 권한

| 권한 | 의미 |
| --- | --- |
| 공개 | 인증 없이 접근 가능 |
| 인증 | CHZZK OAuth 로그인 필요 |
| 본인 또는 관리자 | 요청 대상 `userId`가 본인이거나 관리자 |
| 관리자 | `AuthorizationAccount.admin = true` |
| 오버레이 토큰 | OBS 브라우저 소스 URL fragment의 `token` 값을 `Authorization: Bearer` 헤더로 전달 |
| 운영 | 배포 환경 정책에 따라 공개 또는 제한 |

원칙:

- 일반 사용자는 다른 사용자의 상세 히스토리를 조회할 수 없다.
- 관리자는 닉네임 부분 일치 검색과 전체 히스토리 조회를 수행할 수 있다.
- 오버레이 페이지 자체는 공개할 수 있지만 이벤트 fragment 조회는 오버레이 토큰이 필요하다.
- 관리자 상태 변경 route는 Spring Security와 CSRF 보호를 통과해야 한다.
- 상태 변경은 htmx form action으로 처리하고, 성공/실패는 fragment나 `HX-Trigger`로 표현한다.

### 2.2 응답 형식

| 유형 | 반환 | 사용처 |
| --- | --- | --- |
| Page | Thymeleaf full page | 최초 진입, 새로고침, 직접 URL 접근 |
| Fragment | Thymeleaf fragment HTML | htmx 부분 갱신, inline feedback, modal body |
| Redirect | HTTP redirect | 루트 진입, 인증 흐름 |
| Tool JSON | 도구 표준 응답 | `/v3/api-docs`, Actuator |

화면 route는 JSON wrapper를 사용하지 않는다.
실패는 가능한 한 같은 target 안의 feedback fragment로 표시하고, 전역 예외는 Spring MVC 예외 처리 정책을 따른다.

### 2.3 페이지네이션

기본 query parameter:

| 이름 | 기본값 | 제한 |
| --- | ---: | --- |
| `page` | 0 | 0 이상 |
| `size` | 20 | 최대 50 |

히스토리 fragment는 요청당 최대 50건으로 제한한다.

### 2.4 시간과 금액

- 화면 표시 시간은 서버에서 사용자에게 보여줄 문자열로 포맷한다.
- 호감도와 업보 환산값은 정수로 표현한다.
- 후원 금액은 정수 원 단위로 표현한다.
- 금전성, 환전성, 현금 등가 표현은 UI/route 설명에 사용하지 않는다.

## 3. 라우트 요약

| Method | Path | 권한 | 반환 | 설명 |
| --- | --- | --- | --- | --- |
| GET | `/` | 인증 | Redirect | 호감도 화면으로 이동 |
| GET | `/oauth2/authorization/chzzk` | 공개 | Redirect | Spring Security OAuth2 인증 시작 |
| GET | `/login/oauth2/code/chzzk` | 공개 | Redirect | CHZZK OAuth 콜백 처리 |
| GET | `/favorite/list` | 인증 | Page 또는 Fragment | 호감도 보드 화면, htmx 요청은 board fragment |
| GET | `/favorite/history` | 본인 또는 관리자 | Fragment | 특정 사용자의 호감도 히스토리 |
| GET | `/favorite/adjustments` | 관리자 | Fragment | 호감도 조정 항목 목록 |
| GET | `/favorite/adjustments/modal` | 관리자 | Fragment | 호감도 조정 모달 본문 |
| POST | `/favorite/adjustments` | 관리자 | Fragment | 호감도 조정 항목 생성 |
| POST | `/favorite/adjustments/apply` | 관리자 | Fragment | 호감도 조정 적용 feedback |
| GET/POST | `/attendance/users` | 관리자 | Fragment | 출석 수집 대상자 목록 |
| POST | `/attendance/start` | 관리자 | Fragment | 출석 수집 시작 feedback |
| POST | `/attendance/stop` | 관리자 | Fragment | 출석 수집 종료 feedback |
| POST | `/attendance/apply` | 관리자 | Fragment | 출석 보상 적용 feedback |
| POST | `/google/sync` | 관리자 | Fragment | Google Sheets 동기화 feedback |
| POST | `/chzzk/connect` | 관리자 | Fragment | CHZZK 소켓 수동 연결 feedback |
| GET | `/admin/roulette/tables` | 관리자 | Fragment | 룰렛 설정 region |
| GET | `/admin/roulette/tables/{tableId}/detail` | 관리자 | Fragment | 선택 룰렛 설정 region |
| POST | `/admin/roulette/tables` | 관리자 | Fragment | 룰렛 확률표 생성 |
| POST | `/admin/roulette/items` | 관리자 | Fragment | 룰렛 항목 추가 |
| POST | `/admin/roulette/tables/{tableId}/activate` | 관리자 | Fragment | 룰렛 확률표 활성화 |
| POST | `/admin/roulette/tables/{tableId}/deactivate` | 관리자 | Fragment | 룰렛 확률표 비활성화 |
| GET | `/admin/roulette/tables/{tableId}/simulation` | 관리자 | Fragment | 룰렛 시뮬레이션 결과 |
| GET | `/admin/roulette/events` | 관리자 | Fragment | 최근 룰렛 실행 목록 |
| GET | `/admin/commands` | 관리자 | Fragment | 명령어 목록 |
| GET | `/admin/commands/editor` | 관리자 | Fragment | 명령어 editor |
| POST | `/admin/commands/validate` | 관리자 | Fragment | 명령어 저장 전 검증 |
| POST | `/admin/commands/preview` | 관리자 | Fragment | 메시지 템플릿 미리보기 |
| POST | `/admin/commands` | 관리자 | Fragment | 명령어 저장 |
| POST | `/admin/commands/deactivate` | 관리자 | Fragment | 명령어 비활성화 |
| POST | `/admin/overlay/roulette/token` | 관리자 | Fragment | OBS 오버레이 URL 발급 |
| POST | `/admin/overlay/roulette/events/replay` | 관리자 | Fragment | 최근 룰렛 결과 재송출 feedback |
| POST | `/admin/overlay/roulette/events/{rouletteEventId}/replay` | 관리자 | Fragment | 특정 룰렛 결과 재송출 feedback |
| GET | `/overlay/roulette` | 공개 | Page | OBS 브라우저 소스용 오버레이 |
| GET | `/overlay/roulette/events/next` | 오버레이 토큰 | Fragment | 표시할 다음 룰렛 이벤트 |
| GET | `/actuator/**` | 운영 | Tool JSON | 운영 상태 확인 |
| GET | `/v3/api-docs` | 운영 | Tool JSON | OpenAPI 문서 |

## 4. 핵심 route 상세

### 4.1 호감도 히스토리

`GET /favorite/history`

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `userId` | 예 | 조회 대상 사용자 |
| `limit` | 아니오 | 기본 10, 최대 50 |

반환 fragment:

- `features/favorite/components :: history-grid`

### 4.2 호감도 조정 적용

`POST /favorite/adjustments/apply`

Form:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `userId` | 예 | 조정 대상 사용자 |
| `adjustmentIds` | 아니오 | 선택한 조정 항목 ID 목록 |
| `manualAmount` | 아니오 | 수동 조정값 |
| `manualHistory` | 아니오 | 수동 조정 사유 |

반환 fragment:

- `components/feedback :: alert`

성공 시 `HX-Trigger: favorite-board-refresh`로 호감도 목록을 갱신한다.

### 4.3 룰렛 관리

룰렛 관리 route는 JSON 응답을 반환하지 않고, `features/roulette/components.html`의 region fragment를 교체한다.
테이블 생성, 항목 추가, 활성화, 비활성화는 모두 form action이며 CSRF 토큰을 포함한다.

주요 fragment:

- `roulette-config-region`
- `roulette-detail`
- `roulette-events`
- `simulation-result`

활성화 검증:

- 다른 활성 룰렛 테이블이 있으면 활성화할 수 없다.
- 전체 확률 합계는 100%여야 한다.
- `꽝` 항목이 필요하며 0%일 수 없다.
- 명령어와 1회 금액이 필요하다.

### 4.4 오버레이 이벤트 조회

`GET /overlay/roulette/events/next`

Headers:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `Authorization` | 예 | `Bearer <overlay-token>` |

반환 fragment:

- `features/overlay/roulette :: overlay-wait`
- `features/overlay/roulette :: overlay-event`
- `features/overlay/roulette :: overlay-error`

규칙:

- 조회는 결과를 결정하지 않고 확정된 결과만 반환한다.
- 이벤트 fragment를 반환할 때 서버가 표시 완료 상태를 기록한다.
- 오버레이 화면은 URL fragment의 토큰을 htmx 요청 header로 변환해 다음 이벤트 fragment를 polling한다.

## 5. 변경 관리

- 화면 route 변경은 관련 FR ID와 함께 [요구사항 추적표](requirements-traceability.md)를 갱신한다.
- form/view model 변경은 controller test 또는 template rendering test로 검증한다.
- 사용자 권한 변경은 보안 테스트를 같이 추가한다.
- 화면 조작용 JSON route와 fragment route를 병행해서 남기지 않는다.
