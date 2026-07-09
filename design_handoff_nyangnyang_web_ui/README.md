# Handoff: 냥냥봇 (치즈냥 호감도) 웹 UI — 1c 소프트 엘리베이션 톤

## Overview
CHZZK 채널 커뮤니티의 호감도(잔액형 포인트) 관리 봇 "냥냥봇"의 웹 UI 하이파이 디자인.
`docs/spec/ux-ui-guidelines.md`, `web-ui.md`, `wireframes.md` 스펙을 기반으로 하며, 사용자가 선택한 **1c 소프트 엘리베이션** 비주얼 톤(그린 틴트 다크 그레이 + 섀도 카드 + CHZZK Neon Green 단일 액센트)으로 전체 화면을 전개했다.

## About the Design Files
이 번들의 `냥냥봇 디자인.dc.html`은 **HTML로 만든 디자인 레퍼런스**다. 프로덕션 코드가 아니며 그대로 복사하지 않는다.
목표는 이 디자인을 대상 코드베이스의 기존 환경 — **Spring Boot MVC + Thymeleaf fragment + Bootstrap 5.x + htmx 2.x** — 으로 재구현하는 것이다.
스펙(ux-ui-guidelines.md §5.1)에 따라:

- 화면별 커스텀 CSS/JS 파일을 새로 만들지 않는다. Bootstrap utility class를 우선 사용한다.
- 정확한 브랜드 색(Neon Green `#00FFA3`)이 필요한 부분은 **전역 Bootstrap theme build(SCSS 변수 오버라이드)** 로만 처리한다.
- 부분 갱신은 htmx(`hx-get`, `hx-post`, `hx-target`, `hx-swap`, `hx-indicator`)로 처리한다.
- 유일한 예외는 OBS 오버레이 — 실시간 소켓/애니메이션이 핵심이므로 전용 CSS/JS 허용.

## Fidelity
**High-fidelity.** 색상, 타이포, 간격, 라운드, 상태 배지, 카피가 모두 확정값이다. Bootstrap 컴포넌트로 재구현하되 시각 결과는 디자인 파일과 최대한 일치시킨다.
디자인 파일 안에서 화면 id는 `1c`(호감도 보드), `2a`~`2f`, `3a`, `3b`로 표시되어 있다.

## Design Tokens (전역 Bootstrap theme build 기준)

색상:
| 토큰 | 값 | Bootstrap 매핑 제안 |
| --- | --- | --- |
| 페이지 배경 | `#101312` | `$body-bg` (dark mode) |
| 헤더 그라데이션 | `linear-gradient(180deg,#151918,#101312)` | hero/헤더 영역만 |
| 카드 표면 | `#181d1b` | `$card-bg` |
| 카드 표면(강조/헤더행) | `#1a1f1d` | `$table-striped-bg` 계열 |
| 칩/서브 표면 | `#202623` | badge neutral |
| 입력 배경 | `#161a19` | `$input-bg` |
| 보더 | `#242b28` (카드) / `#2c3330` (입력·버튼) / `#3c4541` (hover) | `$border-color` |
| 본문 텍스트 | `#e6e9e7` | `$body-color` |
| 보조 텍스트 | `#8f9894` | `$text-muted` |
| 3차 텍스트 | `#6f7773` | — |
| 흰색 강조 | `#ffffff` | 제목·주요 숫자 |
| **Primary (CHZZK Neon Green)** | `#00FFA3` | `$primary` / `$success` 오버라이드. 버튼 텍스트는 `#001b10` |
| Primary 링크 | `#00c37e` (hover `#00FFA3`) | `$link-color` |
| Danger | `#ff6b6b` | 음수·실패·폐기 전용. 일반 강조색 금지 |
| Green tint 배경 | `rgba(0,255,163,0.06~0.14)` | 활성 배지·검증 OK 배너 |
| Red tint 배경 | `rgba(255,107,107,0.05~0.14)` | 음수·실패 배지 |

