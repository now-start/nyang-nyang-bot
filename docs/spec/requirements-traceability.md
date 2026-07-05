# 요구사항 추적표

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 기준 아키텍처: [클린 아키텍처](architecture.md)
- 목적: Spec의 기능 요구사항을 Phase, 클린 아키텍처 경계, 구현 상태, 검증 기준에 매핑한다.

## 1. 상태 정의

| 상태 | 의미 |
| --- | --- |
| `현행 구현` | 현재 PRD 기준으로 이미 존재하는 기능이다. 클린 아키텍처 전환과 보안/테스트 보강은 별도 필요할 수 있다. |
| `부분 구현` | 기능 일부가 있으나 PRD 요구사항을 모두 만족하지 않는다. |
| `미구현` | 신규 구현이 필요하다. |
| `보류` | PRD상 후순위 정책으로 남긴다. |

## 2. 추적표

| ID | 요구사항 | 우선순위 | 상태 | Phase | 주요 Use Case | 주요 검증 |
| --- | --- | --- | --- | --- | --- | --- |
| FR-001 | CHZZK OAuth 로그인 | P0 | 현행 구현 | Phase 1 | Spring Security OAuth2 Client | OAuth state 검증, 토큰 로그 미노출 |
| FR-002 | 관리자 권한 구분 | P0 | 부분 구현 | Phase 1 | `AuthorizeAdminUseCase` | 일반 사용자 관리자 API 거부 |
| FR-003 | 호감도 보드 조회 | P0 | 현행 구현 | Phase 3 | `ViewFavoriteBoardUseCase` | 페이지네이션, 음수 잔액 표시 |
| FR-004 | 전체 히스토리 조회 | P0 | 부분 구현 | Phase 3 | `ViewFavoriteHistoryUseCase` | 본인/관리자 권한, 50건 제한 |
| FR-005 | 업보/호감도 반영 점수 조회 | P0 | 미구현 | Phase 3, Phase 4 | `ViewUserUpboHistoryUseCase` | 반영 전/후 점수, 출처 표시 |
| FR-006 | 업보/쿠폰 목록 관리 | P0 | 미구현 | Phase 4 | `ViewUserUpboUseCase`, `UseRewardUseCase` | 상태 필터, 자동 전환 제외 |
| FR-007 | 호감도 수동 조정 | P0 | 부분 구현 | Phase 2 | `AdjustFavoriteUseCase` | 원장 기록, 관리자 권한, 음수 처리 |
| FR-008 | 출석체크 적용 | P0 | 부분 구현 | Phase 5 | `ApplyAttendanceRewardUseCase` | 사이클 내 1회 지급, 취소 미지급 |
| FR-009 | `!호감도` 명령 처리 | P0 | 현행 구현 | Phase 5 | `ViewFavoriteForChatUseCase` | 사용자별 쿨타임, 본인 정보 응답 |
| FR-010 | Google Sheets 포인트 마이그레이션 | P0 | 현행 구현 | Phase 8 | `MigrateFavoriteFromSheetUseCase` | 중복 실행 방지, 검증 리포트 |
| FR-011 | 업보 템플릿/자유 입력 관리 | P0 | 미구현 | Phase 4 | `CreateUpboTemplateUseCase`, `ApplyUpboUseCase` | 필수값 검증, 활성 여부 |
| FR-012 | 업보 수동 적용/정정 | P0 | 미구현 | Phase 4 | `ApplyUpboUseCase`, `CorrectUpboUseCase` | 원장 연결, 정정 공개 |
| FR-013 | 관리자 수동 구매/사용 처리 | P0 | 미구현 | Phase 4 | `PurchaseRewardUseCase`, `UseRewardUseCase` | 잔액 부족 시 관리자만 음수 허용 |
| FR-014 | 호감도 원장 확장 | P0 | 미구현 | Phase 2 | `RecordFavoriteLedgerUseCase` | `delta`, `balanceAfter`, 출처, 주체 |
| FR-015 | 호감도 정책 관리 | P0 | 미구현 | Phase 4 | `ManageFavoritePolicyUseCase` | 정책값 코드 고정 제거 |
| FR-016 | 치지직 후원 이벤트 구독 | P1 | 현행 구현 | Phase 6 | `SubscribeDonationEventUseCase` | 후원 이벤트 저장, 재연결 |
| FR-017 | 후원 명령어 기반 룰렛 실행 | P1 | 현행 구현 | Phase 6 | `RunRouletteFromDonationUseCase` | `ROULETTE_DONATION` 토큰 단위 명령 매칭, 현재 활성 룰렛 테이블 기준 실행, 회차 계산 |
| FR-018 | OBS 룰렛 오버레이 | P1 | 현행 구현 | Phase 7 | `PollOverlayEventUseCase` | 120초 유효시간, 순차/요약 표시 |
| FR-019 | 룰렛 결과 반영/조회 | P1 | 현행 구현 | Phase 6, Phase 7 | `ApplyRouletteRoundResultUseCase`, `ViewRecentRouletteResultUseCase` | 원장 반영, 자동 채팅 공지 없음 |
| FR-020 | 룰렛 확률표 관리 | P1 | 현행 구현 | Phase 6 | `ManageRouletteTableUseCase` | 합계 100%, 꽝 필수, 공개 여부, 활성 룰렛 테이블 1개 제한 |
| FR-021 | 룰렛 결과 정정 | P1 | 미구현 | Phase 6 | `CorrectRouletteResultUseCase` | 삭제 없는 보정 거래 |
| FR-022 | 관리자 명령어 관리 | P1 | 현행 구현 | Phase 5 | `ManageCommandUseCase`, `ChatService` | 텍스트 명령어 CRUD, 시스템 명령어 정책, 단일 테이블 트리거 검증, 관리자 UI |
| FR-023 | 공개 채널 페이지 | P2 | 보류 | Phase 4 이후 | `ViewPublicChannelPageUseCase` | 공개 랭킹/이벤트 규칙 |
| FR-024 | 오버레이 토큰 관리 | P1 | 현행 구현 | Phase 7 | `IssueOverlayTokenUseCase`, `RotateOverlayTokenUseCase` | 원문 1회 표시, 해시 저장 |
| FR-025 | 룰렛 오버레이 재송출 | P1 | 현행 구현 | Phase 7 | `ReplayOverlayEventUseCase` | 원장 재반영 없음 |

