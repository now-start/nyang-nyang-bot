# Nyang-Nyang Bot Implementation Plan

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 기준 PRD: [Nyang-Nyang Bot PRD](PRD.md)
- 기준 명세: [Nyang-Nyang Bot Spec](spec/index.md)
- 상세 계획: [Spec Implementation Plan](spec/implementation-plan.md)
- 목적: Spec/PRD를 실제 구현 순서와 검증 게이트로 압축한다.

## 1. 실행 원칙

- 호감도 원장을 먼저 안정화한 뒤 출석, 업보, 룰렛, 오버레이를 같은 거래 모델 위에 올린다.
- 기준 구조는 [클린 아키텍처](spec/architecture.md)이다.
- 신규 기능은 `domain`, `application`, `port`, `adapter` 경계를 지킨다.
- 기존 `controller/service/repository/data` 구조는 전면 재작성하지 않고 수정 범위에 닿는 부분부터 점진 전환한다.
- 잘못된 거래는 삭제하지 않고 보정 거래로 정정한다.
- 모든 Phase는 테스트와 문서 갱신을 완료 기준에 포함한다.

## 2. 마일스톤 요약

| Phase | 범위 | 목적 | 핵심 산출물 |
| --- | --- | --- | --- |
| Phase 0 | 문서/현황 정리 | 구현 기준 확정 | 요구사항 상태표, 현행 패키지 분류, 테스트 기준 |
| Phase 1 | 보안 안정화 | OAuth/권한/로그 위험 제거 | state 검증, 토큰 로그 제거, 관리자 API 보호 |
| Phase 2 | 호감도 원장 | 공통 거래 기반 확보 | Favorite domain/use case/port, 원장 확장 |
| Phase 3 | 조회/마이페이지 | 사용자/관리자 조회 정리 | 히스토리, 업보 조회, 미확인 배지, 마이페이지 UI |
| Phase 4 | 업보/쿠폰/리워드 | 관리자 수동 운영 도구 | Upbo 모델, 템플릿, 적용/사용/정정, 관리자 UI |
| Phase 5 | 출석/채팅 명령 | 방송 중 운영 흐름 안정화 | 출석 사이클, `!호감도`, `!룰렛결과`, 쿨타임 |
| Phase 6 | 후원 룰렛 | 서버 확정 룰렛 구현 | 룰렛 테이블, 후원 idempotency, 회차 반영, 룰렛 관리 UI |
| Phase 7 | OBS 오버레이 | 방송 화면 표시와 재송출 | 토큰, 표시 이벤트, missed/replay, overlay UI |
| Phase 8 | 운영/릴리즈 | 운영 가능 상태 검증 | logfmt, runbook, Grafana, 릴리즈 체크 |

## 3. Phase 0. 문서/현황 정리

작업:

- [요구사항 추적표](spec/requirements-traceability.md)의 상태를 실제 코드 기준으로 재검토한다.
- 현재 패키지를 클린 아키텍처 레이어 기준으로 분류한다.
- `docs/spec/index.md`의 의도 불명 Skilljar URL 처리 방침을 정한다.
- `docs/granafa_dashboard.json` 파일명 오타 교정 여부를 결정한다.
- `00체/영어 금지 2분`과 `3분` 정책 차이를 확정한다.
- 기준 테스트 명령과 현재 결과를 기록한다.

완료 기준:

- P0/P1 요구사항이 `현행 구현`, `부분 구현`, `미구현`, `보류`로 분류된다.
- 첫 구현 PR에서 따라야 할 패키지 구조와 예외가 명확하다.
- 문서상 불명확한 링크/정책 충돌이 이슈 또는 문서 TODO로 남아 있다.

## 4. Phase 1. MVP 보안 안정화

작업:

- OAuth `state`를 요청별 난수로 생성하고 콜백에서 검증한다.
- CHZZK access token, refresh token, OAuth state 로그를 제거 또는 마스킹한다.
- 관리자 API와 관리자 화면 진입점에 `ADMIN` 권한 검사를 일관 적용한다.
- CSRF 정책과 `/actuator/**`, `/v3/api-docs` 공개 범위를 점검한다.
- OAuth/CHZZK 연동을 outbound adapter로 격리할 전환 경계를 잡는다.

완료 기준:

- 잘못된 OAuth `state` 콜백은 실패한다.
- 토큰 원문이 로그에 남지 않는다.
- 일반 사용자는 관리자 API 호출 시 거부된다.
- 보안 회귀 테스트가 추가된다.

검증:

- OAuth state 성공/실패 테스트
- 관리자 API 403 테스트
- 토큰 DTO 로그 미노출 확인

## 5. Phase 2. 호감도 원장 확장

작업:

- `FavoriteAccount`, `FavoriteLedgerEntry`, `FavoriteSourceType` domain model을 만든다.
- `AdjustFavoriteUseCase`, `CorrectFavoriteLedgerUseCase`, `GrantFavoriteUseCase`를 정의한다.
- `LoadFavoriteAccountPort`, `SaveFavoriteLedgerPort`, `CheckIdempotencyPort`를 정의한다.
- 현행 `FavoriteEntity`, `FavoriteHistoryEntity`를 확장하거나 persistence adapter 모델로 분리한다.
- 원장 필드 `delta`, `balanceAfter`, `sourceType`, `actorId`, `idempotencyKey`, `nickNameSnapshot` 등을 추가한다.
- 잔액 변경 코드를 단일 application service로 집중한다.
- 사용자별 비관적 락으로 잔액 변경을 직렬화한다.

완료 기준:

- 모든 호감도 변경은 원장과 잔액이 같은 트랜잭션에서 처리된다.
- 동일 `idempotencyKey`는 중복 반영되지 않는다.
- 정정은 원본 삭제 없이 별도 원장 거래로 남는다.
- adapter가 domain 규칙을 우회해 잔액을 직접 수정하지 않는다.

검증:

- domain unit test
- application use case fake port test
- JPA transaction rollback test
- 잔액/원장 일치 검증

## 6. Phase 3. 사용자 조회와 마이페이지

작업:

- 본인 호감도와 전체 히스토리 조회 use case를 정리한다.
- 관리자 전체 히스토리 조회와 닉네임 부분 일치 검색을 정리한다.
- `lastSeenAt` 기반 미확인 배지를 추가한다.
- 본인 업보 내역, 누적 업보 반영 합계, 반영 전/후 호감도 표시 구조를 만든다.
- 히스토리 최대 50건 제한을 적용한다.
- [디자인 와이어프레임](spec/wireframes.md)의 호감도 보드와 마이페이지 구조를 반영한다.
- [웹 UI 명세](spec/web-ui.md)의 음수 잔액, 미확인 배지, 히스토리 로딩/빈 상태를 구현한다.

완료 기준:

- 일반 사용자는 다른 사용자의 상세 히스토리를 볼 수 없다.
- 관리자는 닉네임 부분 일치 검색을 수행할 수 있다.
- 마이페이지 진입 시 미확인 배지가 해소된다.
- 음수 잔액은 랭킹과 본인 화면에서 그대로 표시된다.
- 데스크톱/모바일에서 호감도 보드와 마이페이지 주요 텍스트가 겹치지 않는다.

## 7. Phase 4. 업보/쿠폰/리워드 관리자 기능

작업:

- `UpboTemplate`, `UserUpbo`, `UpboStatus`, `RewardType`, `ConversionMode`를 정의한다.
- 업보 템플릿 CRUD를 구현한다.
- 관리자 업보 수동 적용, 사용 처리, 쿠폰 구매 처리, 구매 즉시 사용 처리를 구현한다.
- `AUTO` 전환 항목은 Favorite use case를 호출해 원장에 반영한다.
- `NONE` 전환 항목은 보유 목록에만 저장한다.
- 업보/쿠폰/리워드 정정 거래를 구현한다.
- 관리자 처리 화면은 대상 검색, 처리 유형, 템플릿/자유 입력, 공개 설명, 관리자 메모, 적용 전/후 미리보기를 포함한다.

