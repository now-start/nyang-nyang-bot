# Nyang-Nyang Bot Spec Implementation Plan

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 기준 Spec: [Nyang-Nyang Bot Spec](index.md)
- 기준 아키텍처: [클린 아키텍처](architecture.md)
- 관련 추적 문서:
  - [요구사항 추적표](requirements-traceability.md)
  - [데이터 모델/마이그레이션 계획](data-model-migration.md)
  - [API 명세](api.md)
  - [이벤트 명세](events.md)
  - [테스트 전략](test-strategy.md)
  - [운영/배포 Runbook](runbook.md)
- 목적: Spec의 P0/P1 요구사항을 클린 아키텍처 기준의 작업 순서, 산출물, 완료 기준으로 분해한다.

## 1. 실행 원칙

- 호감도 원장을 먼저 안정화한 뒤 출석, 업보, 룰렛, 오버레이를 같은 거래 모델 위에 올린다.
- 사용자 잔액 변경은 단일 트랜잭션과 사용자별 비관적 락으로 직렬화한다.
- 잘못된 거래는 삭제하지 않고 별도 정정 거래로 보정한다.
- 일반 사용자는 본인 데이터만 조회하고, 관리자는 전체 조회/검색/수동 처리를 수행한다.
- 금전성, 환전성, 현금 등가 표현은 제품 UI와 로그에서 사용하지 않는다.
- Google Sheets는 전환기 기능으로만 유지하고, 최종 원본 데이터는 플랫폼 DB와 원장으로 둔다.
- 도메인과 유스케이스는 Spring MVC, JPA, Feign, WebSocket, Thymeleaf 같은 외부 기술에 의존하지 않는다.
- 외부 연동, DB, 웹 컨트롤러, 보안 설정은 어댑터로 두고, 내부 유스케이스는 포트 인터페이스에만 의존한다.

## 2. 클린 아키텍처 기준

### 2.1 레이어

| 레이어 | 책임 | 예시 |
| --- | --- | --- |
| Domain | 비즈니스 규칙, 상태 전이, 정책 검증 | 호감도 잔액, 원장 거래, 업보 상태, 룰렛 확률표 |
| Application | 유스케이스 orchestration, 트랜잭션 경계, 권한 의도 표현 | 호감도 조정, 업보 적용, 출석 보상 적용, 룰렛 실행 |
| Ports | Application이 필요로 하는 저장소/외부 시스템 계약 | `FavoriteLedgerPort`, `ChzzkDonationPort`, `OverlayEventPort` |
| Adapters | Web, Persistence, External API, Overlay 같은 입출력 구현 | Spring MVC Controller, JPA Repository, Feign Client, OBS 페이지 |
| Configuration | DI, Security, Transaction, OpenAPI, 운영 설정 | Spring Security, Feign 설정, Swagger 설정 |

### 2.2 의존성 규칙

- 의존성 방향은 `Adapters -> Application -> Domain`으로만 흐른다.
- Domain은 프레임워크 annotation과 DB schema 세부사항을 모른다.
- Application은 JPA repository가 아니라 port 인터페이스를 호출한다.
- Persistence adapter는 JPA entity와 domain model 간 변환을 담당한다.
- Web adapter는 request/response DTO와 application command/result 간 변환만 담당한다.
- CHZZK, Google Sheets, Grafana/Loki, OBS 오버레이는 모두 외부 adapter로 취급한다.

### 2.3 권장 패키지 구조

현재 코드의 `controller`, `service`, `repository`, `data/entity` 구조는 한 번에 모두 옮기지 않는다. 신규 P0/P1 기능부터 아래 구조로 작성하고, 기존 기능은 작업 범위에 닿는 부분부터 점진적으로 이동한다.

```text
org.nowstart.nyangnyangbot
  domain
    favorite
    upbo
    roulette
    attendance
    auth
  application
    favorite
    upbo
    roulette
    attendance
    auth
    port
      in
      out
  adapter
    in
      web
      overlay
      chat
    out
      persistence
      chzzk
      google
      monitoring
  config
```

