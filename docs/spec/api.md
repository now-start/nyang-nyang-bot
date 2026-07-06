# API 명세

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

이 문서는 Nyang-Nyang Bot의 HTTP API, 화면 라우트, 권한, 요청/응답 규칙, 에러 형식, 페이지네이션 기준을 정의한다. 상세 구현은 Spring MVC adapter가 담당하고, 비즈니스 처리는 application use case로 위임한다.

## 2. 공통 규칙

### 2.1 인증과 권한

| 권한 | 의미 |
| --- | --- |
| 공개 | 인증 없이 접근 가능 |
| 인증 | CHZZK OAuth 로그인 필요 |
| 본인 또는 관리자 | 요청 대상 `channelId`가 본인이거나 관리자 |
| 관리자 | `AuthorizationAccount.admin = true` |
| 오버레이 토큰 | `Authorization: Bearer` 헤더의 유효한 오버레이 토큰 |
| 시스템 | 내부 서버 use case 또는 scheduler 전용 |

원칙:

- 일반 사용자는 다른 사용자의 상세 히스토리, 업보 내역, 최근 룰렛 결과를 조회할 수 없다.
- 관리자는 닉네임 부분 일치 검색과 전체 히스토리 조회를 수행할 수 있다.
- 오버레이 페이지 자체는 공개할 수 있지만 이벤트 조회/표시 완료 API는 오버레이 토큰이 필요하다.
- 관리자 상태 변경 API는 Spring Security와 application use case에서 모두 권한 의도를 명확히 한다.
- `/admin/commands` 상태 변경 API는 세션 쿠키만으로 처리하지 않는다. CSRF 토큰 또는 동일 출처 JSON 요청 검증과 별도 관리자 API 토큰 중 하나를 적용한다.

### 2.2 응답 형식

JSON API의 기본 성공 응답:

```json
{
  "success": true,
  "data": {}
}
```

JSON API의 기본 실패 응답:

```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "요청 권한이 없습니다.",
    "traceId": "abc123"
  }
}
```

화면 렌더링 라우트는 HTML을 반환할 수 있다. OpenAPI JSON과 Actuator는 각 도구의 표준 응답을 따른다.

### 2.3 에러 코드

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 요청 형식, 필수값, 값 범위 오류 |
| 401 | `UNAUTHENTICATED` | 로그인 필요 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `NOT_FOUND` | 대상 리소스 없음 |
| 409 | `DUPLICATE_REQUEST` | idempotency key 또는 중복 상태 충돌 |
| 422 | `POLICY_VIOLATION` | 룰렛 확률 합계, 음수 잔액 권한 등 정책 위반 |
| 429 | `RATE_LIMITED` | 명령어/토큰 요청 제한 |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

### 2.4 페이지네이션

기본 query parameter:

| 이름 | 기본값 | 제한 |
| --- | ---: | --- |
| `page` | 0 | 0 이상 |
| `size` | 20 | 최대 50 |
| `sort` | 리소스별 기본값 | 허용된 필드만 |

히스토리 API는 요청당 최대 50건으로 제한한다.

### 2.5 시간과 금액

- 시간은 서버 응답에서 ISO 8601 문자열을 사용한다.
- 호감도와 업보 환산값은 정수로 표현한다.
- 후원 금액은 정수 원 단위로 표현한다.
- 금전성, 환전성, 현금 등가 표현은 UI/API 설명에 사용하지 않는다.

## 3. 라우트 요약