완료 기준:

- 관리자는 템플릿 또는 자유 입력으로 업보를 적용할 수 있다.
- 자동 전환 항목은 원장과 보유 상태가 함께 갱신된다.
- 관리자 내부 메모는 일반 사용자에게 노출되지 않는다.
- 사용 처리와 정정 처리는 사용자 히스토리에 공개된다.
- 관리자 입력 검증 오류가 inline으로 표시되고 적용 전/후 호감도가 명확히 보인다.

## 8. Phase 5. 출석체크와 채팅 명령

작업:

- 출석체크를 활성화 사이클 단위로 정리한다.
- 한 사이클 내 동일 사용자 1회 지급을 보장한다.
- `!호감도` 응답을 확장된 원장/잔액 모델에 맞춘다.
- `!룰렛결과` 본인 한정 응답을 추가한다.
- 사용자별 명령어 쿨타임 기본 30초를 적용한다.

완료 기준:

- 같은 출석 사이클에서 중복 지급되지 않는다.
- 취소된 사이클은 지급하지 않는다.
- 일반 채팅은 호감도를 지급하지 않고 주간 채팅 랭킹에만 반영된다.
- 쿨타임 내 동일 명령 재호출은 응답하지 않는다.

## 9. Phase 6. 후원 룰렛

작업:

- `RouletteTable`, `RouletteItem`, `RouletteEvent`, `RouletteRoundResult` 모델을 만든다.
- 룰렛 활성화 검증을 구현한다.
  - 확률 합계 100%
  - `꽝` 필수
  - `꽝` 0% 금지
  - 명령어와 1회 금액 필수
- CHZZK 후원 이벤트 구독과 저장 흐름을 정리한다.
- 후원 메시지 명령어를 토큰 단위 정확 일치로 매칭한다.
- 후원 이벤트 ID 기반 idempotency를 적용한다.
- 후원 시점 룰렛 테이블 스냅샷을 저장한다.
- 다회차 결과 일괄 확정과 회차별 반영을 분리한다.
- 룰렛 결과 정정 기능을 구현한다.
- 룰렛 관리 UI는 확률 합계, 꽝 필수, 항목 수 가독성 경고, 1만 회 시뮬레이션을 표시한다.

완료 기준:

- 명령어가 없는 후원은 룰렛을 실행하지 않는다.
- 같은 후원 이벤트 ID는 중복 반영되지 않는다.
- 일부 회차 반영 실패 시 실패 회차만 재처리할 수 있다.
- 룰렛 결과는 자동 채팅 공지 없이 원장과 사용자 히스토리에 반영된다.
- 확률표가 활성화 불가 상태일 때 UI가 이유를 즉시 보여준다.

## 10. Phase 7. OBS 오버레이

작업:

- 오버레이 토큰 발급/재발급/폐기를 구현한다.
- 토큰 원문은 발급/재발급 직후 한 번만 보여주고 DB에는 해시만 저장한다.
- `/overlay/roulette#token=...` 페이지를 구현한다.
- 이벤트 조회/표시 완료 API는 `Authorization: Bearer` 헤더로 보호한다.
- 오버레이 이벤트 120초 유효시간과 `MISSED` 상태를 구현한다.
- 관리자 재송출 기능을 구현한다.
- [OBS 오버레이 디자인 명세](spec/overlay-design.md)의 `IDLE`, `DISPLAYING`, `SUMMARY`, `ERROR` 상태를 구현한다.
- [디자인 와이어프레임](spec/wireframes.md)의 idle/displaying/summary 구조를 기준으로 화면을 만든다.

