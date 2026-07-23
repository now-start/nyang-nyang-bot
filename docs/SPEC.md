# Nyang-Nyang Bot Spec

## Purpose

Nyang-Nyang Bot은 CHZZK 채널 운영을 위한 호감도 포인트 중심 봇이다.

주요 기능:

- CHZZK 로그인과 관리자 권한 구분
- 호감도 포인트 순위, 본인 내역, 관리자 조정
- 리워드(쿠폰·미션 등) 보유 목록
- 생존자 보상과 주간 채팅 순위
- 후원 룰렛과 OBS 오버레이
- 관리자 명령어와 타이머 메시지 관리

## Roles

개발자는 배포 코드에서 기능과 변수를 추가하는 주체이며 애플리케이션 RBAC 역할은 아니다. 운영 화면의 관리자는 이 배포를 사용하는 스트리머를 뜻한다.

| 역할 | 접근 |
| --- | --- |
| 방문자 | 로그인 화면 |
| 일반 사용자 | 본인 포인트, 실제 순위, 본인 히스토리, 본인 리워드 목록, 주간 채팅 TOP 10 |
| 관리자 | 전체 목록, 검색, 포인트 조정, 생존자 보상, 룰렛, 오버레이, 명령어, 타이머 메시지 |

일반 사용자는 다른 사용자의 상세 내역을 볼 수 없다. 일반 사용자 화면은 본인 카드만 보여주되 순위는 전체 호감도 기준 실제 순위를 표시한다.

## Domain Rules

### Point

- 포인트 변경은 수정하지 않는 `point_ledger_entry` 원장에 추가한다.
- 사용자 잔액은 별도 잔액 컬럼이 아니라 해당 사용자의 원장 `SUM(delta)`로 계산한다.
- 원장 출처는 `ADMIN_ADJUSTMENT`, `PRESENCE_REWARD`, `SHEET_MIGRATION`, `REWARD_MANUAL`, `REWARD_ROULETTE`, `CORRECTION`이다.
- 잔액은 음수가 될 수 있고 UI에서 위험 상태로 표시한다.
- 순위는 계산된 포인트 잔액 내림차순 기준이다. 동점은 같은 순위로 계산한다.
- 주간 채팅 순위는 관리자와 일반 사용자 화면 모두에서 10위까지 표시한다.

### Reward

- 리워드 지급은 `reward_grant`에 포인트 원장과 별도로 보관한다.
- 상태는 `OWNED`, `USED`, `CONVERTED`, `CORRECTED`이다.
- 보상 타입은 `POINT`, `COUPON`, `MISSION`, `PARTICIPATION_PRIORITY`, `CUSTOM`이다.
- 반영 방식은 `AUTO`, `MANUAL`, `NONE`이다.
- `AUTO`는 포인트 원장에 즉시 반영하고 해당 원장 항목을 참조한다.

### Presence Reward

- 관리자는 생존자 수집 사이클을 시작하고, 수집된 사용자 중 선택한 대상에게 포인트를 지급한다.
- 적용 중에는 사이클을 변경하지 않으며, 트랜잭션 커밋 뒤 사이클을 종료하고 롤백 시 다시 활성 상태로 돌린다.
- 같은 사이클의 사용자별 지급은 `presence:{cycleId}:{userId}` 키로 중복 반영을 막는다.

### Roulette

- 룰렛 설정은 `DRAFT`, `ACTIVE`, `ARCHIVED` 버전으로 관리하며 동시에 하나만 활성화한다.
- 활성화 조건:
  - 후원 키워드가 있어야 한다.
  - 1회 금액이 있어야 한다.
  - 옵션 확률 합계가 100%여야 한다.
  - 확률이 0보다 큰 꽝 옵션이 하나 이상 있어야 한다.
- 후원 메시지의 룰렛 키워드와 금액으로 회차를 계산한다.
- 후원 한 건의 룰렛은 최대 1000회이며 초과 요청은 거부한다.
- 공식 CHZZK 후원 소켓 payload에는 제공자 이벤트 ID가 없다. 애플리케이션은 수신 콜백마다 `chzzk-received:<UUID>` 영수증 키를 만들므로 같은 로컬 영수증 키의 중복은 막지만, 제공자 재전송에 대한 exactly-once는 보장하지 않는다.
- 유효한 후원은 하나의 DB 트랜잭션에서 `donation`과 모든 회차를 저장하고 `roulette_run`을 `READY`로 확정한다.
- 커밋 뒤 `AFTER_COMMIT` 리스너가 회차 리워드를 반영하고 오버레이 작업을 만든다. 실패하거나 프로세스가 종료되면 스케줄러가 미처리 `READY` 실행을 복구한다.
- 재송출은 새 오버레이 작업만 만들며 포인트 원장이나 리워드를 다시 반영하지 않는다.

### Overlay Token