| Method | Path | 권한 | 설명 | FR |
| --- | --- | --- | --- | --- |
| GET | `/` | 인증 | 호감도 화면으로 이동 | FR-001 |
| GET | `/oauth2/authorization/chzzk` | 공개 | Spring Security OAuth2 인증 시작 | FR-001 |
| GET | `/login/oauth2/code/chzzk` | 공개 | CHZZK OAuth 콜백 처리 | FR-001 |
| GET | `/favorite/list` | 인증 | 호감도 보드 화면/조회 | FR-003 |
| GET | `/favorite/history` | 본인 또는 관리자 | 특정 사용자의 호감도 히스토리 조회 | FR-004 |
| GET | `/favorite/upbo` | 본인 또는 관리자 | 특정 사용자의 업보 반영 내역 조회 | FR-005 |
| GET | `/favorite/adjustments` | 관리자 | 호감도 조정 항목 조회 | FR-007 |
| POST | `/favorite/adjustments` | 관리자 | 호감도 조정 항목 생성 | FR-007 |
| POST | `/favorite/adjustments/apply` | 관리자 | 호감도 조정 적용 | FR-007 |
| POST | `/attendance/start` | 관리자 | 출석 수집 시작 | FR-008 |
| POST | `/attendance/stop` | 관리자 | 출석 수집 종료 | FR-008 |
| GET | `/attendance/users` | 관리자 | 출석 수집 대상자 조회 | FR-008 |
| POST | `/attendance/apply` | 관리자 | 출석 보상 적용 | FR-008 |
| POST | `/google/sync` | 관리자 | 전환 기간의 Google Sheets 포인트 마이그레이션 | FR-010 |
| GET | `/chzzk/connect` | 관리자 | CHZZK 소켓 수동 연결 | FR-016 |
| GET | `/admin/upbo/templates` | 관리자 | 업보 결과 템플릿 조회 | FR-011 |
| POST | `/admin/upbo/templates` | 관리자 | 업보 결과 템플릿 생성 | FR-011 |
| PATCH | `/admin/upbo/templates/{id}` | 관리자 | 업보 결과 템플릿 수정/비활성화 | FR-011 |
| POST | `/admin/upbo/apply` | 관리자 | 업보 수동 적용 | FR-012 |
| POST | `/admin/upbo/corrections` | 관리자 | 업보 정정 거래 생성 | FR-012 |
| GET | `/admin/roulette/tables` | 관리자 | 룰렛 확률표 목록 조회 | FR-020 |
| POST | `/admin/roulette/tables` | 관리자 | 룰렛 확률표 생성 | FR-020 |
| PATCH | `/admin/roulette/tables/{id}` | 관리자 | 룰렛 확률표 수정/활성화 | FR-020 |
| POST | `/admin/roulette/tables/{id}/simulate` | 관리자 | 활성화 전 1만 회 시뮬레이션 | FR-020 |
| POST | `/admin/roulette/results/{id}/complete` | 관리자 | 룰렛 결과 처리 완료 | FR-019 |
| POST | `/admin/roulette/results/{id}/corrections` | 관리자 | 룰렛 결과 보정 거래 생성 | FR-021 |
| GET | `/admin/commands` | 관리자 | 명령어 목록 조회 | FR-022 |
| POST | `/admin/commands` | 관리자 | 명령어 생성 | FR-022 |
| PATCH | `/admin/commands/{id}` | 관리자 | 명령어 수정, 활성/비활성 변경 | FR-022 |
| POST | `/admin/commands/preview` | 관리자 | 메시지 템플릿 미리보기 | FR-022 |
| POST | `/admin/commands/validate` | 관리자 | 저장 전 충돌/변수 검증 | FR-022 |
| GET | `/favorite/me/roulette/recent` | 인증 | 본인 최근 룰렛 결과 조회 | FR-019 |
| GET | `/admin/overlay-tokens` | 관리자 | OBS 오버레이 토큰 목록 조회 | FR-024 |
| POST | `/admin/overlay-tokens` | 관리자 | OBS 오버레이 토큰 발급 | FR-024 |
| POST | `/admin/overlay-tokens/{id}/rotate` | 관리자 | OBS 오버레이 토큰 재발급 | FR-024 |
| POST | `/admin/overlay-tokens/{id}/revoke` | 관리자 | OBS 오버레이 토큰 폐기 | FR-024 |
| GET | `/overlay/roulette` | 공개 | OBS 브라우저 소스용 오버레이 페이지 | FR-018 |
| GET | `/overlay/roulette/events` | 오버레이 토큰 | 표시할 서버 확정 룰렛 이벤트 조회 | FR-018 |
| POST | `/overlay/roulette/events/{id}/displayed` | 오버레이 토큰 | OBS 오버레이 표시 완료 기록 | FR-018 |
| POST | `/admin/overlay/roulette/events/{id}/replay` | 관리자 | 룰렛 결과 재송출 | FR-025 |
| POST | `/roulette/results` | 시스템 | 서버 확정 룰렛 결과 반영 | FR-019 |
| GET | `/actuator/**` | 공개 또는 운영 제한 | 운영 상태 확인 | 운영 |
| GET | `/v3/api-docs` | 공개 또는 운영 제한 | OpenAPI JSON | 운영 |

