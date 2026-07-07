# UX/UI 가이드라인

- 문서 상태: Draft v1
- 작성일: 2026-07-06
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 관련 문서:
  - [웹 UI 명세](web-ui.md)
  - [컴포넌트 가이드](component-guidelines.md)
  - [디자인 와이어프레임](wireframes.md)
  - [OBS 오버레이 디자인 명세](overlay-design.md)
  - [관리자 명령어 관리 PRD](command-management.md)
- 외부 기준: [CHZZK Brand Guides](https://chzzk.gitbook.io/chzzk/resources/brand-guides)

## 1. 목적

이 문서는 Nyang-Nyang Bot 웹 UI와 OBS 오버레이의 UX/UI 판단 기준을 정의한다.
상세 화면 구조는 [웹 UI 명세](web-ui.md)를 따르고, 이 문서는 신규 화면 추가와 프론트 리팩터링 시 일관성을 판단하는 기준으로 사용한다.

우선 목표는 다음과 같다.

- 운영자가 방송 중 반복 작업을 빠르게 처리할 수 있게 한다.
- 일반 사용자가 자신의 상태와 변경 내역을 쉽게 이해하게 한다.
- 관리자 전용 액션, 위험 액션, 조회 액션을 명확히 구분한다.
- Bootstrap 5, Thymeleaf fragment, htmx를 기준으로 레거시 CSS/JS를 제거한 뒤에도 화면 톤과 상호작용 규칙이 흔들리지 않게 한다.

## 2. 제품 톤

Nyang-Nyang Bot 웹 UI는 `방송 커뮤니티 운영 도구`다.
귀엽거나 장식적인 페이지보다, 운영자가 오래 켜두고 반복적으로 조회/수정하기 편한 조용한 대시보드 톤을 우선한다.

기본 방향:

- CHZZK Black 기반 어두운 운영 화면.
- 높은 정보 밀도.
- 낮은 장식성.
- 빠른 스캔과 검증.
- 적용 전 미리보기.
- 성공/실패의 즉시 피드백.

피해야 할 방향:

- 마케팅 랜딩 페이지 같은 hero 중심 구성.
- 의미 없는 카드 나열.
- 기능마다 다른 색상 체계.
- 과한 glow, blob, orb, bokeh 장식.
- 버튼과 badge가 모두 비슷하게 보이는 화면.

## 3. 사용자별 UX 우선순위

| 사용자 | 우선순위 | 화면 원칙 |
| --- | --- | --- |
| 방문자 | 로그인 진입 | 설명보다 로그인 동선을 먼저 보여준다. |
| 인증 사용자 | 본인 상태 이해 | 내 호감도, 히스토리, 보유/변경 내역을 빠르게 찾게 한다. |
| 관리자 | 반복 작업 처리 | 검색, 선택, 검증, 미리보기, 적용을 한 흐름 안에서 끝낸다. |
| 방송 시청자 | 오버레이 가독성 | 결과를 짧고 크게 보여주고, 방송 화면을 오래 가리지 않는다. |

관리자 화면은 예쁘게 보이는 것보다 실수 방지와 작업 속도가 중요하다.
일반 사용자 화면은 운영 복잡도를 줄이고, 본인에게 필요한 정보만 먼저 보여준다.

## 4. 정보 구조

웹 UI의 기본 구조는 다음 순서를 따른다.

1. 현재 위치와 사용자 맥락.
2. 필터 또는 검색.
3. 목록 또는 작업 대상.
4. 편집 폼.
5. 검증 결과 또는 미리보기.
6. 저장/적용 액션.

관리자 기능은 탭 기반으로 묶되, 각 탭은 하나의 운영 작업에 집중한다.

- 호감도 목록: 조회, 검색, 대상 선택.
- 출석체크: 참여자 수집, 선택, 일괄 적용.
- 룰렛 관리: 테이블, 항목, 활성화, 실행 기록.
- 명령어 관리: 목록, 편집, 검증, 미리보기, 비활성화.

새 관리자 기능을 추가할 때 기존 탭 안에 억지로 넣지 않는다.
사용자의 작업 목표가 다르면 새 탭 또는 별도 화면 후보로 분리한다.

## 5. 시각 시스템

### 5.1 Bootstrap/htmx 우선 원칙

신규 전환 기준은 Spring Boot MVC, Thymeleaf, Bootstrap 5.x, htmx 2.x 조합이다.
Bootstrap 5 bundle과 htmx runtime 외의 큰 전역 JS, 커스텀 탭/모달/목록 구현은 다시 도입하지 않는다.
컴포넌트별 상세 기준은 [컴포넌트 가이드](component-guidelines.md)를 따른다.

새 UI를 만들 때 우선순위는 다음과 같다.

1. Bootstrap layout, grid, form, button, table, alert, badge, utility class로 표현한다.
2. 서버가 Thymeleaf fragment를 반환하고 htmx가 필요한 영역만 교체한다.
3. 페이지 템플릿은 `components/*`와 `features/*/components.html` fragment 조합만 담당한다.
4. 웹 관리자 화면의 기존 기능은 삭제하지 않고, 기능별 CSS/JavaScript는 동등 동작을 가진 컴포넌트/fragment로 대체된 뒤 제거한다.
5. OBS 오버레이처럼 실시간 소켓, 애니메이션, 방송 송출 타이밍이 핵심인 영역만 예외로 둔다.

권장 사용:

| 영역 | Bootstrap 우선 사용 |
| --- | --- |
| 레이아웃 | `container`, `row`, `col-*`, `d-flex`, spacing utility |
| 폼 | `form-label`, `form-control`, `form-select`, `form-check`, validation state |
| 버튼 | `btn` 기반 variant |
| 탭/네비게이션 | `nav`, `nav-pills`, anchor navigation |
| 모달 | 신규 관리자 화면에서는 우선 inline region, 필요 시 Bootstrap modal 구조 |
| 상태 표시 | `badge`, `alert`, `text-*`, `border-*` utility |
| 부분 갱신 | `hx-get`, `hx-post`, `hx-target`, `hx-swap`, `hx-indicator` |

Bootstrap class가 구조와 시각 상태를 모두 담당한다.
정확한 CHZZK Neon Green 적용이 필요해지면 기능별 CSS가 아니라 Bootstrap theme build 또는 전역 Bootstrap 변수 조정으로만 처리한다.

### 5.2 브랜드 컬러 기준

색상 기준은 CHZZK 공식 브랜드 가이드를 따른다.
2026-07-06 확인 기준 핵심 브랜드 컬러는 아래와 같다.

| 역할 | 이름 | 값 |
| --- | --- | --- |
| Brand primary | 치지직 Neon Green | `#00FFA3` |
| Brand black | 치지직 Black | `#000000` |

적용 원칙:

- `#00FFA3`는 primary action, 선택 상태, 활성 상태, 핵심 하이라이트에 사용한다.
- `#000000`는 최상위 배경과 OBS 오버레이의 표시 중인 패널/그림자/텍스트 처리 기준 black으로 사용한다.
- 웹 관리자 화면에서는 Bootstrap `success`, `dark`, `danger`, `warning` 계열을 역할에 맞춰 사용하고, 정확한 브랜드 색상 고정은 전역 Bootstrap theme build에서만 처리한다.
- 브랜드 로고나 아이콘을 사용할 때는 형태와 색상을 임의로 변형하지 않는다.
- 경고, 실패, 성공 같은 기능 상태 색상은 브랜드 컬러와 구분되는 보조 semantic color로 유지한다.

### 5.3 Bootstrap 우선 원칙

새 웹 UI는 별도 CSS token 파일을 만들지 않고 Bootstrap 5 클래스만 사용한다.
색상은 CHZZK Black과 Neon Green 방향에 맞춰 `bg-black`, `bg-dark`, `bg-gradient`, `text-light`, `text-success`, `btn-success`, `btn-outline-success`, `border-secondary-subtle`을 우선 사용한다.
배경은 별도 CSS 없이 Bootstrap `bg-dark bg-gradient`로 깊이를 주고, 정확한 브랜드 그라데이션이 필요해지면 전역 Bootstrap theme build에서 처리한다.
Bootstrap 기본 색상과 공식 브랜드 색상의 차이를 줄여야 할 때는 전역 Bootstrap theme build를 도입하고, 화면별 CSS는 만들지 않는다.

### 5.4 색상 규칙

- CHZZK Neon Green 계열은 일반 primary 상태와 선택 상태에 사용한다.
- 관리자 권한 액션은 별도 색상 role을 만들지 않고 위치, label, icon, permission badge로 구분한다.
- 붉은 계열은 실패, 위험, 음수, 삭제성 행동에만 사용한다.
- 초록 계열은 성공 상태에 제한적으로 사용한다.
- 회색 계열은 조회, 보조, disabled 상태에 사용한다.

색상만으로 의미를 전달하지 않는다.
항상 상태 텍스트, 아이콘, label, helper text 중 하나를 함께 둔다.

### 5.5 타이포그래피

- 기본 폰트는 `--font-sans`를 사용한다.
- 운영 화면 본문은 `--fs-14` 또는 `--fs-15`를 기본으로 한다.
- 보조 정보와 label은 `--fs-12` 또는 `--fs-13`을 사용한다.
- 화면 제목은 운영 화면에서 과하게 키우지 않는다.
- 모달, 테이블, 툴바 내부에는 hero 크기 텍스트를 쓰지 않는다.
- 긴 닉네임, 명령어, 설명, 결과 라벨은 ellipsis 또는 줄바꿈 규칙을 둔다.

### 5.6 표면과 radius

- 기본 입력/행: `--radius-sm`.
- 섹션/탭/작업 패널: `--radius-md` 또는 `--radius-lg`.
- 모달/상단 패널: `--radius-lg`.
- pill은 badge, 작은 상태 chip에만 사용한다.

카드 안에 카드를 중첩하지 않는다.
구분이 필요하면 border, divider, spacing, section title을 사용한다.

## 6. 레이아웃 원칙

### 6.1 데스크톱

- 기본 page width는 현재 `1080px` 흐름을 유지한다.
- 복잡한 관리자 작업은 최대 `1280px`까지 확장할 수 있다.
- 주요 작업 화면은 2-column을 우선한다.
  - 왼쪽: 목록/대상 선택.
  - 오른쪽: 편집/미리보기/상태.
- 검색/필터는 목록보다 시각적으로 작게 둔다.
- 페이지 전체를 floating card처럼 만들지 않는다.

### 6.2 모바일

- 최소 360px 폭에서 텍스트가 버튼이나 패널 밖으로 넘치지 않아야 한다.
- 2-column 관리자 화면은 1-column으로 전환한다.
- 목록 header가 좁은 화면에서 의미 없으면 숨기고, 행 안에 label을 남긴다.
- 모달은 viewport 높이를 넘을 수 있으므로 내부 scroll 영역을 둔다.
- 가로 스크롤과 카드형 행 중 하나만 선택한다.

### 6.3 고정 포맷 UI

보드, 목록 행, 툴바, 폼 grid, 상태 chip은 내용 변화로 레이아웃이 흔들리지 않아야 한다.

- grid는 `minmax(0, ...)`를 사용한다.
- 숫자 열은 우측 정렬한다.
- 긴 텍스트는 ellipsis 또는 `word-break`를 명시한다.
- 버튼 줄은 wrap 가능하게 만들되, 주요 액션의 위치는 유지한다.

## 7. 컴포넌트 규칙

### 7.1 버튼

버튼 역할은 아래 범위로 제한한다.

| 역할 | 용도 |
| --- | --- |
| Primary | 저장, 적용, 발급, 활성화 |
| Secondary | 취소, 닫기, 낮은 우선순위 실행 |
| Ghost | 새로고침, 검증, 미리보기, 보조 액션 |
| Danger | 삭제, 폐기, 위험한 비활성화 |
| Icon | 닫기, 복사, 펼침, 재송출, 새로고침 |

규칙:

- 버튼은 Bootstrap `btn` 구조를 기본으로 사용한다.
- 관리자 전용 실행은 별도 role을 만들지 않고 `primary` 또는 `secondary`에 `auth/admin` modifier를 더해 구분한다.
- 같은 줄의 버튼은 높이와 radius를 맞춘다.
- 주요 액션은 오른쪽 끝 또는 모달 하단 오른쪽에 둔다.
- 위험 액션은 확인 절차를 둔다.
- disabled, hover, focus-visible 상태를 정의한다.
- 아이콘만 쓰는 버튼은 tooltip 또는 `aria-label`을 둔다.

### 7.2 입력과 폼

- 폼은 Bootstrap `form-label`, `form-control`, `form-select`, `form-check` 구조를 우선 사용한다.
- 모든 입력은 label을 가진다.
- placeholder는 예시일 뿐 label을 대체하지 않는다.
- 숫자 입력은 단위, 최소값, 최대값을 명확히 한다.
- 저장 전 검증이 가능한 화면은 검증 버튼이나 실시간 검증 결과를 둔다.
- 오류는 입력 근처 또는 결과 영역에 표시하고, toast만으로 끝내지 않는다.
- 관리자 메모와 공개 설명은 시각적으로 구분한다.

### 7.3 목록과 테이블

- 운영 목록은 장식 카드보다 조밀한 행 레이아웃을 우선한다.
- 행 hover는 약한 border 또는 배경 변화만 사용한다.
- 숫자, 날짜, 상태는 항상 같은 열 위치에 둔다.
- 선택된 행은 accent border/background로 구분한다.
- 비활성 행은 색상 대비를 낮추되 읽을 수 있어야 한다.
- 빈 목록은 빈 상태 문구와 다음 액션을 제공한다.

### 7.4 모달

모달 구조는 다음 순서를 따른다.

1. Header.
2. 대상/맥락.
3. 입력 또는 목록.
4. 미리보기/검증/상태.
5. Actions.

규칙:

- Bootstrap modal 구조와 focus/close 동작을 우선 사용한다.
- 모달 안에 큰 카드 패널을 중첩하지 않는다.
- 하단 액션은 항상 같은 위치에 둔다.
- 닫기는 `Esc`, backdrop click, close button 중 최소 close button을 제공한다.
- 중요한 변경은 적용 전 결과를 보여준다.

### 7.5 Toast와 상태 피드백

- 성공/실패 결과는 즉시 toast로 알린다.
- 검증 실패는 toast보다 화면 내 오류 영역을 우선한다.
- 저장 중에는 버튼 disabled 또는 loading 상태를 표시한다.
- API 실패는 사용자가 다시 시도할 수 있는 위치에 표시한다.

## 8. UX writing

문구는 짧고 작업 중심으로 쓴다.

버튼:

- 좋은 예: `저장`, `검증`, `미리보기`, `비활성화`, `재송출`
- 피할 예: `확인하기`, `이 기능 사용`, `설정을 진행합니다`

오류:

- 원인과 조치를 함께 쓴다.
- backend raw message를 그대로 노출하지 않는다.
- 개발자 용어보다 운영자가 이해할 수 있는 단어를 쓴다.

상태:

- `활성`, `비활성`, `저장 가능`, `확률 합계 부족`, `권한 없음`처럼 짧게 쓴다.
- 색상에 기대지 않고 텍스트로도 상태를 설명한다.

## 9. 접근성과 상호작용

- 모든 클릭 가능한 요소는 keyboard focus가 보여야 한다.
- icon button은 `aria-label` 또는 tooltip을 가진다.
- 색상 대비는 어두운 배경에서도 읽을 수 있어야 한다.
- form control은 label과 연결되어야 한다.
- 동작이 오래 걸리는 버튼은 중복 클릭을 막는다.
- 애니메이션은 정보 전달을 돕는 경우에만 사용한다.
- hover에만 의존하는 정보는 만들지 않는다.

## 10. OBS 오버레이 예외

OBS 오버레이는 웹 관리자 화면보다 강한 대비와 큰 글자를 허용한다.
다만 색상 계열, 폰트 계열, 상태 의미는 웹 UI와 맞춘다.
오버레이는 OBS 브라우저 소스에서 독립적으로 보이므로 Bootstrap 의존을 강제하지 않는다.

- 대기 상태는 완전 투명.
- 결과 표시 시간은 짧게 유지.
- 방송 화면 중앙 안전 영역을 침범하지 않는다.
- 웹 관리 화면의 배경 그리드와 page 패턴을 가져오지 않는다.
- 오류는 기본적으로 방송 화면에 크게 노출하지 않는다.

## 11. 프론트 리팩터링 기준

리팩터링은 기능 변경 없이 구조를 명확히 하는 순서로 진행한다.

### 11.1 레거시 CSS 제거 기준

현재 웹 관리자 화면의 목표 상태는 기능별 CSS 파일을 두지 않는 것이다.
화면 전용 CSS는 Bootstrap 5 클래스와 Thymeleaf fragment로 대체하고 제거한다.

허용 범위:

- Bootstrap CDN 또는 Bootstrap theme build.
- OBS 오버레이 전용 CSS.
- 외부 위젯이 강제하는 최소 스타일.

금지 범위:

- 기능별 버튼, 탭, 목록, 모달 CSS 재구현.
- 화면별 임의 색상, radius, spacing class.
- Bootstrap 컴포넌트와 같은 역할의 커스텀 UI class.

### 11.2 레거시 JavaScript 제거 기준

현재 웹 관리자 화면의 목표 상태는 UI용 전역 JavaScript를 두지 않는 것이다.
기존 화면 전용 JavaScript의 역할은 서버 fragment endpoint와 htmx attribute로 이전했으며, 새 기능도 같은 기준을 따른다.

전환 규칙:

- JSON 응답을 받아 HTML 문자열을 조립하지 않는다.
- 클릭/탭/필터/저장/검증/미리보기 동작은 `hx-*` attribute로 표현한다.
- 서버는 page route와 fragment/action route의 반환 범위를 명확히 한다.
- 복수 region 갱신은 `hx-swap-oob` 또는 `HX-Trigger`로 처리한다.
- CSRF는 form hidden input으로 포함한다.

허용 범위:

- htmx runtime.
- Bootstrap component bundle.

### 11.3 템플릿 분리 기준

- 하나의 fragment는 하나의 사용자 작업에 집중한다.
- fragment의 id prefix는 feature prefix와 맞춘다.
  - `attendance-*`
  - `roulette-*`
  - `command-*`
  - `karma-*`
- fragment 내부에서 다른 feature의 id/class를 참조하지 않는다.
- 반복되는 구조는 class를 공유하되 id는 feature별로 분리한다.

## 12. 구현 체크리스트

새 UI 또는 리팩터링은 아래 기준을 통과해야 한다.

- Bootstrap layout/component/utility를 먼저 사용했다.
- htmx target과 swap 범위가 컴포넌트 단위로 제한되어 있다.
- primary/black 기준 색상이 CHZZK Brand Guides와 맞는다.
- 별도 웹 UI CSS/JS 없이 Bootstrap과 htmx로 표현했다.
- desktop 1280px, mobile 360px에서 텍스트 겹침이 없다.
- 버튼 역할과 색상이 일관된다.
- 관리자 전용 액션이 일반 액션과 구분된다.
- 위험 액션에 확인 절차가 있다.
- 입력 오류가 화면 안에서 확인된다.
- 빈 상태, 실패 상태, loading/disabled 상태가 있다.
- keyboard focus가 보인다.
- 필요한 경우 Thymeleaf 렌더링 테스트가 있다.
- `gradlew test` 또는 변경 범위에 맞는 테스트가 통과한다.
