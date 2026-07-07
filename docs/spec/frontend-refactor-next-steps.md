# 프론트 리팩터링 다음 단계

- 문서 상태: Working Note
- 작성일: 2026-07-06
- 갱신일: 2026-07-07
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
- 명령어 관리 컨트롤러는 `/admin/commands` 기준의 `CommandController`로 정리했고, `Fragment` 접미사와 병행 JSON 컨트롤러는 제거했다.
- 명령어 관리의 기존 JS DOM 렌더링과 관련 CSS는 제거했다.
- 호감도 목록 검색/페이지네이션은 `/favorite/list`의 `HX-Request`에서 board fragment를 반환하도록 전환했다.
- 호감도 히스토리 확장은 `/favorite/history`가 history-grid fragment를 반환하도록 전환했다.
- 호감도 조정은 항목 목록과 적용 결과를 htmx fragment로 전환했다.
- 출석체크는 시작/중지/참여자 목록/적용 결과를 htmx fragment로 전환했다.
- Google 동기화는 `/google/sync` `POST + CSRF`에서 inline feedback fragment를 반환하도록 전환했다.
- 관리자 룰렛은 테이블 목록, 상세, 항목 추가, 활성화/비활성화, 시뮬레이션, 최근 실행 목록을 htmx fragment로 전환했다.
- 관리자 오버레이 토큰 발급/재송출은 htmx fragment로 전환했다.
- Bootstrap JS bundle을 로드하고, 관리자 탭은 Bootstrap Tabs, 업보 조정 창은 Bootstrap Modal, toast는 Bootstrap Toast, 히스토리 펼침은 Bootstrap Collapse로 전환했다.
- 화면 구성만을 위해 있던 JSON 전용 웹 어댑터는 제거했다.
  - 명령어 관리 JSON DTO/API
  - 호감도 조정 적용 JSON 응답
  - 출석체크 JSON 요청/응답
  - 관리자 룰렛 JSON 요청/응답
  - 관리자 오버레이 토큰 JSON 응답
  - 미사용 업보/사용자 룰렛 JSON 웹 API
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
- `nav nav-pills` / `tab-pane`
- `modal`
- `toast`
- `collapse`

커스텀 CSS 파일은 제거했고, 화면의 기본 형태는 Bootstrap 컴포넌트와 utility class로 유지한다.
남은 클래스명은 htmx target, 테스트 식별자, 서버 fragment 경계에 필요한 id/class hook으로 제한한다.

따라서 현재 상태는 `Bootstrap + htmx + Thymeleaf fragment` 중심 전환 완료를 기준으로 관리한다.

## 3. 남은 JavaScript 범위

커스텀 JS 파일은 제거했고, 화면 갱신은 htmx 요청과 서버 fragment 응답으로 표현한다.
Bootstrap bundle과 htmx 라이브러리만 런타임 의존성으로 둔다.

이전 JS 책임의 대체 방식:

- 호감도 수동 조정: lazy modal fragment와 서버 적용 feedback
- 출석체크 시작/중지, 5초 폴링, 참여자 선택 상태: htmx trigger, form field, 서버 fragment
- 룰렛 항목 form 검증: HTML form validation과 서버 검증 feedback
- OBS 오버레이 polling: htmx polling fragment

즉, 별도 커스텀 JS 유지 사유는 현재 없다.

## 4. 다음 전환 순서

### 4.1 API와 htmx 전환 원칙

화면 조작을 위해 JS가 JSON을 받아 DOM을 직접 조립하는 REST API는 htmx용 Thymeleaf fragment 응답으로 대체한다.
기존 경로를 두고 `/fragments/**` 병행 endpoint를 새로 만드는 방식은 기본값으로 두지 않는다.
같은 경로라도 일반 브라우저 진입은 full page를 반환하고, `HX-Request`는 교체 대상 fragment를 반환할 수 있다.

기본 분류:

| 구분 | 응답 형식 | 사용처 | 방향 |
| --- | --- | --- | --- |
| Page | Thymeleaf full page | 최초 화면 진입, 새로고침, 인증 후 landing | 유지 |
| Fragment | Thymeleaf fragment HTML | 검색, 페이지네이션, 저장, 검증, 미리보기, inline feedback, OBS 오버레이 polling | 기존 UI REST 경로를 대체 |
| Tool/External JSON | JSON | OpenAPI, Actuator, 외부 provider 원본 payload | 화면 route와 분리 |

권장 route 구조:

```text
/favorite/list                  full page, HX request -> favorite-board-region
/favorite/history               htmx history fragment

/google/sync                    htmx feedback fragment

/attendance/**                  화면 조작 경로는 htmx fragment/action response로 대체
/admin/roulette/**              화면 조작 경로는 htmx fragment/action response로 대체
/admin/overlay/roulette/**      관리자 화면 조작 경로는 htmx fragment/action response로 대체

/admin/commands                 htmx command list/save fragment
/admin/commands/editor          htmx command editor fragment
/admin/commands/validate        htmx command validation fragment
/admin/commands/preview         htmx command preview fragment
/admin/commands/deactivate      htmx command deactivate action
```

전환 판단 기준:

- 사용자가 버튼, 폼, 검색, 페이지네이션으로 직접 조작하는 화면 영역이면 htmx fragment 후보로 본다.
- JS가 `fetch()`로 JSON을 받은 뒤 `innerHTML`, `appendChild`, 문자열 템플릿으로 DOM을 조립한다면 우선 전환 대상이다.
- htmx fragment는 교체될 region root를 포함해 반환한다.
- 화면 조작용 기존 REST API는 UI 전환 시 fragment 응답으로 바꾼다.
- JSON 유지 예외는 OpenAPI, Actuator, 외부 provider 원본 payload처럼 화면 fragment 소비자가 아닌 경우에만 둔다.
- 같은 기능을 page endpoint와 fragment endpoint가 공유할 때는 use case와 model builder를 공유하고, controller 반환 형식만 나눈다.

전환 대상은 JSON 경로 자체가 아니라, JS가 데이터를 받아 화면을 조립하던 UI 흐름이다.
구현 시에는 기존 UI 경로를 fragment 응답으로 대체하고, 별도 병행 REST wrapper를 만들지 않는다.

| 현재 UI 흐름 | 현재 의존 경로 | htmx 전환 방향 |
| --- | --- | --- |
| 호감도 검색/페이지네이션 | `/favorite/list` | full page는 유지하고 htmx 요청은 `favorite-board-region` fragment로 응답 |
| 호감도 히스토리 확장 | `/favorite/history` | history-grid fragment 응답으로 대체 |
| 호감도 조정 항목 로딩 | `/favorite/adjustments` | 조정 form/preview fragment 응답으로 대체 |
| 호감도 조정 적용 | `/favorite/adjustments/apply` | row/feedback fragment 응답으로 대체 |
| 출석체크 시작 | `/attendance/start` | session 상태 fragment 응답으로 대체 |
| 출석체크 중지 | `/attendance/stop` | session 상태 fragment 응답으로 대체 |
| 출석체크 참여자 목록 | `/attendance/users` | users fragment 응답으로 대체, polling 방식은 별도 판단 |
| 출석체크 적용 | `/attendance/apply` | apply feedback fragment 응답으로 대체 |
| Google 동기화 버튼 | `/google/sync` | feedback fragment 응답으로 대체 |
| 룰렛 테이블 목록 | `/admin/roulette/tables` | selected table을 포함한 config region fragment 응답 |
| 룰렛 테이블 상세 | `/admin/roulette/tables/{tableId}/detail` | selected table을 포함한 config region fragment 응답 |
| 룰렛 최근 실행 목록 | `/admin/roulette/events` | events fragment 응답으로 대체 |
| 룰렛 항목 추가 | `/admin/roulette/items` | selected table을 포함한 config region fragment 응답 |
| 룰렛 검증 표시 | `/admin/roulette/tables/{tableId}/detail` | config region의 selected table detail 안에 포함 |
| 룰렛 활성화 | `/admin/roulette/tables/{tableId}/activate` | config region fragment 응답으로 대체 |
| 룰렛 비활성화 | `/admin/roulette/tables/{tableId}/deactivate` | config region fragment 응답으로 대체 |
| 룰렛 시뮬레이션 | `/admin/roulette/tables/{tableId}/simulation` | simulation result fragment 응답으로 대체 |
| 오버레이 토큰 발급 UI | `/admin/overlay/roulette/token` | 관리자 UI URL field fragment 응답으로 대체 |
| 오버레이 재송출 UI | `/admin/overlay/roulette/events/{rouletteEventId}/replay` | 관리자 UI feedback fragment 응답으로 대체 |

