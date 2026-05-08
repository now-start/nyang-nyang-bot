# 클린 아키텍처 채택 결정

- 상태: Accepted
- 작성일: 2026-05-08
- 관련 문서:
  - [Nyang-Nyang Bot Spec](index.md)
  - [클린 아키텍처](architecture.md)
  - [PRD 구현 계획](implementation-plan.md)

## Context

Nyang-Nyang Bot은 CHZZK OAuth, 채팅 소켓, 후원 이벤트, Google Sheets 마이그레이션, Spring MVC 화면, JPA 저장소, OBS 오버레이를 함께 다룬다.

초기 구현은 `controller`, `service`, `repository`, `data/entity`, `data/dto` 중심의 Spring MVC 구조에 가깝다. 이 구조는 빠르게 기능을 만들기에는 단순하지만, PRD의 다음 요구사항을 구현할수록 비즈니스 규칙과 외부 기술 세부사항이 섞일 위험이 크다.

- 호감도 원장과 잔액 정합성
- 업보/쿠폰/리워드 상태 전이
- 관리자 정정 거래
- 후원 룰렛의 idempotency와 다회차 반영
- OBS 오버레이 재송출
- CHZZK/Google Sheets 같은 외부 API 변경 대응

## Decision

신규 P0/P1 기능과 변경 범위에 닿는 기존 기능은 클린 아키텍처 기준으로 구현한다.

레이어는 다음 기준을 따른다.

- Domain: 비즈니스 규칙과 상태 전이
- Application: 유스케이스, 트랜잭션 경계, 포트 호출
- Ports: application이 필요로 하는 입출력 계약
- Adapters: web, persistence, external API, chat, overlay 구현
- Configuration: DI, 보안, 트랜잭션, 운영 설정

의존성 방향은 `Adapters -> Application -> Domain`으로 제한한다.

기존 코드는 전면 재작성하지 않는다. 호감도 원장부터 새 구조로 전환하고, 출석/업보/룰렛/오버레이 기능이 해당 use case를 호출하도록 점진적으로 이동한다.

## Consequences

긍정적 효과:

- 호감도 원장과 정정 정책을 프레임워크와 분리해 테스트할 수 있다.
- CHZZK, Google Sheets, OBS 오버레이 변경이 domain/application에 직접 전파되지 않는다.
- 같은 Favorite use case를 출석, 업보, 룰렛, 관리자 조정에서 재사용할 수 있다.
- 아키텍처 테스트로 역방향 의존성 회귀를 막을 수 있다.

비용:

- 초기 파일 수와 인터페이스 수가 늘어난다.
- 기존 `service/repository/entity/dto` 구조와 새 구조가 전환 기간 동안 공존한다.
- 단순 CRUD에도 command/result/port 변환이 필요하다.
- 팀이 레이어 경계와 네이밍 규칙을 지켜야 한다.

## Alternatives Considered

### 기존 Spring MVC 계층 구조 유지

장점:

- 변경량이 적다.
- 현재 코드와 테스트를 최대한 유지할 수 있다.

단점:

- 룰렛/업보/원장 정책이 service와 entity에 흩어질 가능성이 높다.
- 외부 API DTO, JPA entity, web DTO가 섞이기 쉽다.
- 핵심 규칙 단위 테스트가 어려워진다.

### Hexagonal Architecture 명칭 사용

장점:

- 포트/어댑터 중심의 설명이 명확하다.
- 외부 시스템이 많은 현재 서비스와 잘 맞는다.

단점:

- 팀 내 용어를 `클린 아키텍처`로 이미 정했으므로 문서 용어가 분산될 수 있다.

결론:

- 구현 방식은 포트/어댑터를 적극 사용하되, 문서와 의사결정 명칭은 `클린 아키텍처`로 통일한다.

## Follow-up

- [클린 아키텍처](architecture.md)의 패키지 구조를 기준으로 신규 패키지를 만든다.
- Phase 0 또는 Phase 1에서 ArchUnit 도입 여부를 결정한다.
- 호감도 원장 use case를 첫 전환 대상으로 삼는다.
- 기존 `data.entity`와 `repository`는 migration 대상이므로 전환기 예외로 관리한다.
