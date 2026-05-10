# 클린 아키텍처

- 문서 상태: MVP PRD v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 관련 문서:
  - [호감도 포인트 제도](point-system.md)
  - [호감도/업보/쿠폰 카탈로그](reward-catalog.md)
  - [치지직 후원 룰렛과 OBS 오버레이](roulette-overlay.md)
  - [기술/운영 부록](technical-appendix.md)
  - [요구사항 추적표](requirements-traceability.md)
  - [데이터 모델/마이그레이션 계획](data-model-migration.md)
  - [API 명세](api.md)
  - [이벤트 명세](events.md)
  - [테스트 전략](test-strategy.md)

## 1. 범위

Nyang-Nyang Bot은 클린 아키텍처를 기준 구조로 사용한다. 이 문서는 PRD의 기능 요구사항을 구현할 때 따라야 하는 레이어, 의존성 방향, 패키지 구조, 포트/어댑터 경계, 테스트 기준을 정의한다.

현재 코드베이스의 web controller는 `adapter/in/web` inbound adapter로 이동했다. 남아 있는 `service`, `repository`, `data/entity`, `data/dto` 중심의 Spring MVC 구조는 신규 P0/P1 기능과 수정 범위에 닿는 기존 기능부터 점진적으로 클린 아키텍처로 이동한다.

## 2. 목표

- 호감도 원장, 업보, 출석, 룰렛, 오버레이 정책을 프레임워크와 분리된 도메인 규칙으로 유지한다.
- Spring MVC, JPA, Feign, WebSocket, Thymeleaf, CHZZK API, Google Sheets API 변경이 도메인 규칙에 전파되지 않게 한다.
- 유스케이스 단위로 트랜잭션, 권한, 중복 방지, 정정 정책을 명확히 한다.
- 테스트가 어려운 컨트롤러/DB 중심 구조를 피하고, 도메인과 유스케이스를 빠르게 단위 테스트할 수 있게 한다.
- OBS 오버레이, 채팅 소켓, 후원 이벤트처럼 입출력 방식이 다른 기능을 같은 애플리케이션 규칙 위에 올린다.

## 3. 레이어

| 레이어 | 책임 | 포함 예시 |
| --- | --- | --- |
| Domain | 핵심 비즈니스 규칙, 상태 전이, 정책 검증 | 호감도 잔액, 원장 거래, 업보 상태, 룰렛 확률표 |
| Application | 유스케이스 orchestration, 트랜잭션 경계, 권한 의도, 포트 호출 | 호감도 조정, 업보 적용, 출석 보상 적용, 룰렛 실행 |
| Ports | Application이 외부에 요구하는 입출력 계약 | 저장소 포트, CHZZK 포트, 오버레이 이벤트 포트 |
| Adapters | 웹, DB, 외부 API, 채팅, 오버레이 등 입출력 구현 | Controller, JPA Repository, Feign Client, WebSocket Client |
| Configuration | DI, 보안, 트랜잭션, OpenAPI, 운영 설정 | Spring Security, Feign 설정, Swagger 설정 |

## 4. 의존성 규칙

- 의존성 방향은 `Adapters -> Application -> Domain`으로만 흐른다.
- Domain은 Spring, JPA, Jackson, Servlet, Feign, WebSocket API에 의존하지 않는다.
- Domain 객체는 DB 테이블 구조나 API 응답 형식을 알지 않는다.
- Application은 JPA repository 구현체가 아니라 outbound port 인터페이스를 호출한다.
- Web adapter는 request/response DTO와 application command/result 간 변환만 담당한다.
- Persistence adapter는 JPA entity와 domain model 간 변환을 담당한다.
- External adapter는 CHZZK, Google Sheets 같은 외부 API DTO를 내부 command/result로 변환한다.
- Configuration은 구현체를 조립하지만 비즈니스 규칙을 포함하지 않는다.

금지 규칙:

- Controller에서 잔액을 직접 계산하거나 JPA entity를 직접 수정하지 않는다.
- Domain에서 `@Entity`, `@Service`, `@Transactional`, `@Controller` 같은 Spring/JPA annotation을 사용하지 않는다.
- Application service에서 Feign client나 Spring Data repository를 직접 주입하지 않는다.
- JPA entity를 web response로 직접 반환하지 않는다.
- 외부 API DTO를 domain 객체로 직접 사용하지 않는다.

## 5. 권장 패키지 구조

```text
org.nowstart.nyangnyangbot
  domain
    auth
    favorite
    upbo
    attendance
    chat
    roulette
    overlay
  application
    auth
    favorite
    upbo
    attendance
    chat
    roulette
    overlay
    port
      in
      out
  adapter
    in
      web
      chat
      overlay
      scheduler
    out
      persistence
      chzzk
      google
      monitoring
  config
```