JSON 유지 예외:

- 외부 CHZZK OpenAPI 연동 payload
- OpenAPI와 Actuator
- 전역 예외 처리 응답

성공 기준:

- 기능 화면에서 JSON 응답을 직접 받아 DOM을 조립하는 JS가 줄어든다.
- 화면 갱신은 `hx-get`, `hx-post`, `hx-target`, `hx-swap`, `hx-include`로 표현한다.
- 실패와 빈 상태는 JSON alert 조립이 아니라 `components/feedback` fragment로 표시한다.
- 중복 REST wrapper와 fragment wrapper를 동시에 남기지 않는다.

### 4.2 Bootstrap 디자인 정렬

우선순위:

1. Bootstrap 기본 버튼, 폼, 테이블, alert, pagination을 우선 사용한다.
2. 화면 전용 스타일 파일을 다시 만들지 않는다.
3. CHZZK 색상은 필요한 경우 Bootstrap theme 변수 또는 제한된 inline 서버 렌더링 값으로만 다룬다.
4. 커스텀 클래스는 htmx target, 테스트 식별자, fragment 경계처럼 구조상 필요한 곳에만 남긴다.

성공 기준:

- 주요 입력/버튼/테이블이 Bootstrap class만으로 기본 형태를 유지한다.
- `btn-strong`, `btn-ghost` 같은 자체 버튼 role을 다시 만들지 않는다.
- `/static/css`와 `/static/js` 아래 화면 전용 파일을 다시 추가하지 않는다.

### 4.3 호감도 목록 htmx 전환

이번 단계에서 가장 먼저 줄인 JS/페이지 동작 영역이다.

적용 경로:

- `GET /favorite/list` full page, HX request는 `favorite-board-region`
- `GET /favorite/history?userId=...` history-grid fragment

전환 대상:

- 닉네임 검색
- 페이지네이션
- 히스토리 행 확장
- empty/loading/error state

성공 기준:

- 검색과 페이지 이동이 `#favorite-board-region`만 갱신한다.
- 히스토리는 서버 fragment로 렌더링한다.
- 기존 권한 조건은 유지한다.

### 4.4 Google 동기화 htmx 전환

이번 단계에서 `fetch + CSRF`를 htmx `POST`로 대체했다.
동기화 결과는 toast 조립이 아니라 inline alert fragment로 표시한다.

전환 대상:

- `#sync-button`
- `#sync-feedback`

성공 기준:

- 동기화 버튼 클릭에 JS fetch가 필요 없다.
- 성공/실패 메시지는 `components/feedback` fragment로 표시한다.
- CSRF는 form hidden field 또는 htmx header 설정으로 처리한다.

### 4.5 호감도 조정 모달 전환

이 영역은 HTML 조립, 선택, 적용, feedback을 모두 서버 fragment와 htmx form submit으로 옮겼다.

적용 상태:

- 조정 항목 목록은 `/favorite/adjustments` fragment가 렌더링한다.
- 모달 본문은 `/favorite/adjustments/modal` fragment가 렌더링한다.
- 적용 요청은 `/favorite/adjustments/apply` htmx form submit으로 처리한다.
- 적용 결과는 `components/feedback` alert fragment로 표시한다.
- 모달 열기/닫기는 Bootstrap Modal이 처리하고, 대상 사용자 값은 서버 fragment model로 채운다.
- 성공 시 `favorite-board-refresh` htmx trigger로 목록을 갱신한다.

성공 기준:

- 업보/호감도 조정 기능은 기존과 동일하게 동작한다.
- 커스텀 JS 없이 Bootstrap Modal과 htmx form submit만 사용한다.

### 4.6 출석체크 전환

출석체크는 실시간 채팅 참여자 수집과 polling을 htmx fragment로 처리한다.

적용 상태:

- start/stop/apply 결과는 fragment 응답으로 바꿨다.
- 참여자 목록은 `/attendance/users` fragment를 5초마다 htmx로 갱신한다.
- 사용자가 선택 해제한 참여자는 `userIds`, `knownUserIds`, `selectionInitialized` form field를 서버로 보내 polling swap 이후에도 다시 체크되지 않게 했다.

성공 기준:

- 방송 중 선택 상태가 예측 가능하게 유지된다.
- 5초 폴링 중 화면이 흔들리지 않는다.
- 적용 실패가 inline alert로 표시된다.

### 4.7 룰렛 관리 전환

룰렛 관리는 JSON 응답과 JS DOM 조립을 제거하고, 서버 fragment와 htmx action으로 전환했다.

적용 상태:

- 룰렛 테이블 목록, 선택 테이블 상세, 항목 form은 `roulette-config-region` 안에서 함께 렌더링한다.
- `/admin/roulette/tables`와 `/admin/roulette/tables/{tableId}/detail`은 선택 상태를 포함한 config region fragment를 반환한다.
- 항목 추가는 `/admin/roulette/items` form submit 후 config region을 교체한다.
- 활성화/비활성화도 config region을 교체하므로 별도 refresh trigger와 hidden form 값 동기화 JS가 필요 없다.
- 최근 실행 목록과 pagination은 `/admin/roulette/events` fragment가 렌더링한다.
- 시뮬레이션 결과는 `/admin/roulette/tables/{tableId}/simulation` fragment로 표시한다.
- 관리자 오버레이 토큰/재송출은 htmx fragment 응답으로 처리한다.

성공 기준:

- 룰렛 테이블 선택이 명확한 region 갱신으로 처리된다.
- 확률 검증 결과가 서버 기준으로 표시된다.
- 시뮬레이션과 실제 저장/활성화 action이 UI에서 분리된다.

## 5. 작업 원칙

- 기존 기능은 제거하지 않는다. 디자인과 구현 방식만 전환한다.
- 기능 페이지는 데이터와 레이아웃만 조합하고, 반복 UI는 공용 컴포넌트를 사용한다.
- 새 route에는 `/dev`, `/ui-catalog`를 만들지 않는다.
- htmx target은 항상 좁은 region으로 잡는다.
- Bootstrap 기본 컴포넌트를 먼저 사용하고, 화면 전용 커스텀 CSS는 추가하지 않는다.
- 새 상호작용은 커스텀 JS가 아니라 htmx/server fragment로 먼저 표현한다.

## 6. 다음 작업 추천

바로 다음 검증 후보:

1. 실제 브라우저에서 관리자 탭 전환, 출석 polling, 룰렛 관리, 호감도 모달, OBS 오버레이 polling을 한 번씩 확인한다.
2. htmx fragment 응답이 같은 endpoint를 즉시 재호출하지 않는지 네트워크 탭에서 확인한다.
3. 새 화면 작업은 `/static/js`, `/static/css`를 추가하지 않는 것을 체크리스트에 넣는다.

이 세 가지가 끝나면 구현 전환뿐 아니라 런타임 화면 동작까지 전환 완료로 볼 수 있다.