### 2.4 네이밍 규칙

- 사용자 액션은 inbound port/use case로 표현한다. 예: `AdjustFavoriteUseCase`, `ApplyUpboUseCase`.
- 저장소와 외부 시스템은 outbound port로 표현한다. 예: `LoadFavoriteAccountPort`, `SaveFavoriteLedgerPort`.
- JPA 구현은 adapter 하위에 둔다. 예: `FavoriteJpaEntity`, `FavoritePersistenceAdapter`.
- Web DTO와 Application command/result는 분리한다.
- Domain 객체는 `Entity` 접미사를 피하고, JPA 객체에만 `JpaEntity` 또는 현재 호환용 `Entity` 접미사를 사용한다.

## 3. 우선순위 요약

| 단계 | 범위 | PRD 우선순위 | 목적 |
| --- | --- | --- | --- |
| Phase 0 | 문서/현황 정리 | 준비 | 구현 기준과 위험 항목 정리 |
| Phase 1 | MVP 보안 안정화 | P0 | OAuth, 토큰 로그, 권한 위험 제거 |
| Phase 2 | 호감도 원장 확장 | P0 | 모든 포인트 변경의 공통 기반 확보 |
| Phase 3 | 사용자 조회/마이페이지 | P0 | 본인 히스토리, 업보 내역, 미확인 배지 제공 |
| Phase 4 | 업보/쿠폰/리워드 관리자 기능 | P0 | 수동 지급, 사용, 구매, 정정 처리 |
| Phase 5 | 출석체크/채팅 명령 정리 | P0 | 운영 중 지급 흐름과 채팅 응답 안정화 |
| Phase 6 | 후원 룰렛 | P1 | 치지직 후원 명령 기반 룰렛 실행 |
| Phase 7 | OBS 오버레이 | P1 | 확정된 룰렛 결과 화면 표시와 재송출 |
| Phase 8 | 운영/릴리즈 | 공통 | 테스트, 로그, 모니터링, 배포 검증 |

## 4. Phase 0. 문서와 현황 정리

### 작업

- PRD 요구사항과 현재 구현 기능을 기능 ID 기준으로 매핑한다.
- 현재 패키지를 클린 아키텍처 레이어 기준으로 분류한다.
  - `controller`: inbound web adapter 후보
  - `service`: application use case 후보
  - `repository`: outbound persistence/external adapter 후보
  - `data/entity`: persistence model 또는 domain model 분리 대상
  - `data/dto`: web/external/application DTO 분리 대상
- 신규 기능의 기준 패키지와 기존 기능의 점진적 이동 원칙을 확정한다.
- 문서 내 이상 항목을 정리한다.
  - `docs/spec/index.md`의 Skilljar URL은 의도된 참고 링크인지 확인 후 제거 또는 별도 참고 링크로 이동한다.
  - `docs/granafa_dashboard.json` 파일명은 `grafana_dashboard.json`로 교정할지 결정한다.
  - `reward-catalog.md`의 `00체/영어 금지 2분`과 `roulette-overlay.md`의 `00체/영어 금지 3분` 정책 차이를 확정한다.
- 현재 테스트 기준을 기록한다.

### 완료 기준

- P0/P1 요구사항별 구현 상태가 `구현됨`, `부분 구현`, `미구현`, `보류`로 분류되어 있다.
- 현재 코드가 Domain/Application/Ports/Adapters 중 어디에 속하는지 표로 정리되어 있다.
- 문서상 의도 불명 링크와 정책 충돌이 별도 이슈로 추적된다.
- 기준 테스트 명령과 현재 결과가 기록되어 있다.

## 5. Phase 1. MVP 보안 안정화

### 작업