타이포그래피:
- 폰트 스택: `"Archivo","Space Grotesk","Apple SD Gothic Neo","Malgun Gothic",sans-serif` (Google Fonts: Archivo 400/500/600/700)
- 화면 제목 22px/700, 랜딩 h1 32px/700, letter-spacing -0.01em
- 본문 13~14.5px, 보조/label 11.5~12.5px, 테이블 헤더 11.5px
- 숫자는 전부 `font-variant-numeric: tabular-nums`, 숫자 열은 우측 정렬
- 스탯 숫자 22~26px/700

간격·형태:
- radius: 카드/패널 14~16px, 입력·버튼 10~12px, 작은 버튼 8~9px, pill(배지·칩·토글) 999px
- 카드 그림자 `0 2px 8px rgba(0,0,0,0.2~0.25)`, 모달 `0 20px 60px rgba(0,0,0,0.5)`
- Primary 버튼 glow `0 4px 14px rgba(0,255,163,0.2)`
- 버튼 높이: 주요 38~40px, 행 내부 28~32px. 같은 줄의 버튼 높이는 동일
- 페이지 폭: 기본 1080px, 관리자 밀집 화면 최대 1280px
- 콘텐츠 패딩: 좌우 32px, 섹션 간 16~24px, 카드 행 gap 8px

## Screens / Views

### 1. 호감도 보드 (`/favorite/list`) — 디자인 id `1c`
- 헤더(그라데이션 배경): 좌측 로고 사각형(44px, green bg, radius 14) + "치즈냥 호감도" 20px/700 + 채널·권한 서브텍스트. 우측 [데이터 동기화](secondary) [출석체크 시작](primary, glow)
- 요약 스탯 3열 그리드(gap 12): 등록 사용자 148명 / 이번 주 지급 호감도 +312(green) / 주간 채팅 1위 츄르도둑 412회
- 검색: input(h42, radius 12) + [검색] 버튼 한 줄. 목록보다 시각적으로 작게
- 랭킹 목록: **카드형 행** (개별 카드, radius 14, gap 8). 그리드 `44px minmax(0,1fr) 130px 110px`
  - 순위 뱃지: 28px 사각(radius 9). 1위는 green tint bg + green 텍스트, 나머지 `#202623` bg
  - 히스토리 펼침 행: 카드 보더가 `#00FFA3`로 변경 + 약한 green glow, 내부에 dashed divider 후 히스토리 3행 (일시 / 분류 pill / 증감 / 반영 후 / 설명)
  - 음수 행: red tint bg + red 보더 + "음수 잔액" pill + 숫자 red/800 (색+배지+숫자 3중 표시)
- 페이지네이션: 34px 정사각 버튼, 현재 페이지만 green fill
- 상태: 빈 목록/검색 결과 없음/히스토리 로딩(skeleton)/권한 없음(toast) — web-ui.md §6 표 준수
- htmx 포인트: 검색·페이지네이션·히스토리 펼침은 fragment 교체(`hx-get` + `hx-target`)

### 2. 로그인/랜딩 — `2a`
- 유일하게 hero 허용. 중앙 정렬: 로고(64px green 사각형) → h1 "치즈냥 호감도" → 2줄 설명 → [치지직 계정으로 로그인](h48, primary) → 보안 각주 12px → 기능 3개 도트 리스트
- 배경에 상단 radial green glow `rgba(0,255,163,0.06)` 한 번만

### 3. 마이페이지 — `2b`
- 헤더: "내 호감도" + 우측 "새 항목 3건" pill(green tint, 도트 포함) — 진입 시 읽음 처리
- 스탯 3열: 현재 호감도 875(green 26px) / 누적 업보 반영 +42 / 최근 룰렛 결과 호감도 +10
- 보유 업보·쿠폰·리워드: 필터 pill 4개(OWNED 기본 선택=green fill, 나머지 outline). 행 그리드 `96px 1fr 110px 200px` (일시/이름+NEW/상태 배지/환산 정보). 원장 참조는 링크(`#512`)
- 전체 히스토리: 단일 카드 테이블. 그리드 `110px 90px 70px 90px 1fr` (일시/분류 pill/증감/반영 후/설명). 정정 행은 red pill + 원본 거래 링크. "최신순 · 50건 단위" 표기

