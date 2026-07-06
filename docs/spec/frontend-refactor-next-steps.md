# 프론트 리팩터링 다음 단계

- 문서 상태: Working Note
- 작성일: 2026-07-06
- 기준 스택: Spring Boot MVC, Thymeleaf, Bootstrap 5, htmx
- 관련 문서:
  - [UX/UI 가이드라인](ux-ui-guidelines.md)
  - [컴포넌트 가이드](component-guidelines.md)
  - [웹 UI 명세](web-ui.md)

## 1. 현재 완료 상태

이번 단계는 전체 프론트 전환 완료가 아니라, 기존 기능을 유지하면서 신규 구조의 기준선을 만든 상태다.

최종 목표:

- 화면 구조와 시각 디자인은 Bootstrap 5 컴포넌트와 utility를 기본으로 한다.
- 화면 조각과 데이터 바인딩은 Thymeleaf fragment로 구성한다.
- 목록 조회, 검색, 저장, 검증, 미리보기, 부분 갱신은 htmx가 담당한다.
- 기능별 페이지는 `Bootstrap UI + Thymeleaf fragment + htmx region`의 조합으로 만든다.
- 현재의 모던한 시각 톤은 유지하되, 프로젝트 전용 JS와 CSS는 최대한 작성하지 않는다.
- JS는 htmx로 표현하기 어려운 실시간 상태, 복잡한 선택 상태, 애니메이션에만 제한한다.
- CSS는 Bootstrap theme/token으로 해결할 수 없는 CHZZK 브랜드 보정과 도메인 특수 상태에만 제한한다.
- 별도 SPA, Storybook, `/ui-catalog`, `/dev` route 없이 실제 기능 화면이 곧 컴포넌트 검증 표면이 되게 한다.

완료된 내용:

- `index.html`의 큰 인라인 UI를 `components/*`와 `features/*` fragment 조합으로 분리했다.
- `templates/fragments/*`의 레거시 화면 조각을 제거하고 기능별 디렉터리로 옮겼다.
- 공용 컴포넌트 기준 파일을 만들었다.
  - `components/buttons.html`
  - `components/forms.html`
  - `components/tables.html`
  - `components/badges.html`
  - `components/feedback.html`
  - `components/navigation.html`
- 명령어 관리 화면은 htmx 기반 fragment 갱신으로 전환했다.
  - 목록 조회
  - 필터 조회
  - 편집 폼 로딩
  - 검증
  - 미리보기
  - 저장
  - 비활성화
- 명령어 관리의 기존 JS DOM 렌더링과 관련 CSS는 제거했다.
- Google 동기화는 `GET`에서 `POST + CSRF`로 변경했다.
- Storybook, `/ui-catalog`, `/dev` 경로는 두지 않는다.

## 2. 현재 한계

Bootstrap은 도입됐지만, 화면 전체가 Bootstrap 디자인으로 완전히 전환된 상태는 아니다.

현재 Bootstrap 사용 범위:

- `btn`
- `form-control`
- `form-select`
- `table table-dark`
- `pagination`
- `alert`
- `badge`
- `spinner-border`
- 일부 grid/spacing utility

아직 커스텀 CSS가 화면 인상을 지배하는 영역:

- `hero`
- `board`
- `ticker`
- `modal-panel`
- `attendance-*`
- `roulette-*`
- `karma-*`
- `history-*`

따라서 현재 상태는 `Bootstrap 적용 시작`에 가깝고, `Bootstrap 중심 디자인 전환 완료`는 아니다.

## 3. 남은 JavaScript 범위

`favorite-list.js`는 명령어 관리 코드가 제거되었지만 아직 크다.
남은 JS는 단순 클릭 처리보다 클라이언트 상태와 실시간 흐름이 많은 영역이다.

남은 주요 책임:

- 호감도 히스토리 행 확장과 캐시
- 호감도 수동 조정 모달, 선택 상태, 계산 미리보기
- 출석체크 시작/중지, 5초 폴링, 참여자 선택 상태
- 룰렛 테이블/항목 동적 렌더링
- 룰렛 확률 합계와 활성화 가능 여부 표시
- 룰렛 시뮬레이션
- OBS 오버레이 토큰 발급과 재송출
- Google 동기화 버튼 fetch

즉, JS가 남아 있는 이유는 명령어 관리 외 기능들이 아직 JSON API와 클라이언트 렌더링 중심이기 때문이다.

## 4. 다음 전환 순서

### 4.1 Bootstrap 디자인 정렬

우선순위:

1. `favorite-list.css`에서 Bootstrap과 중복되는 버튼, 폼, 테이블, alert, pagination 스타일을 제거한다.
2. `hero`, `board`, `ticker`를 Bootstrap layout과 utility 중심으로 재구성한다.
3. CHZZK 색상은 기능별 CSS가 아니라 공통 token 또는 Bootstrap theme 변수로 좁힌다.
4. 커스텀 클래스는 JS hook, htmx target, 복잡한 애니메이션이 필요한 곳에만 남긴다.