- OAuth `state`를 요청별 난수로 생성하고 콜백에서 검증한다.
- CHZZK access token, refresh token, OAuth state가 로그에 남지 않게 제거 또는 마스킹한다.
- 관리자 API와 관리자 화면 진입점에 `ADMIN` 권한 검사를 일관되게 적용한다.
- 세션 쿠키 기반 상태 변경 요청의 CSRF 정책을 점검한다.
- `/actuator/**`, `/v3/api-docs` 공개 범위를 운영 요구에 맞게 재확인한다.
- 인증/인가 판단을 web adapter에 흩뿌리지 않고 application use case 입력 단계에서 필요한 권한 의도를 명확히 한다.
- CHZZK OAuth 연동은 outbound adapter로 격리하고, application은 토큰 발급/갱신 port만 호출한다.

### 완료 기준

- 잘못된 OAuth `state` 콜백은 인증에 실패한다.
- 토큰 갱신, 로그인, 소켓 연결 로그에 민감 값 원문이 없다.
- 일반 사용자는 관리자 API 호출 시 거부된다.
- 보안 관련 테스트가 추가되어 회귀를 막는다.
- OAuth와 CHZZK client 구현 세부사항이 domain 객체로 새지 않는다.

## 6. Phase 2. 호감도 원장 확장

### 작업

- Domain에 호감도 계정과 원장 거래 모델을 정의한다.
  - `FavoriteAccount`
  - `FavoriteLedgerEntry`
  - `FavoriteSourceType`
  - `FavoriteCorrection`
- Application에 호감도 변경 use case를 만든다.
  - `AdjustFavoriteUseCase`
  - `CorrectFavoriteLedgerUseCase`
  - `GrantFavoriteUseCase`
- Outbound port를 정의한다.
  - 호감도 계정 조회/저장
  - 원장 거래 저장
  - idempotency key 중복 확인
  - 사용자별 비관적 락 획득
- Persistence adapter에서 현행 `FavoriteEntity`, `FavoriteHistoryEntity`를 확장하거나 `FavoriteJpaEntity`, `FavoriteLedgerJpaEntity`로 분리한다.
- 원장에 필요한 필드를 추가한다.
  - `delta`
  - `balanceAfter`
  - `sourceType`
  - `sourceId`
  - `displayCategory`
  - `publicDescription`
  - `privateMemo`
  - `correctionOfLedgerId`
  - `actorId`
  - `idempotencyKey`
  - `nickNameSnapshot`
- 호감도 잔액 변경 application service를 단일 진입점으로 정리한다.
- 사용자별 비관적 락으로 잔액 갱신을 직렬화한다.
- 정정 거래 생성 규칙을 domain/application 계층에 추가한다.
- 기존 히스토리 조회 API가 확장 필드를 안전하게 반환하도록 DTO를 정리한다.

### 완료 기준

- 모든 호감도 변경은 원장 레코드와 잔액 변경이 같은 트랜잭션에서 처리된다.
- 동일 `idempotencyKey` 거래는 중복 반영되지 않는다.
- 음수 잔액 생성은 관리자 또는 시스템 정책 경로에서만 가능하다.
- 정정은 원본 거래 삭제 없이 별도 원장 거래로 남는다.
- Web adapter와 persistence adapter는 domain 규칙을 우회해 잔액을 직접 변경하지 않는다.

## 7. Phase 3. 사용자 조회와 마이페이지

### 작업

- 인증 사용자는 본인 호감도와 전체 히스토리를 조회할 수 있게 한다.
- 관리자는 특정 사용자 히스토리와 전체 사용자 데이터를 조회할 수 있게 한다.
- 일반 사용자의 닉네임 검색 기능은 제공하지 않고, 관리자 검색만 허용한다.
- `lastSeenAt` 기반 미확인 배지를 추가한다.
- 본인 업보 내역, 누적 업보 반영 합계, 반영 전/후 호감도 표시 구조를 만든다.
- 음수 잔액은 랭킹과 본인 화면에서 그대로 표시하고 시각적 신호를 제공한다.
- 조회 use case와 web response DTO를 분리한다.
- 관리자 검색은 application inbound port로 제공하고, persistence adapter가 검색 구현을 담당한다.

### 완료 기준