완료 기준:

- 오버레이 토큰은 query string이나 서버 access log에 남지 않는다.
- 유효하지 않은 토큰은 이벤트 조회가 거부된다.
- 오버레이는 서버 확정 결과만 표시한다.
- 재송출은 원장과 보유 목록을 다시 반영하지 않는다.
- 이벤트 없음 상태는 완전 투명이다.
- 1920x1080과 1280x720에서 결과 라벨이 안전 영역 안에 표시된다.

## 11. Phase 8. 운영과 릴리즈

작업:

- logfmt 표준 키를 적용한다.
- 민감 값 로그 노출 여부를 릴리즈 체크리스트에 포함한다.
- Grafana/Loki 대시보드와 현재 로그 포맷을 맞춘다.
- Google Sheets 마이그레이션 검증 리포트 또는 운영 절차를 만든다.
- 아키텍처 경계 테스트를 추가한다.
- [운영/배포 Runbook](spec/runbook.md)을 배포 절차와 맞춘다.

완료 기준:

- `./gradlew test`가 통과한다.
- 운영 로그에서 관리자 행위와 실패 원인을 추적할 수 있다.
- 민감 값 원문이 로그에 남지 않는다.
- 아키텍처 경계 위반이 테스트에서 잡힌다.

## 12. 바로 시작할 작업

| 순서 | 작업 | 결과물 |
| --- | --- | --- |
| 1 | Phase 0 상태 정리 | 요구사항 추적표 상태 보정 |
| 2 | 클린 아키텍처 패키지 골격 추가 | `domain/application/adapter` 기준 디렉터리 |
| 3 | OAuth state 난수화 | 보안 테스트 포함 |
| 4 | 토큰 로그 제거 | 민감 값 로그 검증 |
| 5 | Favorite domain/use case/port 정의 | 원장 확장 기반 |
| 6 | Favorite persistence adapter 연결 | 기존 JPA 모델과 호환 |
| 7 | 관리자 정정 거래 use case 구현 | 삭제 없는 보정 원칙 고정 |

## 13. 리스크와 대응

| 리스크 | 대응 |
| --- | --- |
| 기존 Spring MVC 구조와 클린 아키텍처 구조가 섞임 | 신규 기능부터 새 구조, 기존 기능은 수정 범위별 점진 이동 |
| 원장 migration 중 잔액 불일치 | 백필 검증 리포트와 관리자 보정 거래 |
| 후원 이벤트 중복 반영 | CHZZK 후원 이벤트 ID UNIQUE와 application idempotency |
| 오버레이 토큰 유출 | 토큰 해시 저장, 원문 1회 표시, 즉시 rotate/revoke |
| 룰렛 다회차 일부 실패 | 결과 확정과 회차별 반영 분리, `CONFIRMED` 재처리 |
| 운영 로그 카디널리티 폭발 | Loki 라벨은 `level`, `action`, `result` 중심 |

## 14. 검증 게이트

각 Phase 완료 전 다음을 확인한다.

- 관련 FR ID가 [요구사항 추적표](spec/requirements-traceability.md)에 반영되어 있다.
- 관련 API 변경은 [API 명세](spec/api.md)에 반영되어 있다.
- 이벤트 상태나 idempotency 변경은 [이벤트 명세](spec/events.md)에 반영되어 있다.
- 화면/오버레이 변경은 [웹 UI 명세](spec/web-ui.md), [OBS 오버레이 디자인 명세](spec/overlay-design.md), [디자인 와이어프레임](spec/wireframes.md)에 반영되어 있다.
- schema 변경은 [데이터 모델/마이그레이션 계획](spec/data-model-migration.md)에 반영되어 있다.
- 테스트 범위는 [테스트 전략](spec/test-strategy.md)에 맞는다.
- 운영 절차 변경은 [운영/배포 Runbook](spec/runbook.md)에 반영되어 있다.