- OBS 룰렛 오버레이 URL은 `/overlay/roulette#token=...` 형식이다.
- 관리자는 토큰을 발급하고 OBS 브라우저 소스에 등록한다.
- 토큰 발급 화면은 룰렛 관리와 별도 관리자 탭으로 둔다.
- 오버레이는 `/overlay/roulette/jobs/next`에서 표시 작업을 클레임하고 `/overlay/roulette/jobs/{displayJobId}/displayed`로 완료한다.

### Command

- 스트리머가 관리하는 명령어는 채팅 호출어와 냥냥봇 응답 템플릿으로 구성하며 DB에 저장한다.
- 채팅 호출어는 `!` 접두사 유무와 관계없이 2~20자의 공백 없는 단일 토큰을 사용한다.
- 방송 채널 자신이 보낸 채팅은 봇 응답의 재실행을 막기 위해 명령어로 처리하지 않는다.
- 후원 룰렛 키워드는 일반 채팅 호출어와 별도이며 `!` 접두사를 유지한다.
- 개발자가 제공하는 데이터는 namespaced 변수로 노출한다. 예: `{viewer.nickname}`, `{point.balance}`.
- 변수 정의와 조회 로직은 코드의 `CommandVariableContributor`가 소유하고, 명령 실행 시 템플릿에서 실제 사용한 contributor만 한 번 호출한다.
- 변수 키를 변경할 때는 같은 Flyway 마이그레이션에서 저장된 템플릿을 새 키로 이전하며, 런타임에 이전 키 별칭을 유지하지 않는다.
- 변수 조회는 읽기 전용이며 데이터가 없는 호감도는 `0`으로 표시하고 계정을 생성하지 않는다.
- 승인된 명령 실행은 `command_execution`에 추가하며 `{count.total}`과 `{count.user}`는 이 테이블의 전체/사용자별 행 수로 계산한다. 현재 승인된 실행도 횟수에 포함한다.
- `{streak.current}`와 `{streak.longest}`는 `USER_CALENDAR_DAY` 실행의 `Asia/Seoul` 날짜만 사용한다. `USER_INTERVAL` 실행에는 날짜가 없으므로 연속 실행일에 포함하지 않는다.
- 저장 템플릿 원문은 최대 1000자이며, 실제 채팅 응답은 변수 치환 후 유니코드 문자 기준 최대 300자로 제한한다.
- 기본 명령어 프리셋은 DB에 최초 복사된 이후 스트리머가 독립적으로 수정한다.
- 룰렛 후원 실행은 채팅 명령어가 아니다. 후원 키워드, 금액, 활성 상태는 룰렛 설정이 직접 소유한다.
- 타이머 메시지는 채팅 명령어와 별도 모델로 관리하며 호출자 문맥이 없는 `time.*` 변수만 사용한다.

### Timer Message

- 타이머 메시지는 발송 간격과 최소 시청자 채팅 수를 모두 충족해야 실행한다.
- 채팅 수는 각 타이머의 마지막 성공 발송 이후부터 누적하며, 발송 중 들어온 채팅은 다음 회차에 보존한다.
- 실행기는 만료 시간이 있는 DB 조건부 클레임을 사용해 여러 인스턴스가 동시에 같은 타이머를 발송하지 않도록 한다.
- 발송 성공 시 클레임 시점까지의 채팅 수만 차감하고 다음 실행 시각을 계산한다.
- 발송 실패 시 클레임을 해제하고 1분 뒤 재시도하며, 한 타이머의 실패가 다른 타이머 실행을 막지 않는다.
- CHZZK 메시지 API가 멱등성 키를 받지 않으므로 HTTP 성공 직후 프로세스가 종료되는 극단적 상황은 at-least-once 재전송 가능성이 있다.

## Web Routes

