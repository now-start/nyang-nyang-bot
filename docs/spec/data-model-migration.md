# 데이터 모델/마이그레이션 계획

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 기준 아키텍처: [클린 아키텍처](architecture.md)
- 관련 문서:
  - [호감도 포인트 제도](point-system.md)
  - [호감도/업보/쿠폰 카탈로그](reward-catalog.md)
  - [치지직 후원 룰렛과 OBS 오버레이](roulette-overlay.md)

## 1. 목적

현재 호감도 모델을 PRD의 원장 중심 모델로 확장하고, 업보/쿠폰/룰렛/오버레이 데이터를 추가하기 위한 단계적 데이터 모델과 마이그레이션 기준을 정의한다.

## 2. 현행 모델 요약

현재 코드 기준 핵심 모델:

| 모델 | 현행 필드 | 한계 |
| --- | --- | --- |
| `FavoriteEntity` | `userId`, `nickName`, `favorite` | 잔액은 있으나 `lastSeenAt`, 락 버전, 원장 합계 검증 기준이 부족하다. |
| `FavoriteHistoryEntity` | `id`, `history`, `favorite`, `favoriteEntity` | 증감값, 출처, 실행 주체, 정정 연결, idempotency key가 없다. |
| `AuthorizationEntity` | `channelId`, `channelName`, token 필드, `admin` | 토큰 로그/보관 정책과 권한 확장 여지가 필요하다. |
| `FavoriteAdjustmentEntity` | 조정 라벨/값 | 관리자 수동 조정 템플릿으로 유지 가능하다. |
| `DonationEntity`, `SubscriptionEntity` | 이벤트 저장 후보 | 룰렛 idempotency와 스냅샷 모델이 필요하다. |
| `WeeklyChatRankEntity` | 주간 채팅 수 집계 | 일반 채팅 랭킹 용도 유지. |

## 3. 마이그레이션 원칙

- 기존 데이터는 삭제하지 않는다.
- 원장 확장 시 기존 `FavoriteHistoryEntity.history`와 `favorite`는 호환 필드로 보존한다.
- 새 필드는 nullable로 추가한 뒤 백필하고, 검증 후 필수 제약을 강화한다.
- 잔액 변경 코드는 새 application use case를 통과하게 만든 뒤 DB 제약을 강화한다.
- 정정 거래는 원본 데이터를 수정하지 않고 새 레코드로 추가한다.
- Google Sheets 마이그레이션은 전환기 기능으로만 유지하고, 최종 원본은 DB 원장이다.
- schema migration 도구가 없다면 Phase 0에서 Flyway 또는 Liquibase 도입 여부를 결정한다.

## 4. 목표 모델

### 4.1 Favorite

`favorite_account` 또는 현행 `favorite_entity` 확장:

| 필드 | 설명 |
| --- | --- |
| `user_id` | CHZZK channelId, PK |
| `nick_name` | 최신 표시명 |
| `balance` | 현재 호감도 잔액. 현행 `favorite` 컬럼과 호환 |
| `last_seen_at` | 마이페이지 미확인 배지 기준 |
| `created_at`, `updated_at` | BaseEntity 기준 |

`favorite_ledger` 또는 현행 `favorite_history_entity` 확장:

| 필드 | 설명 |
| --- | --- |
| `id` | 원장 ID |
| `user_id` | 대상 사용자 |
| `nick_name_snapshot` | 거래 당시 닉네임 |
| `delta` | 증감값 |
| `balance_after` | 변경 후 잔액 |
| `reason` | 표시용 사유. 현행 `history`와 호환 |
| `source_type` | `ATTENDANCE`, `ADMIN`, `UPBO_MANUAL`, `UPBO_ROULETTE`, `SHEET_MIGRATION` 등 |
| `source_id` | 원본 이벤트 ID |
| `display_category` | 사용자 화면 분류 |
| `public_description` | 사용자 공개 설명 |
| `private_memo` | 관리자 전용 메모 |
| `correction_of_ledger_id` | 정정 대상 원장 ID |
| `actor_id` | 관리자 또는 시스템 실행 주체 |
| `idempotency_key` | 중복 반영 방지 키 |
| `created_at`, `updated_at` | BaseEntity 기준 |