## 4. 핵심 API 상세

### 4.1 호감도 히스토리 조회

`GET /favorite/history`

Query:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `channelId` | 조건부 | 관리자가 특정 사용자를 조회할 때 사용. 일반 사용자는 생략하거나 본인만 가능 |
| `page` | 아니오 | 기본 0 |
| `size` | 아니오 | 최대 50 |

응답 data:

```json
{
  "items": [
    {
      "ledgerId": 1,
      "channelId": "abc",
      "nickNameSnapshot": "viewer",
      "delta": 10,
      "balanceAfter": 132,
      "sourceType": "ATTENDANCE",
      "displayCategory": "출석",
      "publicDescription": "깜짝 출석",
      "correction": false,
      "createdAt": "2026-05-08T12:34:56+09:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100
}
```

### 4.2 업보 수동 적용

`POST /admin/upbo/apply`

Request:

```json
{
  "targetChannelId": "abc",
  "templateId": 1,
  "label": "호감도 +10",
  "exchangeFavoriteValue": 10,
  "conversionMode": "AUTO",
  "publicDescription": "룰렛 결과",
  "privateMemo": "관리자 확인"
}
```

규칙:

- `templateId`가 있으면 템플릿 기준으로 적용한다.
- 자유 입력이면 `label`, `publicDescription`, `privateMemo`를 필수로 둔다.
- `conversionMode=AUTO`이면 Favorite use case로 원장 거래를 생성한다.
- 관리자만 음수 잔액을 만들 수 있다.

### 4.3 룰렛 테이블 활성화

`PATCH /admin/roulette/tables/{id}`

Request:

```json
{
  "active": true,
  "command": "!룰렛",
  "priceAmount": 1000,
  "maxAnimatedRounds": 5,
  "items": [
    {
      "label": "꽝",
      "probability": 30.0,
      "resultType": "NONE",
      "requiredNoPrize": true
    }
  ]
}
```

활성화 검증:

- 다른 활성 룰렛 테이블이 있으면 활성화할 수 없다.
- 전체 확률 합계 100%.
- `꽝` 필수.
- `꽝` 0% 금지.
- 명령어와 1회 금액 필수.

### 4.4 오버레이 이벤트 조회

`GET /overlay/roulette/events`

Headers:

```http
Authorization: Bearer <overlay-token>
```

응답 data:

```json
{
  "eventId": 100,
  "donatorNickname": "viewer",
  "roundCount": 11,
  "animatedRoundCount": 5,
  "summaryRoundCount": 6,
  "rounds": [
    {
      "roundNo": 1,
      "displayMode": "ANIMATED",
      "label": "호감도 +10",
      "resultType": "FAVORITE_DELTA"
    }
  ],
  "expiresAt": "2026-05-08T12:36:56+09:00"
}
```

규칙:

- 토큰은 URL query string으로 받지 않는다.
- 120초 내 가져가지 못한 이벤트는 `MISSED`로 남긴다.
- 조회 API는 결과를 결정하지 않고 확정된 결과만 반환한다.

## 5. API 변경 관리

- API 변경은 관련 FR ID와 함께 [요구사항 추적표](requirements-traceability.md)를 갱신한다.
- 요청/응답 DTO 변경은 controller test 또는 OpenAPI snapshot test로 검증한다.
- 사용자 권한 변경은 보안 테스트를 같이 추가한다.
- 기존 화면 라우트와 JSON API가 같은 path를 공유하면 반환 형식을 명확히 분리한다.