- 일반 사용자는 다른 사용자의 상세 히스토리에 접근할 수 없다.
- 관리자는 닉네임 부분 일치로 사용자를 검색할 수 있다.
- 마이페이지 진입 시 `lastSeenAt`이 갱신되고 미확인 배지가 해소된다.
- 히스토리는 요청당 최대 50건 제한을 따른다.
- Controller에는 권한/파라미터 변환 외 비즈니스 규칙이 없다.

## 8. Phase 4. 업보/쿠폰/리워드 관리자 기능

### 작업

- Domain에 업보/쿠폰/리워드 모델을 정의한다.
  - `UpboTemplate`
  - `UserUpbo`
  - `UpboStatus`
  - `RewardType`
  - `ConversionMode`
- Application에 관리자 처리 use case를 만든다.
  - `CreateUpboTemplateUseCase`
  - `ApplyUpboUseCase`
  - `UseRewardUseCase`
  - `PurchaseRewardUseCase`
  - `CorrectUpboUseCase`
- Persistence adapter에 `UpboTemplateJpaEntity`를 추가한다.
  - 라벨, 설명, 활성 여부, 표시 순서
  - 호감도 환산값
  - 리워드 타입
  - 전환 모드: `AUTO`, `MANUAL`, `NONE`
- Persistence adapter에 `UserUpboJpaEntity`를 추가한다.
  - 상태: `OWNED`, `USED`, `CONVERTED`, `CORRECTED`
  - 획득 당시 닉네임 스냅샷
  - 연결 원장 ID
  - 공개 설명과 관리자 메모
- 관리자 업보 템플릿 CRUD를 구현한다.
- 관리자 업보 수동 적용, 사용 처리, 쿠폰 구매 처리, 구매 즉시 사용 처리를 구현한다.
- 호감도 자동 전환 대상은 즉시 원장에 반영한다.
- 환산 불가 업보는 호감도 잔액에 포함하지 않고 보유 목록에만 표시한다.
- 업보/쿠폰/리워드 정정 거래를 구현한다.

### 완료 기준

- 관리자는 템플릿 선택 또는 자유 입력으로 업보를 적용할 수 있다.
- `FAVORITE` 자동 전환 항목은 원장과 보유 상태가 함께 갱신된다.
- `NONE` 전환 항목은 호감도 잔액을 변경하지 않는다.
- 사용 처리와 정정 처리는 사용자 히스토리에 공개된다.
- 관리자 내부 메모는 일반 사용자에게 노출되지 않는다.
- 업보 자동 전환은 호감도 use case를 통해서만 원장에 반영된다.

## 9. Phase 5. 출석체크와 채팅 명령

### 작업

- 출석체크를 활성화 사이클 단위로 명확히 모델링한다.
- 한 사이클 내 동일 사용자 1회 지급을 보장한다.
- 관리자가 입력한 양수 정수 `N`을 대상자에게 일괄 지급한다.
- 취소 시 수집 대상자에게 호감도를 지급하지 않고 사이클을 종료한다.
- `!호감도` 응답을 확장된 원장/잔액 모델에 맞춘다.
- `!룰렛결과` 본인 한정 응답을 추가한다.
- 사용자별 명령어 쿨타임 기본 30초를 적용한다.
- CHZZK 채팅 수신/발신은 chat adapter로 격리한다.
- 출석 보상 적용은 호감도 원장 use case를 호출한다.

### 완료 기준

- 같은 출석 사이클에서 동일 사용자에게 중복 지급되지 않는다.
- 일반 채팅은 호감도를 지급하지 않고 주간 채팅 랭킹에만 반영된다.
- 쿨타임 내 동일 명령 재호출은 응답하지 않는다.
- 채팅 응답에 다른 사용자의 상세 히스토리가 노출되지 않는다.
- 채팅 adapter는 명령어 파싱과 use case 호출만 담당하고 잔액을 직접 수정하지 않는다.

## 10. Phase 6. 후원 룰렛

### 작업

