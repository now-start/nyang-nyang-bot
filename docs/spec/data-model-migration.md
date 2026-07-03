# 데이터 모델/마이그레이션 계획

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 기준 아키텍처: [클린 아키텍처](architecture.md)
- 관련 문서:
  - [호감도 포인트 제도](point-system.md)
  - [호감도/업보/쿠폰 카탈로그](reward-catalog.md)
  - [치지직 후원 룰렛과 OBS 오버레이](roulette-overlay.md)
  - [관리자 명령어 관리 PRD](command-management.md)

## 1. 목적

현재 호감도 모델을 PRD의 원장 중심 모델로 확장하고, 업보/쿠폰/룰렛/오버레이 데이터를 추가하기 위한 단계적 데이터 모델과 마이그레이션 기준을 정의한다.

## 2. 현행 모델 요약

현재 코드 기준 핵심 모델:

| 모델 | 현행 필드 | 한계 |
| --- | --- | --- |
| `FavoriteAccount` | `userId`, `nickName`, `favorite` | 잔액은 있으나 `lastSeenAt`, 락 버전, 원장 합계 검증 기준이 부족하다. |
| `FavoriteHistory` | `id`, `history`, `favorite`, `favoriteAccount` | 증감값, 출처, 실행 주체, 정정 연결, idempotency key가 없다. |
| `AuthorizationAccount` | `channelId`, `channelName`, token 필드, `admin` | 토큰 로그/보관 정책과 권한 확장 여지가 필요하다. |
| `FavoriteAdjustment` | 조정 라벨/값 | 관리자 수동 조정 템플릿으로 유지 가능하다. |
| `Donation`, `Subscription` | 이벤트 저장 후보 | 룰렛 idempotency와 스냅샷 모델이 필요하다. |
| `WeeklyChatRank` | 주간 채팅 수 집계 | 일반 채팅 랭킹 용도 유지. |

## 3. 마이그레이션 원칙

- 기존 데이터는 삭제하지 않는다.
- 원장 확장 시 기존 `FavoriteHistory.history`와 `favorite`는 호환 필드로 보존한다.
- 새 필드는 nullable로 추가한 뒤 백필하고, 검증 후 필수 제약을 강화한다.
- 잔액 변경 코드는 새 application use case를 통과하게 만든 뒤 DB 제약을 강화한다.
- 정정 거래는 원본 데이터를 수정하지 않고 새 레코드로 추가한다.
- Google Sheets 마이그레이션은 전환기 기능으로만 유지하고, 최종 원본은 DB 원장이다.
- schema migration 도구는 Flyway를 사용한다.
- 엔티티 어노테이션은 ORM 매핑만 담당하고, UNIQUE/INDEX/NOT NULL 같은 DB DDL은 Flyway SQL에서만 관리한다.
- 운영 DB에는 물리 FK를 두지 않는다. 관계 컬럼은 `*_id`로 보관하고, 무결성은 application transaction, UNIQUE 제약, 명시 인덱스, 운영 검증 쿼리로 관리한다.
- 테이블/컬럼/조인 컬럼명은 Spring Boot/Hibernate 기본 물리 네이밍 전략에 맡기고, Flyway SQL도 그 결과와 동일하게 작성한다.
- 운영 설정은 config server에서 관리하므로 애플리케이션 공통 `application.yaml`에는 운영 DB migration 설정을 두지 않는다.

## 3.1 Flyway 운영 적용 기준

현재 `main` 브랜치 스키마가 상용 DB에 이미 존재하므로, 첫 Flyway 배포 전에 config server에 다음 값을 먼저 반영한다.

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
    baseline-description: "main schema before flyway"
    clean-disabled: true
    locations: "classpath:db/migration"
  jpa:
    hibernate:
      ddl-auto: validate