## 3. Phase별 최소 완료 묶음

| Phase | 완료 기준 |
| --- | --- |
| Phase 1 | FR-001, FR-002의 보안 위험이 제거된다. |
| Phase 2 | FR-007, FR-014가 원장 기반으로 동작한다. |
| Phase 3 | FR-003, FR-004, FR-005 조회 권한과 DTO가 정리된다. |
| Phase 4 | FR-006, FR-011, FR-012, FR-013, FR-015가 관리자 흐름으로 동작한다. |
| Phase 5 | FR-008, FR-009, FR-022가 원장, 쿨타임, 관리자 명령어 기준을 따른다. |
| Phase 6 | FR-016, FR-017, FR-019, FR-020, FR-021의 서버 확정/반영 흐름이 동작한다. |
| Phase 7 | FR-018, FR-024, FR-025의 오버레이 표시/토큰/재송출 흐름이 동작한다. |

## 4. 추적 관리 규칙

- 구현 PR은 관련 FR ID를 설명에 포함한다.
- 테스트 이름 또는 테스트 클래스 주석에 관련 FR ID를 남긴다.
- 상태가 `현행 구현`이어도 클린 아키텍처 전환, 권한, 테스트가 부족하면 해당 Phase에서 `부분 구현`으로 낮춘다.
- 요구사항이 바뀌면 PRD 본문과 이 추적표를 같은 PR에서 갱신한다.
