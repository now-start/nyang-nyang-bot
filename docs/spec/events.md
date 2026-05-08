# 이벤트 명세

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 관련 문서:
  - [치지직 후원 룰렛과 OBS 오버레이](roulette-overlay.md)
  - [데이터 모델/마이그레이션 계획](data-model-migration.md)
  - [API 명세](api.md)

## 1. 범위

이 문서는 CHZZK 채팅/후원 이벤트, 내부 호감도 원장 이벤트, 룰렛 이벤트, 오버레이 표시 이벤트의 상태, 식별자, 중복 방지, 재처리 기준을 정의한다.

## 2. 이벤트 원칙

- 외부 이벤트 DTO는 adapter에서 application command로 변환한다.
- 외부 이벤트 원문 전체를 domain model로 사용하지 않는다.
- 중복 반영 가능성이 있는 이벤트는 idempotency key를 가진다.
- 원장에 반영된 이벤트는 삭제하지 않고 정정 거래로 보정한다.
- 오버레이 표시는 원장 반영의 선행 조건이 아니다.

## 3. 이벤트 목록

| 이벤트 | 발생 주체 | 소비 주체 | 목적 |
| --- | --- | --- | --- |
| `ChzzkChatReceived` | CHZZK chat adapter | Chat/Attendance use case | 채팅 랭킹, 출석 대상 수집, 명령 처리 |
| `ChzzkDonationReceived` | CHZZK donation adapter | Roulette use case | 후원 명령 기반 룰렛 실행 |
| `FavoriteLedgerRecorded` | Favorite use case | 조회/운영 로그 | 호감도 원장 거래 기록 |
| `UserUpboGranted` | Upbo use case, Roulette use case | 조회/미확인 배지 | 업보/쿠폰/리워드 보유 항목 생성 |
| `RouletteEventCreated` | Roulette use case | Roulette result confirmer | 후원 기반 룰렛 실행 요청 저장 |
| `RouletteRoundConfirmed` | Roulette use case | Roulette result applier | 회차별 결과 확정 |
| `RouletteRoundApplied` | Roulette result applier | Overlay event publisher | 원장/보유 목록 반영 완료 |
| `OverlayDisplayEventCreated` | Overlay publisher | OBS overlay | 화면 표시 대상 생성 |
| `OverlayDisplayEventDisplayed` | OBS overlay | Overlay use case | 표시 완료 기록 |
| `OverlayDisplayEventMissed` | Overlay use case/scheduler | 관리자 조회 | 표시 유효시간 초과 |

## 4. 외부 이벤트

### 4.1 CHZZK Chat

필수 매핑:

| 필드 | 설명 |
| --- | --- |
| `channelId` | 방송 채널 ID |
| `senderChannelId` | 채팅 작성자 CHZZK channelId |
| `senderNickname` | 채팅 작성자 닉네임 |
| `content` | 채팅 내용 |
| `receivedAt` | 서버 수신 시각 |

처리:

- 일반 채팅은 호감도를 지급하지 않고 주간 채팅 랭킹에만 반영한다.
- 출석체크 활성화 중이면 대상자로 수집한다.
- `!호감도`, `!룰렛결과` 같은 명령어는 사용자별 쿨타임을 적용한다.

### 4.2 CHZZK Donation

필수 매핑:

| 필드 | 설명 |
| --- | --- |
| `donationEventId` | CHZZK 후원 이벤트 ID. idempotency key |
| `channelId` | 방송 채널 ID |
| `donatorChannelId` | 후원자 CHZZK channelId |
| `donatorNickname` | 후원자 닉네임 |
| `payAmount` | 후원 금액 |
| `donationText` | 후원 메시지 |
| `receivedAt` | 서버 수신 시각 |

처리:

- 후원 메시지에서 룰렛 명령어를 토큰 단위 정확 일치로 찾는다.
- 명령어가 없으면 룰렛을 실행하지 않는다.
- 회차 수는 `floor(payAmount / roulette.priceAmount)`이다.
- `donationEventId` 재수신은 모두 무시한다.

## 5. 룰렛 상태

### 5.1 RouletteEvent 상태