### 4. 출석체크 모달 — `2c`
- 모달 560px, 구조: header(제목 + "수집 중" 상태 pill + ✕) → 설정(지급 호감도 input, 값은 `+1`처럼 부호 표시, green 텍스트 / 대상자 24명·선택 22명 + [새로고침]) → 대상자 체크리스트(max-height 230, 내부 스크롤, 행에 채팅 횟수 표시, 지급 완료자는 비활성 + 사유 텍스트) → footer("사용자당 1회만 지급" 안내 + [취소하고 종료](danger outline) [선택 22명에게 +1 적용](primary))
- 적용 버튼 라벨에 인원수·지급값을 동적으로 포함

### 5. 룰렛 테이블 관리 (`/admin/roulette/tables`) — `2d`
- 헤더 + [1만 회 시뮬레이션](secondary) [새 룰렛](primary)
- 설정 카드: 5열 그리드 — 이름 / 후원 명령어(`!룰렛`, green 텍스트) / 1회 금액 / 최대 순차 애니메이션 / 공개·활성 토글 2개(36×20 pill 토글)
- 항목 테이블: 그리드 `1fr 120px 170px 120px` (항목/확률 % 4소수 우측정렬/결과 타입 enum/액션). "꽝" 행은 "필수" 칩 + 액션 "고정"
- **검증 배너(핵심)**: green tint 배너에 "확률 합계 100.0000%" + "꽝 포함 OK" + 항목 수 + [저장][활성화]. 합계≠100이면 red 배너로 전환하고 활성화 버튼 disabled
- 시뮬레이션 카드: 기대 확률/시뮬레이션/차이(+green, -red)

### 6. 명령어 관리 (`/admin/commands`) — `2e`
- 2-column: 좌측 목록(1.15fr), 우측 편집 폼(1fr)
- 목록 행: 그리드 `110px 70px 1fr 64px` — 트리거(green/700) / 유형 배지(TEXT=green tint, TRIGGER=neutral) / 응답·action 요약(ellipsis) / 상태 배지(활성=green tint, 비활성=neutral + 행 opacity 0.65)
- 편집 중 행: green 보더 + "마지막 수정" 메타
- 편집 폼(카드): 트리거+쿨타임 → 메시지 템플릿(허용 변수 `{nickname}` `{favorite}` label에 명시) → **충돌 검증 결과 배너**(green tint "충돌 없음…") → 미리보기(dashed 보더 박스, 봇 닉네임 green + 치환된 샘플) → [비활성화](danger outline, 좌) [취소][저장](우)
- 시스템 명령어(TRIGGER)는 템플릿 편집 불가, 트리거·활성·쿨타임만 수정

### 7. 관리자 호감도/업보 처리 모달 — `3a`
- 모달 720px. 구조: header("관리자 처리" + "ADMIN 전용" 칩 + ✕) → 대상 검색 + 선택 사용자 카드(아바타 이니셜, 닉네임, channelId 축약, 현재 호감도 green) → 처리 유형 pill 5개(호감도 지급/차감 · 업보 지급 · 쿠폰 구매 · 사용 처리 · 정정) → 템플릿 select + 증감값(`+10` 부호 필수, green, 우측정렬) → 공개 설명 / 관리자 메모(비공개 칩 + **dashed 보더**로 구분) → footer 미리보기 "현재 875 + +10 = 적용 후 885"(적용 후는 green tint 박스) + [취소][적용]
- 미저장 입력 상태에서 닫기 시 확인 필요

