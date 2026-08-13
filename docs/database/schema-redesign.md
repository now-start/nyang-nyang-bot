# Canonical database schema

현재 데이터베이스 스키마의 단일 기준은
[`V1__canonical_schema.sql`](../../src/main/resources/db/migration/V1__canonical_schema.sql)이다.
Flyway 이력은 이 V1부터 새로 시작하며 이전 버전의 데이터베이스를 업그레이드하지 않는다.
기존 환경은 배포 전에 데이터베이스를 백업하고 재생성해야 한다.

## 운영 원칙

- 업무 테이블은 16개다.
- 런타임 DB에는 업무 trigger를 설치하지 않는다.
- 상태 전이와 업무 불변성은 애플리케이션의 서비스, 엔티티, 영속성 어댑터가 검증한다.
- DB는 PK, FK, UNIQUE, CHECK와 인덱스로 구조적 무결성을 보장한다.
- Hibernate는 `ddl-auto=validate`를 사용하며 스키마 생성과 변경은 Flyway만 담당한다.
- MariaDB system/session time zone은 `Asia/Seoul`로 통일한다.
- Flyway 실행 전에 `+09:00` session, `explicit_defaults_for_timestamp=ON`, 비-`MAXDB`
  SQL mode를 검증한다.
- 절대시각과 서울 달력 경계는 `TIMESTAMP(6)`에 저장하고 애플리케이션에서는 `Instant`를
  사용한다.

## 테이블

| 영역 | 테이블 | 역할 |
|---|---|---|
| 사용자 | `user_account` | CHZZK 사용자 계정 |
| 인증 | `oauth_credential` | 사용자별 OAuth 자격 증명 |
| 명령 | `command` | 사용자 정의 채팅 명령 |
| 명령 | `command_execution` | 승인된 명령 실행 이벤트 |
| 타이머 | `timer_message` | 타이머 설정과 선점 상태 |
| 포인트 | `point_ledger_entry` | append-only 포인트 원장 |
| 포인트 | `point_adjustment_preset` | 관리자 조정 프리셋 |
| 채팅 | `weekly_chat_count` | 서울 기준 주간 채팅 집계 |
| 후원 | `donation` | 후원 수신 사실 |
| 룰렛 | `roulette_config` | 불변 룰렛 설정 버전 |
| 룰렛 | `roulette_option` | 설정에 속한 불변 옵션 |
| 룰렛 | `roulette_run` | 후원별 룰렛 실행 |
| 룰렛 | `roulette_round` | 실행의 개별 추첨 회차 |
| 보상 | `reward_grant` | 실제 지급된 보상 |
| 오버레이 | `overlay_access_token` | 접근 토큰 해시 |
| 오버레이 | `overlay_display_job` | 원자적으로 선점하는 표시 작업 |

## 주요 불변성

- `roulette_config`와 `roulette_option`은 활성화 이후 수정하지 않고 새 버전으로 교체한다.
- 룰렛 실행은 `BUILDING` 상태로 생성하고 전체 `CONFIRMED` 회차 저장 후 `READY`로 전환한다.
- 룰렛 회차의 `APPLIED`와 `FAILED` 전이는 실행이 `READY`인 경우에만 허용한다.
- 포인트 원장은 append-only이며 잔액은 `point_ledger_entry.delta` 합계가 정본이다.
- 명령 실행 횟수와 달력일 streak는 `command_execution`에서 파생한다.
- `USER_CALENDAR_DAY` 실행의 `calendar_day_started_at`은 실행 시각이 속한 서울 날짜의
  시작 절대시각이어야 한다.
- OAuth token 원문은 로그, API 응답, 예외 메시지에 노출하지 않는다.

## 초기화 절차

1. 필요한 운영 데이터를 별도로 백업한다.
2. 기존 애플리케이션을 중지한다.
3. 기존 스키마와 `flyway_schema_history`를 제거하거나 빈 데이터베이스를 준비한다.
4. 새 애플리케이션을 시작해 Flyway V1을 적용한다.
5. Hibernate schema validation과 애플리케이션 기동을 확인한다.
6. 필요한 데이터는 현재 canonical 모델에 맞는 별도 복원 절차로 입력한다.

이전 Flyway V1~V12, Java migration, shadow table, bridge table, trigger 기반 컷오버 및
`ignore-migration-patterns` 호환 경로는 현재 artifact에 포함하지 않는다.