성공 기준:

- 주요 입력/버튼/테이블이 Bootstrap class만으로 기본 형태를 유지한다.
- `btn-strong`, `btn-ghost` 같은 자체 버튼 role을 다시 만들지 않는다.
- `favorite-list.css`는 layout 보정과 도메인 특수 상태만 담당한다.

### 4.2 호감도 목록 htmx 전환

가장 먼저 줄일 수 있는 JS/페이지 동작 영역이다.

추가할 서버 fragment 후보:

- `GET /favorite/fragments/list`
- `GET /favorite/fragments/board`
- `GET /favorite/fragments/history?userId=...`

전환 대상:

- 닉네임 검색
- 페이지네이션
- 히스토리 행 확장
- empty/loading/error state

성공 기준:

- 검색과 페이지 이동이 `#favorite-board-region`만 갱신한다.
- 히스토리는 서버 fragment로 렌더링한다.
- 기존 권한 조건은 유지한다.

### 4.3 Google 동기화 htmx 전환

현재는 `fetch + CSRF`로 동작한다.
다음 단계에서는 htmx로 바꿔 toast 또는 inline alert fragment를 반환하게 한다.

전환 대상:

- `#sync-button`
- `#toast`
- `#loading-spinner`

성공 기준:

- 동기화 버튼 클릭에 JS fetch가 필요 없다.
- 성공/실패 메시지는 `components/feedback` fragment로 표시한다.
- CSRF는 form hidden field 또는 htmx header 설정으로 처리한다.

### 4.4 호감도 조정 모달 전환

이 영역은 선택 상태와 계산 미리보기가 있어 단순 전환보다 단계적으로 진행한다.

1단계:

- 조정 항목 목록을 서버 fragment로 렌더링한다.
- 적용 결과를 row fragment 또는 alert fragment로 반환한다.

2단계:

- 선택 상태와 계산 미리보기를 htmx form submit 중심으로 바꿀지, 최소 JS로 남길지 결정한다.

성공 기준:

- 업보/호감도 조정 기능은 기존과 동일하게 동작한다.
- JS는 모달 열기/닫기와 최소 선택 상태만 담당한다.

### 4.5 출석체크 전환

출석체크는 실시간 채팅 참여자 수집과 폴링이 있으므로 htmx만으로 완전 대체하지 않는다.

전환 방향:

- start/stop/apply 결과는 fragment 응답으로 바꾼다.
- 참여자 목록은 polling 방식 유지 또는 htmx polling 중 하나를 선택한다.
- 선택 상태는 서버 세션/hidden input/최소 JS 중 하나로 결정한다.

성공 기준:

- 방송 중 선택 상태가 예측 가능하게 유지된다.
- 5초 폴링 중 화면이 흔들리지 않는다.
- 적용 실패가 inline alert로 표시된다.

### 4.6 룰렛 관리 전환

룰렛은 가장 마지막에 진행한다.
현재 JS 책임이 많고, 확률 검증/시뮬레이션/오버레이 재송출이 모두 얽혀 있다.

전환 순서:

1. 룰렛 테이블 목록 fragment
2. 룰렛 항목 목록 fragment
3. 확률 상태/활성화 가능 여부 fragment
4. 최근 실행 목록 pagination fragment
5. 시뮬레이션 결과 fragment
6. 오버레이 토큰/재송출 action fragment

성공 기준:

- 룰렛 테이블 선택이 명확한 region 갱신으로 처리된다.
- 확률 검증 결과가 서버 기준으로 표시된다.
- 시뮬레이션과 실제 저장/활성화 action이 UI에서 분리된다.

## 5. 작업 원칙

- 기존 기능은 제거하지 않는다. 디자인과 구현 방식만 전환한다.
- 기능 페이지는 데이터와 레이아웃만 조합하고, 반복 UI는 공용 컴포넌트를 사용한다.
- 새 route에는 `/dev`, `/ui-catalog`를 만들지 않는다.
- htmx target은 항상 좁은 region으로 잡는다.
- Bootstrap 기본 컴포넌트를 먼저 사용하고, 커스텀 CSS는 마지막 수단으로 둔다.
- JS 제거는 기능별 htmx/server fragment 대체가 끝난 뒤 진행한다.

## 6. 다음 작업 추천

바로 다음 커밋 후보:

1. `favorite-list.css`에서 Bootstrap 중복 스타일을 더 줄인다.
2. `favorite-tab`을 `favorite-board-region`으로 나누고 검색/페이지네이션을 htmx화한다.
3. Google 동기화 버튼을 htmx POST로 바꾸고 inline feedback fragment를 반환한다.

이 세 가지가 끝나면 사용자가 체감하는 Bootstrap 전환도 더 명확해지고, JS도 추가로 줄어든다.