### 8. OBS 토큰 관리 — `3b`
- 헤더 + [새 토큰 발급](primary)
- 발급 직후 배너(green tint): "이 값은 다시 볼 수 없습니다" + URL 코드 블록(monospace, green) + [URL 복사][확인했습니다]
- 토큰 테이블: 이름/상태(ACTIVE=green, REVOKED=red)/마지막 사용/액션([URL 복사][재발급][폐기=danger outline]). REVOKED 행 opacity 0.6
- 재송출 카드: "재송출은 원장에 재반영되지 않습니다" 캡션 필수. 이벤트 행: 일시/요약/상태(표시 완료=green, 표시 실패=red)/[재송출] — 실패 건은 primary fill로 강조

### 9. OBS 룰렛 오버레이 (`/overlay/roulette#token=...`) — `2f`
- 1920×1080, 배경 완전 투명. IDLE/CONNECTING/READY/ERROR는 투명 (ERROR는 debug mode에서만 소형 표시)
- DISPLAYING 패널: 중앙, 폭 ~760px(디자인은 50% 스케일 380px), `rgba(0,0,0,0.82)` bg + green 보더 `rgba(0,255,163,0.35)` + radius 40px(스케일 기준 20px)
  - 제목 "{닉네임}님의 룰렛" 38px/700 + 회차 pill "3 / 11회"
  - 휠: conic-gradient 세그먼트 + 중앙 블랙 원(로고) + 상단 흰색 포인터
  - 결과 라벨 52px/800 green + green text-shadow
- SUMMARY 패널: "추가 6회 결과" + 항목×개수 리스트(당첨=green, 꽝=gray)
- 타이밍(overlay-design.md §6): 진입 300ms → 회전 1800ms → 감속 900ms → 결과 강조 1500ms → 퇴장 300ms. 최종 정지 결과는 서버 확정값과 일치
- 주요 UI는 중앙 70% 안전 영역 내. 1280×720에서 텍스트 잘림 없음

## Interactions & Behavior
- hover: 카드 보더 `#242b28→#3c4541`, outline 버튼은 보더·텍스트 green 전환. 큰 이동/확대 없음
- focus: 입력 보더 `#00FFA3`
- primary 버튼 hover: `#00FFA3→#33ffb5`
- 모든 상태 변경 버튼에 loading/disabled 상태 필요 (htmx `hx-indicator` 활용)
- 성공/실패는 toast + (검증 실패 시) 입력 근처 인라인 메시지 병행
- 위험 동작(폐기, 취소하고 종료)은 확인 절차 필요
- 색상만으로 의미 전달 금지 — 항상 배지/label/보조 문구 병행 (음수 잔액은 3중 표시)

## State Management (서버 렌더링 기준)
- htmx fragment 교체 대상: 보드 목록+페이지네이션, 히스토리 펼침 영역, 출석 대상자 목록, 룰렛 검증 배너, 명령어 편집 폼+미리보기, 토큰 테이블
- 출석체크: 사이클 상태(수집 중/종료), 선택 인원 수를 footer 버튼 라벨에 반영
- 룰렛: 확률 합계·꽝 포함 여부에 따라 배너 색/활성화 버튼 disabled 토글
- 명령어: 저장 전 충돌 검증 결과를 배너로 갱신

## Assets
- 외부 이미지 없음. 로고는 green 사각형 + "냥" 텍스트 플레이스홀더 — 실제 채널 로고/브랜드 에셋으로 교체 필요
- Google Fonts: Archivo (400/500/600/700)
- 오버레이 휠은 CSS conic-gradient — 실제 구현 시 항목 수에 따라 동적 세그먼트

## Files
- `냥냥봇 디자인.dc.html` — 전체 디자인 캔버스 (턴3 → 턴1 순, 최신이 위). 브라우저에서 직접 열어 확인
- 원본 스펙: 코드베이스의 `docs/spec/ux-ui-guidelines.md`, `web-ui.md`, `component-guidelines.md`, `overlay-design.md`, `wireframes.md`