권장 제약:

- `idempotency_key`는 null 허용, 값이 있으면 UNIQUE.
- `user_id`, `created_at` 복합 인덱스.
- `source_type`, `source_id` 복합 인덱스.
- `correction_of_ledger_id` foreign key.

### 4.2 Upbo

`upbo_template`:

| 필드 | 설명 |
| --- | --- |
| `id` | 템플릿 ID |
| `label` | 업보/쿠폰/리워드 이름 |
| `description` | 설명 |
| `active` | 활성 여부 |
| `display_order` | 표시 순서 |
| `exchange_favorite_value` | 호감도 환산값. 환산 불가면 null |
| `reward_type` | `FAVORITE`, `COUPON`, `MISSION`, `PARTICIPATION_PRIORITY`, `CUSTOM` |
| `conversion_mode` | `AUTO`, `MANUAL`, `NONE` |
| `created_at`, `updated_at` | BaseEntity 기준 |

`user_upbo`:

| 필드 | 설명 |
| --- | --- |
| `id` | 사용자 보유 항목 ID |
| `user_id` | 대상 사용자 |
| `upbo_template_id` | 템플릿 ID, 자유 입력이면 null 허용 |
| `nick_name_snapshot` | 획득/사용 당시 닉네임 |
| `label` | 당시 표시명 |
| `status` | `OWNED`, `USED`, `CONVERTED`, `CORRECTED` |
| `exchange_favorite_value` | 당시 환산값 |
| `conversion_mode` | 당시 전환 모드 |
| `source_type` | `UPBO_MANUAL`, `UPBO_ROULETTE`, `ADMIN_GRANT` |
| `ledger_id` | 호감도 원장 연결 ID |
| `public_description` | 사용자 공개 설명 |
| `private_memo` | 관리자 전용 메모 |
| `created_at`, `updated_at` | BaseEntity 기준 |

### 4.3 Roulette

추가 테이블:

- `roulette_table`
- `roulette_item`
- `roulette_event`
- `roulette_round_result`

핵심 제약:

- 활성 룰렛 테이블은 application/domain 검증으로 확률 합계 100%, 꽝 필수를 보장한다.
- `roulette_event.idempotency_key`와 `donation_event_id`는 UNIQUE.
- `roulette_round_result(roulette_event_id, round_no)`는 UNIQUE.
- `roulette_event.items_snapshot_json`은 후원 시점 확률표 재현을 위해 보존한다.

### 4.4 Overlay

추가 테이블:

- `overlay_token`
- `overlay_display_event`

핵심 제약:

- `overlay_token.token_hash`만 저장하고 토큰 원문은 저장하지 않는다.
- 재발급 시 기존 토큰은 `active=false`, `revoked_at` 설정.
- `overlay_display_event.replay_of_display_event_id`로 재송출 이벤트를 연결한다.
- 재송출 이벤트는 원장/보유 목록을 재반영하지 않는다.

## 5. 단계별 마이그레이션

### Step 1. 호환 컬럼 추가

- `FavoriteEntity`에 `lastSeenAt`을 추가한다.
- `FavoriteHistoryEntity`에 원장 확장 컬럼을 nullable로 추가한다.
- 기존 `history`는 `reason`, 기존 `favorite`는 `balanceAfter`로 해석할 수 있게 adapter에서 변환한다.

완료 기준:

- 기존 조회 API가 깨지지 않는다.
- 새 컬럼이 null이어도 현행 화면과 테스트가 동작한다.

### Step 2. 원장 백필

- 기존 히스토리의 `balanceAfter`를 채운다.
- 기존 히스토리의 `delta`는 사용자별 시간순 이전 잔액과 비교해 계산한다.
- 계산 불가한 첫 거래는 `sourceType=SHEET_MIGRATION` 또는 `ADMIN`으로 분류하고 `delta=balanceAfter`로 처리한다.
- `nickNameSnapshot`은 현재 `FavoriteEntity.nickName`으로 채운다.

