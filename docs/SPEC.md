# Nyang-Nyang Bot Spec

## Purpose

Nyang-Nyang Bot은 CHZZK 채널 운영을 위한 호감도 중심 봇이다.

주요 기능:

- CHZZK 로그인과 관리자 권한 구분
- 호감도 순위, 본인 내역, 관리자 조정
- 업보/쿠폰/리워드 보유 목록
- 출석체크와 주간 채팅 순위
- 후원 룰렛과 OBS 오버레이
- 관리자 명령어 관리

## Roles

| 역할 | 접근 |
| --- | --- |
| 방문자 | 로그인 화면 |
| 일반 사용자 | 본인 호감도, 실제 순위, 본인 히스토리, 본인 업보 목록, 주간 채팅 TOP 10 |
| 관리자 | 전체 목록, 검색, 조정, 출석, 룰렛, 오버레이, 명령어 |

일반 사용자는 다른 사용자의 상세 내역을 볼 수 없다. 일반 사용자 화면은 본인 카드만 보여주되 순위는 전체 호감도 기준 실제 순위를 표시한다.

## Domain Rules

### Favorite

- 호감도는 사용자별 잔액이다.
- 변경은 원장에 기록한다.
- 원장 출처는 `ADMIN_ADJUSTMENT`, `ATTENDANCE`, `SHEET_MIGRATION`, `UPBO_MANUAL`, `UPBO_ROULETTE`, `CORRECTION`이다.
- 잔액은 음수가 될 수 있고 UI에서 위험 상태로 표시한다.
- 순위는 `favorite` 내림차순 기준이다. 동점은 같은 순위로 계산한다.
- 주간 채팅 순위는 관리자와 일반 사용자 화면 모두에서 10위까지 표시한다.

### Upbo

- 업보/쿠폰/리워드는 호감도 잔액과 별도로 보관한다.
- 상태는 `OWNED`, `USED`, `CONVERTED`, `CORRECTED`이다.
- 보상 타입은 `FAVORITE`, `COUPON`, `MISSION`, `PARTICIPATION_PRIORITY`, `CUSTOM`이다.
- 반영 방식은 `AUTO`, `MANUAL`, `NONE`이다.
- `AUTO`는 호감도 원장에 즉시 반영한다.

### Roulette

- 채널에서 사용할 룰렛은 하나만 생성하고 사용한다.
- 활성화 조건:
  - 명령어가 있어야 한다.
  - 1회 금액이 있어야 한다.
  - 활성 항목 확률 합계가 100%여야 한다.
  - 꽝 항목이 하나 이상 있어야 한다.
- 후원 메시지의 룰렛 명령어와 금액으로 회차를 계산한다.
- 결과는 서버에서 먼저 확정하고 원장/업보에 반영한 뒤 오버레이에 보여준다.
- 재송출은 화면 표시만 다시 수행하며 원장에 다시 반영하지 않는다.

### Overlay Token

- OBS 룰렛 오버레이 URL은 `/overlay/roulette#token=...` 형식이다.
- 관리자는 토큰을 발급하고 OBS 브라우저 소스에 등록한다.
- 토큰 발급 화면은 룰렛 관리와 별도 관리자 탭으로 둔다.

### Command

- 명령어 타입은 `TEXT`, `TRIGGER`, `TIMER`이다.
- 시스템 action은 `FAVORITE_STATUS`, `ROULETTE_RESULT`, `ROULETTE_DONATION`이다.
- 룰렛 후원 명령어는 기존 명령어 트리거와 충돌하면 안 된다.

## Web Routes