```

적용 순서:

1. 상용 DB 백업을 생성한다.
2. config server에 위 Flyway/JPA 설정을 먼저 반영한다.
3. 애플리케이션을 배포한다.
4. 기동 로그에서 Flyway가 기존 스키마를 `baselineVersion=1`로 기록하고 `V2`를 적용했는지 확인한다.
5. `flyway_schema_history`에 `V2__add_ledger_roulette_overlay_upbo_schema.sql`이 성공으로 남았는지 확인한다.

`baseline-on-migrate`가 없으면 기존 상용 DB에는 Flyway history가 없기 때문에 애플리케이션 시작이 실패할 수 있다.

로컬의 빈 H2 DB는 Flyway 기본값만으로 `V1`부터 적용하면 되므로 `application-local.yaml`에 baseline 설정을 두지 않는다.
로컬/test H2는 MariaDB `LONGTEXT` 메타데이터를 Hibernate `@Lob` 검증과 다르게 보고하므로 `ddl-auto=none`을 사용하고, Flyway SQL 적용 테스트로 스키마를 검증한다.
운영 MariaDB는 config server에서 `ddl-auto=validate`를 사용한다.
로컬 기본 DB는 인메모리 H2를 사용해 Flyway 도입 전 파일 DB 잔재가 마이그레이션을 오염시키지 않게 한다.

`V2__add_ledger_roulette_overlay_upbo_schema.sql`은 첫 Flyway 운영 적용 전 migration이다. 운영 또는 공유 DB에 `V2` 성공 이력이 생긴 뒤에는 같은 파일을 수정하지 않고, 새 `V3`에서 테이블 rename/copy, 제약, 인덱스 변경을 별도 migration으로 처리한다.
`V2`는 운영 main 스키마와 다른 부분 수동 반영이나 드리프트를 숨기지 않도록 컬럼/테이블/인덱스 생성 DDL을 엄격하게 작성한다.
운영 dump 기준 DB는 MariaDB 10.11.11이며, Flyway 도입 전 Hibernate `ddl-auto=update`로 일부 증분 컬럼이 이미 생성되어 있다.
`V1`은 이 운영 main 스키마를 baseline으로 모델링한다.
`V2`는 운영에 이미 있는 컬럼을 다시 추가하지 않고, 확인된 `favorite_history_entity` FK/인덱스(`FKnjkqgrcjhyhbc9544fedyj0a0`)를 제거한 뒤 기존 main 테이블명(`authorization_entity`, `favorite_entity`, `favorite_history_entity` 등)을 도메인 테이블명으로 rename한다.
운영 baseline의 `subscription_entity.month`는 예약어/함수명 충돌을 피하기 위해 `subscription.subscription_month`로 rename한다.
그 다음 운영에 없는 `source_type`, `source_id`와 신규 룰렛/오버레이/업보 테이블을 적용한다.
`V2`는 최종 스키마에 신규 물리 FK를 만들지 않는다.

## 4. 목표 모델

### 4.1 Favorite

`favorite_account`:

| 필드 | 설명 |
| --- | --- |
| `user_id` | CHZZK channelId, PK |
| `nick_name` | 최신 표시명 |
| `favorite` | 현재 호감도 잔액 |
| `create_date`, `modify_date` | BaseEntity 기준 |

`favorite_history`:

| 필드 | 설명 |
| --- | --- |
| `id` | 원장 ID |
| `favorite_account_user_id` | 대상 사용자 |
| `nick_name_snapshot` | 거래 당시 닉네임 |
| `delta` | 증감값 |
| `balance_after` | 변경 후 잔액 |
| `history` | 기존 표시용 사유 |
| `source_type` | `ATTENDANCE`, `ADMIN`, `UPBO_MANUAL`, `UPBO_ROULETTE`, `SHEET_MIGRATION` 등 |
| `source_id` | 원본 이벤트 ID |
| `display_category` | 사용자 화면 분류 |
| `public_description` | 사용자 공개 설명 |
| `private_memo` | 관리자 전용 메모 |
| `correction_of_ledger_id` | 정정 대상 원장 ID |
| `actor_id` | 관리자 또는 시스템 실행 주체 |
| `idempotency_key` | 중복 반영 방지 키 |
| `create_date`, `modify_date` | BaseEntity 기준 |

권장 제약:

- `idempotency_key`는 null 허용, 값이 있으면 UNIQUE.
- `favorite_account_user_id`, `create_date` 복합 인덱스.
- `source_type`, `source_id` 복합 인덱스.
- `correction_of_ledger_id`는 물리 FK 없이 운영 검증 쿼리로 orphan 여부를 확인한다.

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
| `create_date`, `modify_date` | BaseEntity 기준 |

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
| `create_date`, `modify_date` | BaseEntity 기준 |

### 4.3 Roulette

추가 테이블:

- `roulette_table`
- `roulette_item`
- `roulette_event`
- `roulette_round_result`

핵심 제약:

- 활성 룰렛 테이블은 전체에서 1개만 허용한다.
- 활성 룰렛 테이블은 application/domain 검증으로 확률 합계 100%, 꽝 필수를 보장한다.
- `roulette_event.idempotency_key`와 `donation_event_id`는 UNIQUE.
- `roulette_round_result(roulette_event_id, round_no)`는 UNIQUE.
- 룰렛 테이블/이벤트/회차 연결은 물리 FK 없이 `roulette_table_id`, `roulette_event_id`와 인덱스로 관리한다.
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

### 4.5 Chat Command

추가 테이블:

- `chat_command`

핵심 필드:

- `type`, `trigger`, `action_key`, `message_template`
- `timer_interval_minutes`, `timer_min_chat_count`
- `active`, `required_role`, `user_cooldown_seconds`
- `created_by`, `updated_by`, `create_date`, `modify_date`

핵심 제약:

- `TEXT`, `TRIGGER`, `TIMER`는 하나의 `chat_command` 테이블에서 관리한다.
- 별칭 테이블은 만들지 않는다.
- `TEXT`, `TRIGGER`는 하나의 `trigger`를 가진다. `TIMER`는 `trigger` null을 허용한다.
- `TRIGGER`는 allow-list 기반 `action_key`가 필요하다.
- 룰렛 후원 명령어도 `TRIGGER/action_key=ROULETTE_DONATION` row로 저장한다. 어떤 룰렛을 실행할지는 현재 활성 룰렛 테이블 1개로 결정한다.
- 저장 전 정규화한 `trigger`에 unique index를 둔다. MariaDB는 unique index에서 null 중복을 허용하므로 `TIMER`의 `trigger=null`은 여러 row를 저장할 수 있다.
- P1 템플릿 변수 중 DB 조회 값은 `{favorite}` 하나만 제공한다. 이 값은 마이그레이션된 `favorite_account.favorite`를 읽는다.
- 업보/쿠폰, 룰렛 결과, 랭킹 변수는 P1에서 제공하지 않는다.
- 타이머 interval 시작 시각과 채팅 수 같은 실행 상태는 `chat_command` 정의 컬럼에 넣지 않는다. P2에서 런타임 상태 관리 방식으로 결정한다.

## 5. 단계별 마이그레이션

### Step 1. 호환 컬럼 추가

- `FavoriteAccount`에 `lastSeenAt`을 추가한다.
- `FavoriteHistory`에 원장 확장 컬럼을 nullable로 추가한다.
- 기존 `history`는 `reason`, 기존 `favorite`는 `balanceAfter`로 해석할 수 있게 adapter에서 변환한다.

완료 기준:

- 기존 조회 API가 깨지지 않는다.
- 새 컬럼이 null이어도 현행 화면과 테스트가 동작한다.

### Step 2. 원장 백필

- 기존 히스토리의 `balanceAfter`를 채운다.
- 기존 히스토리의 `delta`는 사용자별 시간순 이전 잔액과 비교해 계산한다.
- 계산 불가한 첫 거래는 `sourceType=SHEET_MIGRATION` 또는 `ADMIN`으로 분류하고 `delta=balanceAfter`로 처리한다.
- `nickNameSnapshot`은 현재 `FavoriteAccount.nickName`으로 채운다.

완료 기준:

- 사용자별 마지막 `balanceAfter`가 `FavoriteAccount.favorite`와 일치한다.
- `{favorite}` 템플릿 변수는 이 검증이 끝난 `favorite_account.favorite` 값을 읽는다.
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

### Step 6. 명령어 모델 추가

- `chat_command`를 추가한다.
- 기존 `!호감도`, `!룰렛결과`를 `TRIGGER` row로 seed한다.
- 룰렛 테이블 명령어는 `TRIGGER/action_key=ROULETTE_DONATION` row로 backfill한다.
- `roulette_table.command`는 전환기 표시용 사본으로만 유지하고 런타임 매칭은 `chat_command`를 기준으로 한다.
- 트리거 중복은 `trigger` unique index로 차단한다.

완료 기준:

- `TEXT`, `TRIGGER`는 활성 여부와 관계없이 같은 트리거를 동시에 가질 수 없다.
- `TIMER`는 `trigger=null`로 여러 row를 저장할 수 있다.
- 룰렛 테이블 명령어 변경과 `chat_command` 변경이 같은 트랜잭션에서 처리된다.

### Step 7. 제약 강화

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

- 사용자별 현재 잔액과 마지막 원장 `balance_after` 비교.
- `idempotency_key` 중복 레코드 존재 여부.
- `favorite_history.favorite_account_user_id`가 `favorite_account.user_id`에 없는 레코드 존재 여부.
- `roulette_round_result.roulette_event_id`가 `roulette_event.id`에 없는 레코드 존재 여부.
- `overlay_display_event.roulette_event_id`가 `roulette_event.id`에 없는 레코드 존재 여부.
- `user_upbo.status=CONVERTED`인데 `ledger_id`가 없는 레코드 존재 여부.
- `roulette_round_result.status=APPLIED`인데 `ledger_id`와 `user_upbo_id`가 모두 없는 레코드 존재 여부.
- `overlay_token.active=true`인데 `token_hash`가 null인 레코드 존재 여부.

## 8. 미결정 사항

| 항목 | 선택지 | 결정 필요 시점 |
| --- | --- | --- |
| schema migration 도구 | Flyway로 결정 | 완료 |
| 기존 테이블명 유지 여부 | V2에서 도메인 테이블명으로 rename | 완료 |
| 운영 DB 물리 FK 사용 여부 | 사용하지 않음. UNIQUE/INDEX와 application transaction으로 관리 | 완료 |
| `favorite` 컬럼명 변경 | 기존 운영 컬럼명 `favorite` 유지 | 완료 |
| 원장 백필 방식 | batch, one-off SQL, application runner | Phase 2 |
| 운영 DB migration window | 무중단, 짧은 점검 시간 | 첫 schema 변경 전 |