완료 기준:

- 사용자별 마지막 `balanceAfter`가 `FavoriteEntity.favorite`와 일치한다.
- 불일치 사용자는 별도 리포트로 추적하고 관리자 보정 거래로 처리한다.

### Step 3. 새 유스케이스로 쓰기 전환

- 호감도 변경 코드를 `AdjustFavoriteUseCase` 계열로 집중한다.
- 출석, 관리자 조정, Sheets 마이그레이션이 새 원장 필드를 채우도록 변경한다.
- `idempotencyKey`가 있는 거래는 UNIQUE 제약으로 중복 insert를 막는다.

완료 기준:

- 새 거래는 `delta`, `balanceAfter`, `sourceType`, `actorId`가 채워진다.
- 잔액 변경과 원장 저장이 같은 트랜잭션에서 처리된다.

### Step 4. 업보 모델 추가

- `upbo_template`, `user_upbo`를 추가한다.
- 포인트 쿠폰 자동 전환은 Favorite use case를 호출해 원장에 반영한다.
- 환산 불가 업보는 `user_upbo`에만 저장한다.

완료 기준:

- 업보 보유 상태와 원장 연결이 조회 가능하다.
- 관리자 정정이 원본 거래를 삭제하지 않고 별도 레코드로 남는다.

### Step 5. 룰렛/오버레이 모델 추가

- 룰렛 테이블/아이템/이벤트/회차 결과를 추가한다.
- 오버레이 토큰/표시 이벤트를 추가한다.
- 후원 이벤트 ID 기반 idempotency와 회차별 UNIQUE 제약을 추가한다.

완료 기준:

- 동일 후원 이벤트가 재수신되어도 중복 반영되지 않는다.
- 오버레이 재송출이 원장 재반영 없이 표시 이벤트만 만든다.

### Step 6. 제약 강화

- 백필과 신규 쓰기 전환이 안정화되면 필수 컬럼에 NOT NULL 제약을 검토한다.
- `source_type`, `delta`, `balance_after`, `nick_name_snapshot`은 신규 거래 기준 필수로 둔다.
- 기존 레거시 데이터가 남아 있으면 별도 migration batch를 먼저 완료한다.

## 6. 롤백 기준

- 컬럼 추가와 신규 테이블 추가는 기존 코드와 호환되게 수행한다.
- 쓰기 전환 전에는 기존 코드 경로로 되돌릴 수 있어야 한다.
- 쓰기 전환 후 롤백이 필요하면 새 원장 거래를 삭제하지 않고 관리자 보정 거래로 정합성을 맞춘다.
- 토큰/오버레이/룰렛 이벤트처럼 외부 상태와 연결된 데이터는 물리 삭제하지 않는다.

## 7. 검증 쿼리 기준

구현 시 DB별 SQL 문법에 맞춰 다음 검증을 준비한다.

- 사용자별 현재 잔액과 마지막 원장 `balanceAfter` 비교.
- `idempotencyKey` 중복 레코드 존재 여부.
- `user_upbo.status=CONVERTED`인데 `ledger_id`가 없는 레코드 존재 여부.
- `roulette_round_result.status=APPLIED`인데 `ledger_id`와 `user_upbo_id`가 모두 없는 레코드 존재 여부.
- `overlay_token.active=true`인데 `token_hash`가 null인 레코드 존재 여부.

## 8. 미결정 사항

| 항목 | 선택지 | 결정 필요 시점 |
| --- | --- | --- |
| schema migration 도구 | Flyway, Liquibase, 수동 SQL | Phase 0 |
| 기존 테이블명 유지 여부 | 현행 entity 확장, 신규 테이블 생성 | Phase 2 시작 전 |
| `favorite` 컬럼명 변경 | 유지, `balance`로 변경 | Phase 2 시작 전 |
| 원장 백필 방식 | batch, one-off SQL, application runner | Phase 2 |
| 운영 DB migration window | 무중단, 짧은 점검 시간 | 첫 schema 변경 전 |