| Method | Path | 권한 | 용도 |
| --- | --- | --- | --- |
| GET | `/` | 전체 | 랜딩 또는 호감도 페이지 |
| GET | `/favorite/list` | 인증 | 호감도 화면 |
| GET | `/favorite/history` | ADMIN 또는 본인 | 호감도 히스토리 fragment |
| GET | `/favorite/adjustments` | ADMIN | 조정 템플릿 fragment |
| GET | `/favorite/adjustments/modal` | ADMIN | 조정 모달 fragment |
| POST | `/favorite/adjustments` | ADMIN | 조정 템플릿 생성 |
| POST | `/favorite/adjustments/apply` | ADMIN | 호감도 조정 적용 |
| POST | `/attendance/start` | ADMIN | 출석 수집 시작 |
| POST | `/attendance/stop` | ADMIN | 출석 수집 종료 |
| POST | `/attendance/apply` | ADMIN | 출석 보상 적용 |
| POST | `/chzzk/connect` | ADMIN | 채팅 연결 |
| POST | `/google/sync` | ADMIN | Google Sheets 동기화 |
| GET | `/admin/roulette/tables` | ADMIN | 룰렛 설정 fragment |
| POST | `/admin/roulette/tables` | ADMIN | 단일 룰렛 생성 |
| POST | `/admin/roulette/items` | ADMIN | 룰렛 항목 추가 |
| POST | `/admin/roulette/tables/{id}/activate` | ADMIN | 룰렛 활성화 |
| POST | `/admin/roulette/tables/{id}/deactivate` | ADMIN | 룰렛 비활성화 |
| GET | `/admin/roulette/events` | ADMIN | 최근 룰렛 실행 목록 |
| POST | `/admin/overlay/roulette/token` | ADMIN | 오버레이 토큰 발급 |
| POST | `/admin/overlay/roulette/events/replay` | ADMIN | 오버레이 재송출 |
| GET | `/overlay/roulette` | 토큰 | OBS 오버레이 화면 |
| GET | `/overlay/roulette/events/next` | 토큰 | 다음 오버레이 이벤트 |
| GET | `/admin/commands` | ADMIN | 명령어 목록 |
| GET | `/admin/commands/editor` | ADMIN | 명령어 편집 fragment |
| POST | `/admin/commands/validate` | ADMIN | 명령어 검증 |
| POST | `/admin/commands/preview` | ADMIN | 명령어 미리보기 |
| POST | `/admin/commands` | ADMIN | 명령어 저장 |
| POST | `/admin/commands/deactivate` | ADMIN | 명령어 비활성화 |
| GET | `/local/test-login` | local | 테스트 계정 선택 |
| POST | `/local/test-login` | local | 테스트 계정 전환 |

## Architecture

패키지 기준:

- `adapter/in`: 웹, 스케줄러, 외부 입력
- `adapter/out`: DB와 외부 서비스 구현
- `application/port/in`: 유스케이스 계약
- `application/port/out`: 저장소/외부 의존성 계약
- `application/service`: 유스케이스 구현
- `domain`: 순수 도메인 정책과 타입
- `config`: Spring 설정과 local seed

원칙:

- Controller는 화면 모델과 HTTP fragment만 다룬다.
- Service는 유스케이스 규칙을 적용한다.
- Repository/JPA 세부사항은 adapter 밖으로 새지 않는다.
- 도메인 정책은 테스트 가능한 작은 클래스로 둔다.
- Bean Validation은 유스케이스 command의 공통 입력 검증에 사용하고, 타입별 도메인 규칙은 service/domain 정책에 둔다.
- MapStruct mapper는 adapter/out 매핑에만 둔다. domain/application 레이어는 MapStruct에 의존하지 않는다.

## Local Data

`local` profile은 더미 데이터를 자동으로 만든다.

- 호감도 사용자 30명 이상
- 관리자/일반/음수 계정
- 히스토리, 주간 채팅 순위
- 단일 룰렛과 최근 실행 이벤트
- 업보 템플릿과 사용자 보유 업보
- 관리자 조정 템플릿

테스트 계정은 `/local/test-login`에서 전환한다.

## Operations

- 기본 운영 설정은 config server에서 가져온다.
- `local` profile은 config server와 discovery를 끈다.
- Google Sheets 동기화는 관리자 액션이다.
- Grafana 대시보드는 `docs/grafana_dashboard.json`을 사용한다.
- 배포 전 최소 검증은 `.\gradlew.bat test`이다.

## Documentation Policy

문서는 이 파일에 합친다. 새 문서가 필요한 경우는 다음뿐이다.

- 외부 시스템에 업로드해야 하는 산출물
- 긴 표나 대시보드처럼 Markdown 본문에 두면 읽기 어려운 파일
- 코드와 별도로 보존해야 하는 운영 계약