| Method | Path | 권한 | 용도 |
| --- | --- | --- | --- |
| GET | `/` | 전체 | 랜딩 또는 호감도 페이지 |
| GET | `/points/list` | 인증 | 호감도 화면 |
| GET | `/points/history` | ADMIN 또는 본인 | 호감도 히스토리 fragment |
| GET | `/points/adjustments` | ADMIN | 조정 템플릿 fragment |
| GET | `/points/adjustments/modal` | ADMIN | 조정 모달 fragment |
| POST | `/points/adjustments` | ADMIN | 조정 템플릿 생성 |
| POST | `/points/adjustments/apply` | ADMIN | 호감도 조정 적용 |
| GET/POST | `/presence-rewards/users` | ADMIN | 수집된 생존자 목록 fragment |
| POST | `/presence-rewards/start` | ADMIN | 생존자 수집 시작 |
| POST | `/presence-rewards/stop` | ADMIN | 생존자 수집 종료 |
| POST | `/presence-rewards/apply` | ADMIN | 선택 대상 포인트 지급 |
| POST | `/chzzk/connect` | ADMIN | 채팅 연결 |
| POST | `/google/sync` | ADMIN | Google Sheets 동기화 |
| GET | `/admin/roulette/configs` | ADMIN | 룰렛 설정 fragment |
| GET | `/admin/roulette/configs/{configId}/detail` | ADMIN | 룰렛 설정 상세 fragment |
| POST | `/admin/roulette/configs` | ADMIN | 룰렛 설정 생성 |
| POST | `/admin/roulette/options` | ADMIN | 룰렛 옵션 추가 |
| POST | `/admin/roulette/configs/{configId}/activate` | ADMIN | 룰렛 설정 활성화 |
| POST | `/admin/roulette/configs/{configId}/archive` | ADMIN | 룰렛 설정 보관 |
| GET | `/admin/roulette/configs/{configId}/simulation` | ADMIN | 룰렛 확률 시뮬레이션 |
| GET | `/admin/roulette/runs` | ADMIN | 최근 룰렛 실행 목록 |
| POST | `/admin/overlay/roulette/token` | ADMIN | 오버레이 토큰 발급 |
| POST | `/admin/overlay/roulette/runs/replay` | ADMIN | 실행 ID 폼 기반 오버레이 재송출 |
| POST | `/admin/overlay/roulette/runs/{rouletteRunId}/replay` | ADMIN | 지정 실행 오버레이 재송출 |
| GET | `/overlay/roulette` | 전체 | OBS 오버레이 화면 |
| GET | `/overlay/roulette/jobs/next` | 오버레이 토큰 | 다음 표시 작업 클레임 |
| POST | `/overlay/roulette/jobs/{displayJobId}/displayed` | 오버레이 토큰 | 표시 작업 완료 |
| GET | `/admin/commands` | ADMIN | 명령어 목록 |
| GET | `/admin/commands/editor` | ADMIN | 명령어 선택 안내 또는 편집 fragment |
| GET | `/admin/commands/editor/new` | ADMIN | 새 명령어 편집 fragment |
| POST | `/admin/commands/preview` | ADMIN | 명령어 검증과 미리보기 |
| POST | `/admin/commands` | ADMIN | 명령어 저장 |
| POST | `/admin/commands/deactivate` | ADMIN | 명령어 비활성화 |
| GET | `/admin/timers` | ADMIN | 타이머 메시지 목록 |
| GET | `/admin/timers/editor` | ADMIN | 타이머 선택 안내 또는 편집 fragment |
| GET | `/admin/timers/editor/new` | ADMIN | 새 타이머 편집 fragment |
| POST | `/admin/timers/preview` | ADMIN | 타이머 검증과 미리보기 |
| POST | `/admin/timers` | ADMIN | 타이머 저장 |
| POST | `/admin/timers/deactivate` | ADMIN | 타이머 비활성화 |
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

- 하나의 배포는 설정된 CHZZK 채널 하나와 스트리머 한 명을 담당한다.
- Controller는 화면 모델과 HTTP fragment만 다룬다.
- Service는 유스케이스 규칙을 적용한다.
- Repository/JPA 세부사항은 adapter 밖으로 새지 않는다.
- 도메인 정책은 테스트 가능한 작은 클래스로 둔다.
- 기능 확장은 중앙 enum 분기를 추가하는 대신 기능별 contributor 또는 전용 유스케이스 Bean을 등록한다.
- Bean Validation은 유스케이스 command의 공통 입력 검증에 사용하고, 타입별 도메인 규칙은 service/domain 정책에 둔다.
- MapStruct mapper는 adapter/out 매핑에만 둔다. domain/application 레이어는 MapStruct에 의존하지 않는다.

## Local Data

`local` profile은 더미 데이터를 자동으로 만든다.

- 포인트 사용자 30명 이상
- 관리자/일반/음수 계정
- 포인트 히스토리와 주간 채팅 순위
- 활성 룰렛 설정과 옵션
- 비활성 타이머 메시지 예시
- 명령어 프리셋과 관리자 포인트 조정 프리셋

테스트 계정은 `/local/test-login`에서 전환한다.

## Operations

- 기본 운영 설정은 config server에서 가져온다.
- `local` profile은 config server와 discovery를 끈다.
- Google Sheets 동기화는 관리자 액션이다.
- Grafana 대시보드는 `docs/grafana_dashboard.json`을 사용한다.
- 배포 전 최소 검증은 `sh gradlew test`이다.
- canonical 전환은 V7~V9에서 shadow schema 생성·backfill·검증을 끝낸 뒤 V10에서 승인된 컷오버를 수행한다. 절차와 복구 조건은 `docs/database/schema-redesign.md`를 따른다.

## Documentation Policy

문서는 이 파일에 합친다. 새 문서가 필요한 경우는 다음뿐이다.

- 외부 시스템에 업로드해야 하는 산출물
- 긴 표나 대시보드처럼 Markdown 본문에 두면 읽기 어려운 파일
- 코드와 별도로 보존해야 하는 운영 계약