패키지 전환 원칙:

- 신규 기능은 위 구조에 맞춰 작성한다.
- 기존 기능은 수정 범위에 닿을 때 application/use case와 adapter를 분리한다.
- 현행 `service`는 application service 후보로 본다.
- 현행 `repository`는 persistence adapter 또는 external adapter 후보로 본다.
- 현행 `data/entity`는 JPA persistence model로 분리하고, domain model과 직접 공유하지 않는 방향으로 전환한다.
- 현행 `data/dto`는 web DTO, external DTO, application result로 분리한다.

## 6. 네이밍 규칙

| 종류 | 규칙 | 예시 |
| --- | --- | --- |
| Domain model | 비즈니스 이름 사용, `Entity` 접미사 지양 | `FavoriteAccount`, `FavoriteLedgerEntry`, `RouletteTable` |
| Use case | 사용자/시스템 행위 + `UseCase` | `AdjustFavoriteUseCase`, `ApplyUpboUseCase` |
| Application service | Use case 구현체 + `Service` | `FavoriteLedgerService`, `RouletteExecutionService` |
| Inbound port | 외부에서 호출 가능한 use case 계약 | `AdjustFavoriteUseCase` |
| Outbound port | 외부 저장소/시스템 호출 계약 | `LoadFavoriteAccountPort`, `SaveFavoriteLedgerPort` |
| JPA model | JPA 전용 모델임을 명시 | `FavoriteJpaEntity`, `FavoriteLedgerJpaEntity` |
| Adapter | 구현 기술 또는 방향 명시 | `FavoritePersistenceAdapter`, `ChzzkDonationAdapter` |
| Web DTO | 요청/응답 목적 명시 | `FavoriteHistoryResponse`, `ApplyUpboRequest` |

기존 클래스와의 충돌을 줄이기 위해 전환 기간에는 기존 `FavoriteEntity`, `FavoriteHistoryEntity` 이름을 유지할 수 있다. 다만 새 domain model에는 `Entity` 접미사를 붙이지 않는다.

## 7. 도메인 경계

### 7.1 Auth

책임:

- CHZZK 사용자의 식별자와 권한을 application에 전달한다.
- 관리자 여부는 현재 `AuthorizationEntity.admin = true` 기준을 유지한다.
- OAuth 토큰 발급/갱신 세부사항은 CHZZK outbound adapter에 격리한다.

주요 규칙:

- OAuth `state` 검증은 web/application 경계에서 처리한다.
- access token, refresh token, OAuth state는 로그에 남기지 않는다.
- domain은 CHZZK OAuth 응답 DTO를 알지 않는다.

### 7.2 Favorite

책임:

- 호감도 잔액과 원장 거래의 핵심 규칙을 담당한다.
- 출석, 업보, 룰렛, 관리자 조정은 모두 Favorite use case를 통해 잔액을 변경한다.

주요 규칙:

- 모든 잔액 변경은 원장 거래와 같은 트랜잭션에서 처리한다.
- 사용자별 비관적 락으로 잔액 변경을 직렬화한다.
- 음수 잔액 생성은 관리자 또는 시스템 정책 경로에서만 허용한다.
- 정정은 삭제가 아니라 별도 보정 거래로 기록한다.
- `idempotencyKey`가 있는 거래는 중복 반영하지 않는다.

### 7.3 Upbo

책임:

- 업보/쿠폰/리워드 템플릿과 사용자 보유 항목을 관리한다.
- 호감도로 자동 전환되는 항목과 보유 목록에만 남는 항목을 구분한다.

주요 규칙:

- `FAVORITE` 자동 전환 항목은 Favorite use case를 호출해 원장에 반영한다.
- `NONE` 전환 항목은 호감도 잔액을 변경하지 않는다.
- 상태는 `OWNED`, `USED`, `CONVERTED`, `CORRECTED`를 사용한다.
- 사용 처리와 정정 처리는 사용자 히스토리에 공개한다.
- 관리자 내부 메모는 일반 사용자에게 노출하지 않는다.

### 7.4 Attendance

책임:

- 출석체크 활성화 사이클과 대상자 수집을 관리한다.
- 적용 시 Favorite use case로 일괄 지급을 요청한다.

주요 규칙:

- 한 사이클 내 동일 사용자에게 1회만 지급한다.
- 관리자가 입력한 양수 정수 `N`만 지급 점수로 허용한다.
- 취소된 사이클은 호감도를 지급하지 않는다.
- 일반 채팅은 호감도 지급 없이 주간 채팅 랭킹에만 반영한다.

### 7.5 Chat

책임:

- CHZZK 채팅 이벤트 수신, 명령어 파싱, 응답 전송을 담당한다.
- 채팅 adapter는 application use case를 호출하고, 잔액이나 업보 상태를 직접 변경하지 않는다.

주요 규칙:

- `!호감도`, `!룰렛결과` 같은 사용자 명령은 사용자별 쿨타임을 적용한다.
- 기본 쿨타임은 30초이다.
- 본인 한정 명령 응답에는 다른 사용자의 상세 히스토리를 포함하지 않는다.

### 7.6 Roulette

책임:

- 룰렛 테이블, 확률표, 후원 이벤트, 회차별 결과 확정과 반영을 담당한다.
- 결과 선택 알고리즘은 domain service로 분리해 테스트 가능해야 한다.

주요 규칙:

- 활성화 가능한 룰렛 확률 합계는 100%여야 한다.
- `꽝` 항목은 필수이며 0%로 설정할 수 없다.
- 후원 메시지 명령어는 공백/줄바꿈 토큰 단위 정확 일치로 매칭한다.
- 회차 수는 `floor(후원 금액 / 룰렛 1회 금액)`으로 계산한다.
- CHZZK 후원 이벤트 ID를 idempotency key로 사용한다.
- 후원 시점의 룰렛 테이블 버전과 항목 스냅샷을 저장한다.
- N회차 결과는 일괄 확정하고, 회차별 원장/보유 목록 반영은 별도 트랜잭션으로 처리한다.

### 7.7 Overlay

책임:

- OBS 오버레이 토큰, 표시 이벤트, 표시 완료, missed/replay 상태를 관리한다.
- 오버레이는 서버 확정 결과를 표시만 하고 결과를 결정하지 않는다.

주요 규칙:

- 오버레이 토큰 원문은 발급/재발급 직후 한 번만 보여준다.
- DB에는 토큰 해시만 저장한다.
- OBS URL은 `/overlay/roulette#token=...` 형태를 사용한다.
- 이벤트 조회와 표시 완료 API는 `Authorization: Bearer` 헤더로 보호한다.
- 재송출은 원장과 보유 목록을 다시 반영하지 않는다.

## 8. 주요 포트

### 8.1 Inbound Port

| Port | 호출 주체 | 설명 |
| --- | --- | --- |
| `LoginWithChzzkUseCase` | Web adapter | CHZZK OAuth 콜백 처리 |
| `AdjustFavoriteUseCase` | Web/admin adapter | 관리자 호감도 지급/차감 |
| `CorrectFavoriteLedgerUseCase` | Web/admin adapter | 원장 정정 거래 생성 |
| `ViewFavoriteHistoryUseCase` | Web adapter, Chat adapter | 본인 또는 관리자 히스토리 조회 |
| `ApplyAttendanceRewardUseCase` | Web/admin adapter | 출석 사이클 보상 적용 |
| `ApplyUpboUseCase` | Web/admin adapter | 업보/쿠폰/리워드 수동 적용 |
| `UseRewardUseCase` | Web/admin adapter | 보유 리워드 사용 처리 |
| `RunRouletteFromDonationUseCase` | CHZZK adapter | 후원 이벤트 기반 룰렛 실행 |
| `PollOverlayEventUseCase` | Overlay web adapter | OBS 오버레이 표시 이벤트 조회 |
| `MarkOverlayEventDisplayedUseCase` | Overlay web adapter | 표시 완료 보고 |

### 8.2 Outbound Port

| Port | 구현 adapter | 설명 |
| --- | --- | --- |
| `LoadFavoriteAccountPort` | Persistence | 호감도 계정 조회와 락 획득 |
| `SaveFavoriteLedgerPort` | Persistence | 원장 거래 저장 |
| `CheckIdempotencyPort` | Persistence | 중복 반영 방지 |
| `LoadUpboTemplatePort` | Persistence | 업보 템플릿 조회 |
| `SaveUserUpboPort` | Persistence | 사용자 보유 업보 저장 |
| `LoadRouletteTablePort` | Persistence | 활성 룰렛 테이블 조회 |
| `SaveRouletteResultPort` | Persistence | 룰렛 이벤트/회차 결과 저장 |
| `PublishOverlayEventPort` | Persistence or Messaging | 오버레이 표시 이벤트 생성 |
| `ChzzkSessionPort` | CHZZK external adapter | CHZZK 세션과 이벤트 구독 |
| `SendChatMessagePort` | CHZZK external adapter | 채팅 명령 응답 전송 |
| `GoogleSheetMigrationPort` | Google external adapter | 전환기 Google Sheets 데이터 조회 |

## 9. 트랜잭션과 동시성