| 상태 | 의미 | 다음 상태 |
| --- | --- | --- |
| `CREATED` | 후원 이벤트를 저장했으나 결과 미확정 | `RESULT_CONFIRMED`, `CORRECTED` |
| `RESULT_CONFIRMED` | 전체 회차 결과가 확정됨 | `APPLIED`, `CORRECTED` |
| `APPLIED` | 모든 회차가 원장/보유 목록에 반영됨 | `CORRECTED` |
| `CORRECTED` | 관리자 보정 거래가 생성됨 | 종료 |

### 5.2 RouletteRoundResult 상태

| 상태 | 의미 | 다음 상태 |
| --- | --- | --- |
| `CONFIRMED` | 회차 결과 확정, 아직 미반영 | `APPLIED`, `CORRECTED` |
| `APPLIED` | 원장 또는 보유 목록에 반영됨 | `CORRECTED` |
| `CORRECTED` | 보정 거래가 연결됨 | 종료 |

재처리:

- `CONFIRMED`에 머문 회차는 scheduler 또는 관리자 도구로 재반영할 수 있다.
- 재반영은 회차별 idempotency key 또는 `(rouletteEventId, roundNo)` UNIQUE 제약으로 중복을 막는다.

## 6. 오버레이 상태

### 6.1 OverlayDisplayEvent 상태

| 상태 | 의미 | 다음 상태 |
| --- | --- | --- |
| `PENDING` | 표시 대기 | `DISPLAYING`, `MISSED`, `FAILED` |
| `DISPLAYING` | OBS가 이벤트를 가져감 | `DISPLAYED`, `FAILED` |
| `DISPLAYED` | OBS 표시 완료 보고 수신 | 종료 |
| `MISSED` | 120초 내 가져가지 못함 | 재송출로 새 `PENDING` 생성 |
| `FAILED` | 표시 실패 보고 또는 서버 오류 | 재송출로 새 `PENDING` 생성 |

규칙:

- 오버레이 이벤트 기본 유효시간은 120초이다.
- `MISSED` 이벤트는 자동 재생하지 않는다.
- 재송출은 새 `OverlayDisplayEvent`를 만들고 `replayOfDisplayEventId`로 원본을 연결한다.
- 재송출은 호감도 원장과 업보 보유 목록을 다시 반영하지 않는다.

## 7. Idempotency Key

| 흐름 | Key | 제약 |
| --- | --- | --- |
| CHZZK 후원 룰렛 | `donationEventId` | `RouletteEvent.idempotencyKey` UNIQUE |
| 룰렛 회차 결과 | `rouletteEventId + roundNo` | 복합 UNIQUE |
| 호감도 원장 | source별 key | 값이 있으면 UNIQUE |
| Google Sheets 마이그레이션 | `sheet:{range}:{userId}` 등 | 중복 실행 방지 |
| 오버레이 재송출 | 새 display event ID | 원장 재반영 없음 |

## 8. 운영 로그 이벤트

logfmt `action` 권장값:

| action | 발생 조건 |
| --- | --- |
| `favorite.adjust` | 호감도 수동 조정 |
| `favorite.correct` | 원장 보정 거래 |
| `attendance.apply` | 출석 보상 적용 |
| `upbo.apply` | 업보 수동 적용 |
| `roulette.run` | 후원 룰렛 실행 |
| `roulette.high_round` | 회차 수가 운영 임계치 초과 |
| `roulette.apply_failed` | 회차 반영 실패 |
| `overlay_token.issue` | 오버레이 토큰 발급 |
| `overlay_token.rotate` | 오버레이 토큰 재발급 |
| `overlay_token.revoke` | 오버레이 토큰 폐기 |
| `overlay.replay` | 오버레이 재송출 |

민감 값 금지:

- CHZZK access token
- CHZZK refresh token
- OAuth state
- 오버레이 토큰 원문

## 9. 이벤트 테스트 기준

- 같은 `donationEventId`를 두 번 처리해도 원장/보유 목록은 한 번만 반영된다.
- `!룰렛!`, `!룰렛이다`는 명령어로 매칭되지 않는다.
- 다회차 룰렛 중 일부 회차 반영 실패 후 재처리 시 실패 회차만 반영된다.
- 오버레이 `DISPLAYED` 보고가 없어도 원장 반영은 취소되지 않는다.
- 재송출은 원장 저장 port를 호출하지 않는다.