- Domain에 룰렛 모델과 확률 검증 규칙을 정의한다.
  - `RouletteTable`
  - `RouletteItem`
  - `RouletteEvent`
  - `RouletteRoundResult`
  - `RouletteResultType`
- Application에 룰렛 use case를 만든다.
  - `RunRouletteFromDonationUseCase`
  - `ConfirmRouletteResultsUseCase`
  - `ApplyRouletteRoundResultUseCase`
  - `ReplayRouletteOverlayEventUseCase`
- Outbound port를 정의한다.
  - 룰렛 테이블 조회/저장
  - 룰렛 이벤트/회차 결과 저장
  - CHZZK 후원 이벤트 수신
  - 호감도 원장 반영
  - 업보 보유 목록 반영
- Persistence adapter에 `RouletteTableJpaEntity`와 `RouletteItemJpaEntity`를 추가한다.
- 룰렛 활성화 검증을 구현한다.
  - 확률 합계 100%
  - `꽝` 필수
  - `꽝` 0% 금지
  - 명령어와 1회 금액 필수
- CHZZK 후원 이벤트 구독과 저장 흐름을 정리한다.
- 후원 메시지에서 관리자 설정 명령어를 토큰 단위 정확 일치로 매칭한다.
- 회차 수를 `floor(후원 금액 / 룰렛 1회 금액)`으로 계산한다.
- 후원 이벤트 ID 기반 idempotency를 적용한다.
- 후원 시점의 룰렛 테이블 버전과 항목 스냅샷을 저장한다.
- N회차 결과를 하나의 트랜잭션에서 일괄 확정한다.
- 확정된 회차별 결과를 별도 트랜잭션으로 원장/보유 목록에 반영한다.
- `CONFIRMED`까지만 도달한 회차 재처리 경로를 만든다.
- 관리자 룰렛 결과 정정 기능을 구현한다.

### 완료 기준

- 명령어가 없는 후원은 룰렛을 실행하지 않는다.
- 같은 후원 이벤트 ID는 중복 반영되지 않는다.
- 후원 처리 중 룰렛 테이블이 변경되어도 저장된 스냅샷 기준으로 끝까지 처리된다.
- 다회차 일부 반영 실패 시 실패 회차만 재처리할 수 있다.
- 룰렛 결과는 자동 채팅 공지 없이 원장과 사용자 히스토리에 반영된다.
- 랜덤 결과 선택 알고리즘은 domain service로 테스트 가능해야 한다.
- CHZZK 이벤트 DTO 변경은 external adapter 안에서 흡수된다.

## 11. Phase 7. OBS 오버레이

### 작업

- Domain 또는 application model로 오버레이 표시 이벤트 상태를 정의한다.
- Application에 오버레이 use case를 만든다.
  - `IssueOverlayTokenUseCase`
  - `RotateOverlayTokenUseCase`
  - `RevokeOverlayTokenUseCase`
  - `PollOverlayEventUseCase`
  - `MarkOverlayEventDisplayedUseCase`
  - `ReplayOverlayEventUseCase`
- Persistence adapter에 `OverlayTokenJpaEntity`를 추가한다.
- 오버레이 토큰 발급, 재발급, 폐기를 구현한다.
- 토큰 원문은 발급 또는 재발급 직후 한 번만 보여주고 DB에는 해시만 저장한다.
- OBS 페이지 `/overlay/roulette#token=...`를 구현한다.
- 오버레이 이벤트 조회 API는 `Authorization: Bearer` 헤더로 보호한다.
- long polling 또는 streaming fetch 방식으로 표시할 이벤트를 가져온다.
- 이벤트 유효시간 120초와 `MISSED` 상태를 구현한다.
- 표시 완료 보고 API를 구현한다.
- 관리자 재송출 기능을 구현한다.
- 재송출은 원장과 보유 목록을 다시 반영하지 않는다.

### 완료 기준