- 트랜잭션 경계는 application use case에서 선언한다.
- Domain은 트랜잭션 기술을 알지 않는다.
- 호감도 잔액 갱신은 사용자 단위 비관적 락으로 직렬화한다.
- 원장 저장과 잔액 변경은 같은 트랜잭션에서 처리한다.
- 다회차 룰렛은 결과 일괄 확정 트랜잭션과 회차별 반영 트랜잭션을 분리한다.
- `idempotencyKey`는 DB UNIQUE 제약과 application 검증을 함께 사용한다.
- 정정 거래는 원본 거래와 연결하되 원본 거래를 변경하거나 삭제하지 않는다.

## 10. Web Adapter 기준

- Controller는 인증 주체, 요청 DTO, path/query/body 값을 application command로 변환한다.
- Controller는 domain 객체를 직접 반환하지 않는다.
- 관리자 권한이 필요한 요청은 web security와 application use case 양쪽에서 의도를 명확히 한다.
- 일반 사용자용 조회 요청은 actor의 CHZZK `channelId`를 기준으로 본인 데이터만 조회한다.
- 관리자 검색은 별도 use case로 분리한다.

## 11. Persistence Adapter 기준

- JPA entity는 DB schema와 ORM 매핑을 위한 모델이다.
- Domain model은 JPA entity와 분리한다.
- Persistence adapter는 JPA repository를 호출하고 domain model로 변환한다.
- 비관적 락, UNIQUE 제약, 조회 최적화는 persistence adapter 책임이다.
- migration 과정에서 기존 entity를 재사용할 수 있지만, 신규 domain 규칙을 JPA entity method에 넣지 않는다.

## 12. External Adapter 기준

CHZZK:

- OAuth, 사용자 조회, 세션 발급, 채팅/후원/구독 이벤트 구독, 채팅 전송을 담당한다.
- CHZZK DTO는 adapter 내부에서 application command/result로 변환한다.
- 토큰 원문은 로그에 남기지 않는다.

Google Sheets:

- 전환기 마이그레이션 데이터 조회만 담당한다.
- 플랫폼 DB가 최종 원본이 된 뒤에는 제거 대상이다.

Monitoring:

- 운영 로그와 Grafana/Loki는 adapter/config 영역에서 다룬다.
- logfmt 키는 application 결과와 actor 정보를 바탕으로 출력하되, 민감 값은 포함하지 않는다.

## 13. 테스트 기준

| 테스트 종류 | 대상 | 목적 |
| --- | --- | --- |
| Domain unit test | Domain model, domain service | 포인트 계산, 상태 전이, 룰렛 확률 검증 |
| Application use case test | Use case service, mock port | 트랜잭션 흐름, 권한 의도, 중복 방지, 정정 정책 |
| Adapter test | Controller, persistence adapter, external adapter | DTO 변환, 권한, JPA query, 외부 API 매핑 |
| Integration test | Spring context + DB | 실제 wiring, 트랜잭션, 보안 설정 |
| Architecture test | 패키지 의존성 | Domain/Application이 adapter/framework에 의존하지 않는지 검증 |

아키텍처 테스트 규칙:

- `domain..` 패키지는 `org.springframework..`, `jakarta.persistence..`, `javax.persistence..`, `feign..`, `jakarta.servlet..`에 의존하지 않는다.
- `application..` 패키지는 `adapter..` 패키지에 의존하지 않는다.
- `adapter..` 패키지만 Spring MVC, JPA, Feign, WebSocket 구현체에 의존한다.
- web adapter 테스트는 비즈니스 계산보다 요청 검증, 권한, response mapping에 집중한다.

## 14. 점진적 전환 계획

1. 신규 패키지 골격을 추가한다.
2. 호감도 원장부터 domain/application/port/adapter 구조로 분리한다.
3. 기존 `FavoriteService`의 잔액 변경 로직을 application use case로 이동한다.
4. 출석, 업보, 룰렛은 Favorite use case를 호출해 잔액을 변경한다.
5. 채팅/후원/오버레이 입출력은 adapter로 격리한다.
6. 기존 `repository`, `data/dto`는 수정 범위에 닿는 시점마다 새 구조로 이동한다.
7. 아키텍처 경계 테스트를 추가해 역방향 의존성 회귀를 막는다.

## 15. 결정 상태

- 기준 구조는 클린 아키텍처이다.
- 신규 P0/P1 기능은 이 문서의 레이어와 의존성 규칙을 따른다.
- 기존 코드는 전면 재작성하지 않고 기능 변경 범위 내에서 점진적으로 전환하되, web controller는 `adapter/in/web` inbound adapter로 유지한다.
- 도메인 규칙은 프레임워크와 분리하고, 외부 시스템은 포트/어댑터로 격리한다.
