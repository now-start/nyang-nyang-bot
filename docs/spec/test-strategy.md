# 테스트 전략

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 기준 아키텍처: [클린 아키텍처](architecture.md)
- 목적: PRD 구현 시 필요한 테스트 계층, 범위, 완료 기준을 정의한다.

## 1. 원칙

- 테스트는 클린 아키텍처 경계에 맞춰 작성한다.
- Domain 테스트는 빠르고 프레임워크 의존성이 없어야 한다.
- Application 테스트는 gateway를 fake/mock으로 대체해 유스케이스 규칙을 검증한다.
- Adapter 테스트는 Spring MVC, JPA, Feign DTO 매핑 같은 기술 경계를 검증한다.
- Integration 테스트는 wiring, 트랜잭션, 보안 설정을 검증한다.
- 아키텍처 테스트로 역방향 의존성을 막는다.

## 2. 테스트 피라미드

| 계층 | 대상 | 도구 | 검증 예시 |
| --- | --- | --- | --- |
| Domain unit | Domain model, domain service | JUnit | 잔액 계산, 업보 상태 전이, 룰렛 확률 검증 |
| Application use case | Use case service + fake gateway | JUnit, Mockito 또는 hand-written fake | 원장 기록, 중복 방지, 정정 거래, 권한 의도 |
| Adapter slice | Controller, persistence adapter | Spring MVC Test, DataJpaTest | 요청/응답 매핑, 권한, JPA query |
| Integration | Spring context + DB | SpringBootTest | 트랜잭션, Security, 전체 wiring |
| Architecture | 패키지 의존성 | ArchUnit 도입 권장 | Domain/Application 의존성 규칙 |

현재 `build.gradle`에는 `spring-boot-starter-test`가 포함되어 있다. 아키텍처 테스트를 자동화하려면 Phase 0 또는 Phase 1에서 ArchUnit 테스트 의존성을 추가한다.

## 3. Phase별 필수 테스트

| Phase | 필수 테스트 |
| --- | --- |
| Phase 1 | OAuth state 성공/실패, 토큰 로그 미노출, 관리자 API 거부 |
| Phase 2 | 호감도 증감, 음수 허용 정책, idempotency, 정정 거래, 비관적 락 경로 |
| Phase 3 | 본인/관리자 조회 권한, 히스토리 50건 제한, `lastSeenAt` 갱신 |
| Phase 4 | 업보 자동 전환, 환산 불가 보유, 사용 처리, 정정 처리, 관리자 메모 비노출 |
| Phase 5 | 출석 사이클 중복 지급 방지, 취소 미지급, 명령어 쿨타임 |
| Phase 6 | 룰렛 확률 합계 검증, 꽝 필수, 명령어 매칭, 후원 idempotency, 다회차 재처리 |
| Phase 7 | 오버레이 토큰 검증, 이벤트 만료, 표시 완료, 재송출 원장 미반영 |
| Phase 8 | logfmt 키 출력, 민감 값 미노출, Grafana/Loki query 호환 |

## 4. Domain 테스트 기준

대상:

- `FavoriteAccount`
- `FavoriteLedgerEntry`
- `UpboTemplate`
- `UserUpbo`
- `AttendanceCycle`
- `RouletteTable`
- `RouletteDrawPolicy`
- `OverlayDisplayEvent`

검증:

- 호감도 잔액 증가/감소와 `balanceAfter` 계산.
- 관리자 경로가 아닌 음수 잔액 거래 거부.
- 정정 거래가 원본 거래를 삭제하지 않고 연결만 만든다.
- 자동 전환 업보와 보유 전용 업보가 분리된다.
- 룰렛 확률 합계가 100%가 아니면 활성화 불가.
- `꽝` 항목이 없거나 0%이면 활성화 불가.
- 오버레이 이벤트가 120초 이후 `MISSED`가 된다.

## 5. Application 테스트 기준

Application 테스트는 DB와 외부 API 없이 fake gateway로 작성한다.

검증:

- `AdjustFavoriteUseCase`가 계정 조회, 락 획득, 원장 저장, 잔액 저장을 순서대로 호출한다.
- 같은 `idempotencyKey`는 두 번째 호출에서 잔액을 변경하지 않는다.
- `ApplyUpboUseCase`가 `AUTO` 항목에 대해 Favorite use case를 호출한다.
- `RunRouletteFromDonationUseCase`가 후원 시점 스냅샷으로 결과를 확정한다.
- 회차별 반영 실패 시 실패 회차만 재처리 대상으로 남긴다.
- `ReplayOverlayEventUseCase`가 원장 gateway를 호출하지 않는다.

## 6. Adapter 테스트 기준

Web adapter:

- 일반 사용자가 다른 사용자의 상세 히스토리를 조회하면 거부된다.
- 관리자는 닉네임 부분 일치 검색을 수행할 수 있다.
- request DTO 검증 실패 시 적절한 4xx 응답을 반환한다.
- JPA entity가 response body로 직접 노출되지 않는다.

Persistence adapter:

- 사용자별 비관적 락 query가 존재한다.
- `idempotencyKey` UNIQUE 제약 위반을 application 오류로 변환한다.
- 원장 최신순/페이지네이션 query가 50건 제한을 따른다.
- `lastSeenAt` 갱신이 저장된다.

External adapter:

- CHZZK OAuth/세션/채팅/후원 DTO를 application command로 변환한다.
- 후원 메시지의 명령어는 토큰 단위 정확 일치만 매칭한다.
- 토큰 원문은 로그에 남지 않는다.

## 7. Integration 테스트 기준

- Spring Security 설정과 web adapter 권한이 실제로 연결되어야 한다.
- `@Transactional` 경계에서 원장 저장과 잔액 변경이 함께 rollback되어야 한다.
- H2 또는 test container DB에서 JPA mapping이 검증되어야 한다.
- OpenAPI/Actuator 공개 범위가 의도와 맞는지 검증한다.
- 필요 시 CHZZK/Google external adapter는 mock server로 대체한다.

## 8. 아키텍처 테스트 기준

도입 권장 규칙:

- `..domain..`은 `org.springframework..`, `jakarta.persistence..`, `jakarta.servlet..`, `feign..`에 의존하지 않는다.
- `..domain..`에는 `*Dto` 클래스를 두지 않는다.
- `..application..`은 `..adapter..`에 의존하지 않는다.
- `..application.model..` 패키지는 사용하지 않고 내부 모델은 `..domain.model..` 또는 기능별 domain 패키지에 둔다.
- `..application.port..` 패키지는 사용하지 않고 외부 호출 계약은 `..application.gateway.out..`에 둔다.
- `..adapter..`만 web/persistence/external framework에 의존한다.
- `..config..`는 조립만 담당하고 domain 규칙을 포함하지 않는다.
- JPA entity는 `..adapter.out.persistence.entity..`에만 위치한다.
- 루트 패키지는 `adapter`, `application`, `domain`, `config` 네 개만 허용한다.

## 9. 릴리즈 체크리스트

- `./gradlew test --no-daemon` 통과.
- WSL wrapper 문제가 있으면 `java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --no-daemon`로 동일 검증.
- OAuth state 성공/실패 수동 검증.
- 관리자/일반 사용자 권한 수동 검증.
- 호감도 원장 거래와 잔액 일치 검증.
- 업보 자동 전환과 보유 전용 항목 검증.
- 출석체크 적용/취소 검증.
- 룰렛 다회차 확정/반영/재처리 검증.
- 오버레이 토큰 발급/재발급/폐기 검증.
- OAuth token, OAuth state, overlay token 원문 로그 미노출 확인.
- Grafana/Loki query 확인.
- ArchitectureBoundaryTest 통과.

## 10. 미결정 사항

| 항목 | 선택지 | 결정 필요 시점 |
| --- | --- | --- |
| Architecture test 도구 | ArchUnit, custom reflection test | Phase 0 또는 Phase 1 |
| DB integration 방식 | H2, Testcontainers MariaDB | 첫 schema migration 전 |
| 외부 API 테스트 방식 | MockWebServer, WireMock, fake adapter | CHZZK 룰렛 구현 전 |
| E2E 테스트 범위 | 핵심 관리자 플로우만, 전체 화면 플로우 | Phase 4 이후 |
