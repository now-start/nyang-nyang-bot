# 기술/운영 부록

- 문서 상태: MVP PRD v1
- 작성일: 2026-05-07
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 정책 문서:
  - [호감도 포인트 제도](point-system.md)
  - [호감도/업보/쿠폰 카탈로그](reward-catalog.md)
  - [치지직 후원 룰렛과 OBS 오버레이](roulette-overlay.md)
  - [관리자 명령어 관리 PRD](command-management.md)
  - [클린 아키텍처](architecture.md)
  - [API 명세](api.md)
  - [이벤트 명세](events.md)
  - [요구사항 추적표](requirements-traceability.md)
  - [데이터 모델/마이그레이션 계획](data-model-migration.md)
  - [테스트 전략](test-strategy.md)
  - [운영/배포 Runbook](runbook.md)

## 1. API 요구사항

상세 요청/응답, 에러 코드, 페이지네이션 규칙은 [API 명세](api.md)를 따른다.

| Method | Path | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/` | 인증 | 호감도 화면으로 이동 |
| GET | `/oauth2/authorization/chzzk` | 공개 | Spring Security OAuth2 인증 시작 |
| GET | `/login/oauth2/code/chzzk` | 공개 | CHZZK OAuth 콜백 처리 |
| GET | `/favorite/list` | 인증 | 호감도 보드 조회 |
| GET | `/favorite/history` | 본인 또는 관리자 | 특정 사용자의 호감도 히스토리 조회 |
| GET | `/favorite/upbo` | 본인 또는 관리자 | 특정 사용자의 업보 반영 내역 조회 |
| GET | `/favorite/adjustments` | 관리자 | 호감도 조정 항목 조회 |
| POST | `/favorite/adjustments` | 관리자 | 호감도 조정 항목 생성 |
| POST | `/favorite/adjustments/apply` | 관리자 | 호감도 조정 적용 |
| POST | `/attendance/start` | 관리자 | 출석 수집 시작 |
| POST | `/attendance/stop` | 관리자 | 출석 수집 종료 |
| GET | `/attendance/users` | 관리자 | 출석 수집 대상자 조회 |
| POST | `/attendance/apply` | 관리자 | 출석 보상 적용 |
| GET | `/google/sync` | 관리자 | 전환 기간의 Google Sheets 포인트 마이그레이션 |
| GET | `/chzzk/connect` | 관리자 | CHZZK 소켓 수동 연결 |
| GET | `/admin/upbo/templates` | 관리자 | 업보 결과 템플릿 조회 |
| POST | `/admin/upbo/templates` | 관리자 | 업보 결과 템플릿 생성 |
| PATCH | `/admin/upbo/templates/{id}` | 관리자 | 업보 결과 템플릿 수정/비활성화 |
| POST | `/admin/upbo/apply` | 관리자 | 업보 수동 적용 |
| POST | `/admin/upbo/corrections` | 관리자 | 업보 정정 거래 생성 |
| GET | `/admin/roulette/tables` | 관리자 | 룰렛 확률표 목록 조회 |
| POST | `/admin/roulette/tables` | 관리자 | 룰렛 확률표 생성 |
| PATCH | `/admin/roulette/tables/{id}` | 관리자 | 룰렛 확률표 수정/활성화 |
| POST | `/admin/roulette/results/{id}/complete` | 관리자 | 룰렛 결과 처리 완료 |
| POST | `/admin/roulette/results/{id}/corrections` | 관리자 | 룰렛 결과에 연결된 보정 거래 생성 |
| GET | `/admin/commands` | 관리자 | 명령어 목록 조회 |
| POST | `/admin/commands` | 관리자 | 명령어 생성 |
| PATCH | `/admin/commands/{id}` | 관리자 | 명령어 수정, 활성/비활성 변경 |
| POST | `/admin/commands/preview` | 관리자 | 메시지 템플릿 미리보기 |
| POST | `/admin/commands/validate` | 관리자 | 저장 전 충돌/변수 검증 |
| GET | `/admin/overlay-tokens` | 관리자 | OBS 오버레이 토큰 목록 조회 |
| POST | `/admin/overlay-tokens` | 관리자 | OBS 오버레이 토큰 발급 |
| POST | `/admin/overlay-tokens/{id}/rotate` | 관리자 | 기존 OBS 오버레이 토큰을 즉시 폐기하고 새 토큰 재발급 |
| POST | `/admin/overlay-tokens/{id}/revoke` | 관리자 | OBS 오버레이 토큰 폐기 |
| POST | `/admin/roulette/tables/{id}/simulate` | 관리자 | 활성화 전 가상 1만 회 시뮬레이션 분포 미리보기 |
| GET | `/favorite/me/roulette/recent` | 인증 | 본인 최근 룰렛 결과 조회 |
| GET | `/overlay/roulette` | 공개 | OBS 브라우저 소스용 투명 룰렛 오버레이 페이지 |
| GET | `/overlay/roulette/events` | 오버레이 토큰 | `Authorization: Bearer` 헤더로 OBS 오버레이가 표시할 서버 확정 룰렛 이벤트 조회 |
| POST | `/roulette/results` | 시스템 | 서버에서 확정한 룰렛 결과 반영 |
| POST | `/overlay/roulette/events/{id}/displayed` | 오버레이 토큰 | OBS 오버레이 표시 완료 상태 기록 |
| POST | `/admin/overlay/roulette/events/{id}/replay` | 관리자 | 기존 룰렛 결과를 원장 재반영 없이 OBS 오버레이에 재송출 |
| GET | `/actuator/**` | 공개 | 운영 상태 확인 |
| GET | `/v3/api-docs` | 공개 | OpenAPI JSON |

## 2. 데이터 모델

### 2.1 현행 모델

- `FavoriteAccount`: 사용자 ID, 닉네임, 현재 호감도. 사용자 미확인 배지를 위해 `lastSeenAt` 컬럼 추가가 필요하다.
- `FavoriteHistory`: 변경 사유, 변경 후 호감도, 대상 사용자. 현행 모델이며 업보 조회를 위해서는 증감값/출처/공개 설명/닉네임 스냅샷 확장이 필요하다.
- `FavoriteAdjustment`: 수동 조정 항목 라벨과 증감값
- `AuthorizationAccount`: CHZZK OAuth 토큰과 관리자 여부
- `WeeklyChatRank`: 주차별 채팅 수 집계
- `Donation`, `Subscription`: 후원/구독 이벤트 저장 후보

### 2.2 추가 후보

`UpboTemplate`:

- `id`, `label`, `amount`, `description`, `active`, `displayOrder`
- `exchangeFavoriteValue`: 호감도 환산값. 환산 불가 업보는 null
- `rewardType`: `FAVORITE`, `COUPON`, `MISSION`, `PARTICIPATION_PRIORITY`, `CUSTOM`
- `conversionMode`: `AUTO`, `MANUAL`, `NONE`
- `createDate`, `modifyDate`

`UserUpbo`:

- `id`, `userId`, `upboTemplateId`
- `nickNameSnapshot`: 획득/사용 당시 표시 닉네임
- `label`, `status`: `OWNED`, `CONVERTED`, `USED`, `CORRECTED`
- `exchangeFavoriteValue`: 호감도 환산값. 환산 불가 업보는 null
- `conversionMode`: `AUTO`, `MANUAL`, `NONE`
- `sourceType`: `UPBO_MANUAL`, `UPBO_ROULETTE`, `ADMIN_GRANT`
- `ledgerId`: 호감도로 환산되었거나 사용 처리된 경우 연결 원장 ID
- `publicDescription`, `privateMemo`
- `createDate`, `modifyDate`

`RouletteEvent`:

- `id`, `donationEventId`, `donatorChannelId`, `donatorNickname`
- `payAmount`, `donationText`, `command`, `roundCount`, `status`: `CREATED`, `RESULT_CONFIRMED`, `APPLIED`, `CORRECTED`
- `animatedRoundCount`, `summaryRoundCount`
- `rouletteTableId`
- `tableVersionSnapshot`: 후원 도착 시점에 활성이던 룰렛 테이블의 version
- `tableActiveSnapshot`: 후원 도착 시점 활성 여부
- `itemsSnapshotJson`: 후원 도착 시점의 항목 라벨/확률/결과 타입 스냅샷. 테이블 변경 후에도 동일 결과를 재현 가능하게 한다.
- `idempotencyKey`: CHZZK 후원 이벤트 ID. 동일 키 재수신은 모두 무시한다. **DB UNIQUE 제약**으로 중복 insert를 방지한다.
- `donationEventId`도 **UNIQUE 제약**으로 잡아 idempotencyKey와 동일한 의미를 보장한다.
- `appliedAt`: 원장/보유 목록 반영 완료 시각
- `createDate`, `modifyDate`

`RouletteRoundResult`:

- `id`, `rouletteEventId`, `roundNo`, `status`: `CONFIRMED`, `APPLIED`, `CORRECTED`
- `resultItemId`, `resultLabelSnapshot`, `probabilitySnapshot`
- `ledgerId`: 호감도 원장에 반영된 경우 연결 ID
- `userUpboId`: 쿠폰/업보/미션으로 보관된 경우 연결 ID
- `displayMode`: `ANIMATED`, `SUMMARY`
- `(rouletteEventId, roundNo)` **복합 UNIQUE 제약**: 동일 후원의 동일 회차가 중복 insert되지 않게 한다.
- `createDate`, `modifyDate`

`OverlayToken`:

- `id`, `name`, `scope`: 초기 scope는 `ROULETTE_OVERLAY`
- `tokenHash`: 토큰 원문은 저장하지 않고 해시만 저장
- `active`, `revokedAt`, `lastUsedAt`
- `rotatedFromTokenId`: 재발급으로 생성된 토큰인 경우 이전 토큰 ID
- `createdBy`, `createDate`, `modifyDate`

`OverlayDisplayEvent`:

- `id`, `rouletteEventId`, `overlayTokenId`, `status`: `PENDING`, `DISPLAYING`, `DISPLAYED`, `MISSED`, `FAILED`
- `payloadVersion`
- `expiresAt`: 기본값은 생성 후 120초
- `displayStartedAt`, `displayedAt`, `missedAt`, `failedAt`
- `replayOfDisplayEventId`: 재송출 이벤트인 경우 원본 표시 이벤트 ID
- `failureReason`: 표시 실패 사유. 원장 반영 취소 사유가 아니라 오버레이 상태 진단용이다.
- `createDate`, `modifyDate`

`CorrectionLedgerLink`:

- `id`, `sourceLedgerId`, `correctionLedgerId`
- `sourceType`: `UPBO_MANUAL`, `UPBO_ROULETTE`, `REWARD_USE`, `ADMIN`
- `reason`, `createdBy`, `createDate`

`RouletteTable`:

- `id`, `name`, `version`, `active`
- `command`: 전환기 표시용 룰렛 실행 명령어. 런타임 매칭의 권위 데이터는 `chat_command`의 `TRIGGER/action_key=ROULETTE_DONATION` row이다.
- `priceAmount`: 룰렛 1회 실행 금액
- `totalProbability`, `publicVisible`
- `maxAnimatedRounds`: 다회차 룰렛에서 OBS 순차 애니메이션으로 표시할 최대 회차 수
- `createDate`, `modifyDate`

`RouletteItem`:

- `id`, `rouletteTableId`, `label`, `probability`
- `requiredNoPrize`: `꽝` 필수 항목 여부. 필수 항목은 삭제하거나 0%로 설정할 수 없음
- `resultType`: `NONE`, `FAVORITE_DELTA`, `UPBO_REWARD`, `COUPON`, `STREAM_MISSION`, `PARTICIPATION_PRIORITY`
- `favoriteDelta`, `exchangeFavoriteValue`
- `publicVisible`, `active`, `displayOrder`

## 3. 외부 연동

### 3.1 CHZZK Open API

- OAuth 토큰 발급: `/auth/v1/token`
- 내 사용자 조회: `/open/v1/users/me`
- 소켓 세션 발급: `/open/v1/sessions/auth/client`
- 세션 목록 조회: `/open/v1/sessions/client`
- 채팅 이벤트 구독: `/open/v1/sessions/events/subscribe/chat`
- 후원 이벤트 구독: `/open/v1/sessions/events/subscribe/donation`
- 구독 이벤트 구독: `/open/v1/sessions/events/subscribe/subscription`
- 채팅 메시지 전송: `/open/v1/chats/send`. 룰렛 결과 자동 공지에는 사용하지 않고, 명령어 응답 등 범용 채팅봇 기능에 사용한다.

참고:

- CHZZK Developers: https://chzzk.gitbook.io/chzzk
- CHZZK Session API: https://chzzk.gitbook.io/chzzk/chzzk-api/session
- CHZZK Chat API: https://chzzk.gitbook.io/chzzk/chzzk-api/chat

### 3.2 Google Sheets API

Google Sheets는 최종 플랫폼 기능이 아니라 기존 포인트 데이터 이관을 위한 전환기 의존성이다.

- 서비스 계정 키 파일을 사용한다.
- 스프레드시트 ID와 키 파일 경로는 설정 값으로 주입한다.
- 이관 범위는 `호감도 순위표!B2:H2000`이다.
- 플랫폼 내 호감도 원장이 최종 원본 데이터가 된 뒤에는 Google Sheets API 의존성을 제거한다.

## 4. 비기능 요구사항

### 4.1 보안

- 모든 기능 페이지와 API는 기본적으로 인증이 필요해야 한다.
- 초기 MVP의 관리자 API는 서버 측 `@PreAuthorize("hasRole('ADMIN')")` 단일 권한으로 보호한다.
- 스트리머/매니저/조회 전용 관리자 같은 세부 권한은 후순위로 확장하며, Phase 일정에 묶지 않고 공동 운영자 합류·외부 매니저 위임 같은 실 수요가 발생하는 시점에 도입한다.
- 일반 사용자는 본인 히스토리 외 데이터에 접근할 수 없어야 한다.
- 일반 사용자는 다른 사용자 검색 기능을 가지지 않으며, 호감도 보드 전체 랭킹만 조회할 수 있다.
- 관리자만 닉네임 부분 문자열 일치(ILIKE 등) 검색을 수행할 수 있다.
- CHZZK access token과 refresh token은 로그에 노출되지 않아야 한다.
- OAuth `state`는 Spring Security OAuth2 Client의 세션 기반 검증을 사용한다.
- OBS 오버레이 페이지는 공개로 열 수 있지만, 룰렛 이벤트 조회와 표시 완료 API는 추측 불가능한 오버레이 토큰으로 보호해야 한다.
- OBS 오버레이 토큰은 URL query string에 넣지 않고 URL fragment와 `Authorization: Bearer` 헤더를 사용한다.
- 오버레이 토큰은 보안 난수 기반의 충분히 긴 opaque token으로 생성한다.
- 오버레이 토큰 원문은 발급/재발급 직후 한 번만 표시하고, DB에는 해시만 저장한다.
- 오버레이 토큰은 관리자 페이지에서 즉시 재발급/폐기할 수 있어야 한다.
- 오버레이 토큰은 자동 만료 시한을 두지 않는다. 관리자가 수동 폐기/재발급으로 통제한다.
- 토큰 사용 추적은 `lastUsedAt`을 Grafana에 노출하고, 미사용 토큰은 운영자가 수동으로 정리한다.
- 애플리케이션 로그, 프록시 로그, access log에는 오버레이 토큰 원문이 남지 않아야 한다.
- 오버레이 이벤트 API는 same-origin 호출만 허용하고, 불필요한 CORS 허용을 두지 않는다.
- 오버레이 페이지는 토큰 탈취 위험을 줄이기 위해 외부 스크립트 로딩을 사용하지 않는다.
- 유효하지 않은 오버레이 토큰 요청은 rate limit 또는 일시 차단 대상으로 처리한다.

### 4.2 신뢰성

- CHZZK 소켓 연결은 비정상 종료 후 자동 재연결되어야 한다.
- Google Sheets 마이그레이션 실패는 운영 로그와 모니터링에서 확인 가능해야 한다.
- 출석체크 임시 데이터는 현재 메모리 기반이므로 애플리케이션 재시작 시 사라질 수 있음을 운영자가 인지해야 한다.
- 후원 이벤트와 룰렛 결과는 중복 반영되지 않아야 한다.

### 4.3 성능

- 호감도 목록은 페이지네이션을 기본으로 제공해야 한다.
- 히스토리는 요청당 최대 50건으로 제한해야 한다.
- 주간 채팅 집계는 사용자/주차 기준 단일 레코드 갱신 방식으로 유지한다.
- Google Sheets 마이그레이션은 최대 2,000행 범위를 기준으로 처리한다.

### 4.4 운영 로그

- 모든 애플리케이션 로그는 Grafana/Loki에서 검색하기 쉽도록 logfmt(`key=value`) 포맷으로 출력한다.
- 표준 키는 다음과 같다.

| key | 의미 | 예시 |
| --- | --- | --- |
| `ts` | ISO 8601 타임스탬프 | `2026-05-08T12:34:56Z` |
| `level` | 로그 레벨 | `INFO`, `WARN`, `ERROR`, `AUDIT` |
| `actor` | 실행 주체 | `admin:{channelId}`, `system`, `user:{channelId}` |
| `action` | 동작 | `favorite.adjust`, `roulette.run`, `overlay_token.rotate` |
| `target` | 대상 | `user:{channelId}`, `token:{id}`, `roulette_table:{id}` |
| `result` | 결과 | `success`, `failure` |
| `reason` | 사유. 자유 텍스트, 공백 포함 시 큰따옴표로 감싼다. | `"rotation requested"` |
| `trace_id` | 요청 추적 ID. MDC에서 주입한다. | `abc123` |
| `before` / `after` | 상태 변화. 잔액 조정 등에 선택 사용 | `before=120 after=170` |

- AUDIT 레벨은 비-원장 관리자 행위(토큰 발급/재발급/폐기, 룰렛 테이블 활성화/변경, 권한 변경 등) 전용으로 사용한다.
- 호감도 원장 거래는 `FavoriteHistory`의 `actorId`로 추적하며, 별도 `AdminAuditLog` 테이블은 두지 않는다.
- CHZZK access/refresh token, 오버레이 토큰 원문, OAuth state 같은 민감 값은 어떤 레벨에서도 로그에 남기지 않는다.
- 사용자 닉네임은 운영 추적을 위해 로그에 남길 수 있다.

Loki 색인 정책 (카디널리티 관리):

- Loki 라벨로 색인할 키는 카디널리티가 낮은 `level`, `action`, `result`로 제한한다.
- `actor`, `target`, `reason`, `trace_id`, `before`, `after`, 닉네임 등은 라벨이 아닌 메시지 페이로드 키로만 출력해 라벨 폭발을 막는다.
- `actor`와 `target` 값은 `admin:{channelId}`, `user:{channelId}`, `system`, `token:{id}`, `roulette_table:{id}` 등 enum prefix로 정규화해 검색 시 prefix 매칭이 가능하게 한다.

## 5. 운영

- 모든 변경은 PR을 통해 `main`에 반영한다.
- `main` 병합 후 GitHub Actions에서 Build/Test, Docker Image 생성, GHCR Push, 배포를 자동 실행한다.
- Release를 PreRelease로 변경하면 롤백이 가능해야 한다.
- 운영 설정에는 DB, CHZZK OAuth, Google Sheets 전환기 설정, Spring Cloud Config, Eureka 정보가 포함된다.

## 6. 리스크

| 항목 | 현재 상태 | 리스크 | 권장 조치 |
| --- | --- | --- | --- |
| OAuth state | Spring Security OAuth2 Client 사용 | 세션 손실 시 로그인 재시도 필요 | 세션 기반 state 검증 유지 |
| CSRF | 전역 비활성화 | 상태 변경 API가 세션 쿠키 기반 요청에 노출 | CSRF 활성화 또는 API 토큰/동일 출처 정책 강화 |
| 관리자 권한 관리 | DB의 `admin` 플래그 의존 | 관리 도구 부재 시 운영 변경이 수동 DB 작업에 의존 | 관리자 승격/해제 운영 절차 또는 API 정의 |
| 출석 수집 | 인메모리 Map 사용 | 재시작/다중 인스턴스에서 데이터 유실 또는 불일치 | DB 기반 영속화 또는 다른 공유 저장소 검토 |
| 후원 룰렛 | 미구현 | 중복 반영, OBS 연결 실패, 결과 불일치 | idempotency key, 상태 머신, 재처리 정책 정의 |
| 토큰 로그 | refresh 시 토큰 DTO 로그 출력 | 민감정보 노출 가능성 | 토큰 값 마스킹 또는 로그 제거 |
| Sheets 마이그레이션 | 전체 범위 읽기 후 순차 처리 | 전환 기간 중 중복 실행 또는 원본 불일치 가능 | 이관 검증 리포트와 제거 시점 정의 |

## 7. 릴리즈 체크리스트

- `./gradlew test` 통과
- CHZZK OAuth 로그인 수동 검증
- `!호감도`, `!룰렛결과` 채팅 명령 수동 검증
- 관리자 Google Sheets 마이그레이션 수동 검증
- 관리자 호감도 조정 적용 및 히스토리 기록 검증
- 관리자 업보 템플릿 생성/수정/비활성화 검증
- 관리자 업보 수동 적용 및 정정 거래 검증
- 출석체크 사이클(활성화 → 적용/취소) 흐름 검증
- 룰렛 활성화 전 시뮬레이션 분포 미리보기 검증
- 룰렛 다회차 결과 일괄 확정 + 회차별 반영 흐름 검증
- 본인 페이지 미확인 배지/최근 룰렛 결과 노출 확인
- logfmt 포맷 로그 출력 및 민감 값 미노출 확인
- Grafana/Actuator 상태 확인
- 민감 설정과 토큰 로그 노출 여부 확인

## 8. 후순위 정책 결정

후순위 항목도 1차 동작을 고정한다. 운영 데이터가 쌓여도 아래 재검토 조건이 발생하기 전까지는 기본 동작을 유지한다.

| 항목 | 결정 | 재검토 조건 |
| --- | --- | --- |
| 등업 단계명/필요 호감도 | 등업 기능은 비활성화하고 마이페이지에 표시하지 않는다. | 출시 후 1~2개월 운영 데이터로 등업 수요가 확인될 때 |
| 주간 캐쉬백 | 주간 채팅 랭킹은 표시하되 자동 캐쉬백은 지급하지 않는다. 필요하면 관리자가 수동 지급한다. | Phase 2~3에서 실 결제/랭킹 데이터가 충분히 쌓일 때 |
| 룰렛 결과 통계 자동 편향 알람 | 활성화 전 가상 1만 회 시뮬레이션만 제공하고 실 운영 통계 알람은 제공하지 않는다. | 운영 6개월 후 자동 알람 수요가 확인될 때 |
| 외부 인증 업보 업로드 | 별도 인증 업로드 페이지 없이 외부 인증을 관리자가 확인하고 수동 적용한다. | 인증 건수가 관리자 수동 처리 한계를 넘을 때 |
| 사용자 탈퇴/개인정보 처리 | 자동 탈퇴 처리와 자동 닉네임 마스킹은 제공하지 않고 데이터를 보존한다. | 법적 요구나 명시적 운영 수요가 발생할 때 |