- 오버레이 토큰은 query string이나 서버 access log에 남지 않는다.
- 유효하지 않은 토큰은 이벤트 조회와 표시 완료 보고가 거부된다.
- 오버레이는 결과를 결정하지 않고 서버 확정 결과만 표시한다.
- 놓친 이벤트는 자동 재생되지 않고 관리자 재송출로만 다시 표시된다.
- 토큰 해싱과 검증 구현은 adapter/config 영역에 있고, application은 원문 저장을 요구하지 않는다.

## 12. Phase 8. 운영과 릴리즈

### 작업

- logfmt 표준 키를 적용한다.
  - `level`, `actor`, `action`, `target`, `result`, `reason`, `trace_id`
- 민감 값 로그 노출 여부를 릴리즈 체크리스트에 포함한다.
- Grafana/Loki 대시보드가 현재 로그 포맷과 맞는지 점검한다.
- Google Sheets 마이그레이션 검증 리포트 또는 운영 절차를 만든다.
- 전환 완료 후 Google Sheets 의존성 제거 시점을 결정한다.
- `./gradlew test`를 릴리즈 필수 검증으로 둔다.
- 아키텍처 경계 테스트를 추가한다.
  - Domain은 Spring/JPA/Web 의존성이 없어야 한다.
  - Application은 adapter 구현체에 의존하지 않아야 한다.
  - Adapter는 port 구현체로만 application과 연결되어야 한다.

### 완료 기준

- 릴리즈 전 체크리스트가 모두 통과한다.
- 운영 로그에서 주요 관리자 행위와 실패 원인을 추적할 수 있다.
- 민감 값 원문이 로그에 남지 않는다.
- Grafana/Actuator로 운영 상태를 확인할 수 있다.
- 아키텍처 경계 위반이 테스트에서 잡힌다.

## 13. 추천 구현 순서

1. Phase 0 문서/현황 정리
2. Phase 1 OAuth state와 토큰 로그 제거
3. Phase 2 호감도 원장 확장
4. Phase 3 본인/관리자 히스토리 조회 정리
5. Phase 4 업보 템플릿과 수동 적용
6. Phase 5 출석체크 사이클과 채팅 명령 쿨타임
7. Phase 6 룰렛 테이블, 후원 이벤트, 결과 반영
8. Phase 7 OBS 오버레이와 재송출
9. Phase 8 운영 로그, 모니터링, 릴리즈 검증

## 14. 초기 작업 단위

가장 먼저 구현할 수 있는 작은 단위는 다음 순서가 적합하다.

| 순서 | 작업 | 이유 |
| --- | --- | --- |
| 1 | 신규 패키지 골격 추가 | 이후 기능을 클린 아키텍처 기준으로 배치하기 위한 기준점 |
| 2 | OAuth `state` 난수화 | 보안 리스크가 명확하고 변경 범위가 작음 |
| 3 | 토큰 DTO 로그 제거 | 민감 정보 노출 위험을 즉시 낮춤 |
| 4 | 호감도 domain model과 use case port 정의 | 이후 업보/룰렛/출석 공통 기반 |
| 5 | 호감도 persistence adapter와 원장 필드 확장 | 기존 DB 기능과 신규 use case 연결 |
| 6 | 관리자 정정 거래 use case | 삭제 없는 보정 원칙을 구현 흐름에 고정 |

## 15. 보류 정책

다음 항목은 PRD상 미정이므로 기본 동작만 제공하고, 운영 데이터 확보 후 확정한다.

| 항목 | 기본 동작 |
| --- | --- |
| 등업 단계명/혜택 | 기능 비활성, 마이페이지 표시 없음 |
| 주간 캐쉬백 | 자동 지급 없음, 필요 시 관리자 수동 지급 |
| 룰렛 결과 통계 자동 편향 알람 | 비활성, 활성화 전 1만 회 시뮬레이션만 제공 |
| 외부 인증 업보 업로드 | 별도 업로드 페이지 없음, 관리자 수동 확인 |
| 사용자 탈퇴/개인정보 처리 | 자동 탈퇴 처리 없음, 데이터 보존 |
