# 컴포넌트 가이드

- 문서 상태: Draft v1
- 작성일: 2026-07-06
- 상위 문서: [UX/UI 가이드라인](ux-ui-guidelines.md)
- 관련 문서:
  - [웹 UI 명세](web-ui.md)
  - [관리자 명령어 관리 PRD](command-management.md)
  - [OBS 오버레이 디자인 명세](overlay-design.md)
- 외부 기준:
  - [CHZZK Brand Guides](https://chzzk.gitbook.io/chzzk/resources/brand-guides)
  - [htmx Documentation](https://htmx.org/docs/)

## 1. 목적

이 문서는 Nyang-Nyang Bot 웹 UI의 컴포넌트 단위 UX/UI 기준을 정의한다.
기존 화면 전용 CSS/JavaScript와 커스텀 탭/모달/목록 구현은 보존하지 않는다.

목표 스택:

- Spring Boot MVC
- Thymeleaf page와 fragment
- Bootstrap 5.x layout/component/utility
- htmx 2.x partial update
- CHZZK 브랜드 컬러 방향의 Bootstrap 5 색상 사용

기본 방향:

- 서버가 화면 상태를 소유하고 Thymeleaf fragment를 반환한다.
- htmx는 fragment 요청, 교체, loading, disabled, cross-component event만 담당한다.
- Bootstrap은 레이아웃, 폼, 버튼, 모달, 탭, 유틸리티의 기본 구조를 담당한다.
- 웹 관리자 UI에는 프로젝트 전용 CSS와 기능별 JavaScript를 두지 않는다.
- UI용 CRUD는 htmx와 서버 fragment로 처리한다.

## 2. 신규 전환 원칙

### 2.1 레거시 폐기 기준

신규 UI에서는 아래 패턴을 사용하지 않는다.

- 하나의 JS 파일에 모든 feature 상태와 API 호출을 모으는 방식.
- JSON API 응답을 받아 클라이언트에서 HTML 문자열을 조립하는 방식.
- Bootstrap component와 동일한 역할을 하는 커스텀 탭, 모달, 드롭다운 재구현.
- 화면별로 임의 색상, 임의 radius, 임의 spacing을 추가하는 방식.
- 관리자 전용 액션을 주황색 같은 별도 브랜드 밖 색상에 의존해 구분하는 방식.

허용되는 JavaScript:

- htmx runtime.
- Bootstrap component bundle.

### 2.2 서버 렌더링 계약

각 화면은 full page route와 fragment/action route의 반환 범위를 명확히 한다.
같은 path가 일반 브라우저 요청에서는 full page, htmx 요청에서는 fragment를 반환할 수 있다.

| 유형 | 반환 | 용도 |
| --- | --- | --- |
| Page | 전체 Thymeleaf page | 직접 접근, 새로고침, fallback |
| Fragment | Thymeleaf fragment HTML | htmx 부분 갱신 |
| Redirect | HTTP redirect | 인증, 권한, 완료 후 전체 이동 |
| JSON | JSON | OpenAPI, Actuator, 외부 provider 계약처럼 화면 fragment가 아닌 경우 |

UI에서 htmx로 호출하는 endpoint는 가능한 한 HTML fragment를 반환한다.
클라이언트가 JSON을 받아 DOM을 직접 조립해야 한다면 해당 영역은 htmx 대상이 아니다.

### 2.3 htmx 계약

htmx 사용 기본값:

- 조회: `hx-get`
- 생성/수정: `hx-post` 또는 `hx-put`
- 삭제/폐기: `hx-delete`
- 대상: `hx-target`은 component root 또는 명확한 region id
- 교체: 목록/폼 영역은 `innerHTML`, 행 단위는 `outerHTML`
- loading: `hx-indicator`
- 중복 클릭 방지: `hx-disabled-elt`
- 검색: `hx-trigger="keyup changed delay:300ms"` 또는 `change`

규칙:

- htmx target은 너무 넓게 잡지 않는다. 전체 `main` 교체는 page 전환에만 사용한다.
- fragment 응답은 교체될 target의 구조와 맞아야 한다.
- 관련 컴포넌트 동기화는 `HX-Trigger` 응답 헤더 또는 `hx-swap-oob`로 처리한다.
- CSRF token은 모든 mutating request에 포함되어야 한다.
- htmx 요청 실패는 target 내부 error state 또는 alert fragment로 회복 가능해야 한다.

## 3. 색상

### 3.1 브랜드 기준

CHZZK 공식 브랜드 컬러를 기준으로 한다.

| 기준 | 값 | Bootstrap 적용 |
| --- | --- | --- |
| Brand primary | `#00FFA3` | `btn-success`, `btn-outline-success`, `text-success`, active state |
| Brand black | `#000000` | `bg-black`, `bg-dark bg-gradient`, dark app background |
| Surface | Bootstrap dark surface | `table-dark`, `text-bg-dark`, `border-secondary-subtle` |
| Text | Bootstrap light text | `text-light`, `text-secondary` |

Semantic color는 브랜드 컬러와 혼동되지 않게 제한적으로 쓴다.

| Bootstrap 적용 | 용도 |
| --- | --- |
| `text-bg-danger`, `btn-outline-danger`, `alert-danger` | 삭제, 실패, 음수, 위험 |
| `text-bg-warning`, `alert-warning` | 주의, 비활성화 전 확인, 정책 근접 |
| `alert-success`, `text-bg-success` | 완료 상태 |
| `alert-info` | 보조 정보 |

### 3.2 색상 사용 규칙

- primary action과 selected state는 CHZZK Neon Green 계열을 사용한다.
- 관리자 전용 액션은 별도 색상 role을 만들지 않고 위치, label, icon, permission badge로 구분한다.
- danger는 삭제, 폐기, 복구 불가, 음수 강조에만 사용한다.
- warning은 위험 전 단계에만 사용하고 primary action처럼 보이면 안 된다.
- Bootstrap 기본 `success`, `danger`, `dark` 계열을 우선 사용한다.
- 정확한 CHZZK 색상 매칭이 필요하면 기능별 CSS가 아니라 Bootstrap theme build에서 한 번에 조정한다.
- 색상만으로 상태를 전달하지 않는다. badge text, helper text, icon 중 하나를 함께 둔다.

## 4. 컴포넌트 구조 규칙

### 4.1 파일과 fragment

신규 구조:

```text
templates/
  components/
    badges.html
    buttons.html
    feedback.html
    forms.html
    navigation.html
    tables.html
	  features/
	    attendance/
	      components.html
	    command/
	      components.html
	      regions.html
	    favorite/
	      components.html
	      overlays.html
	    roulette/
	      components.html
```

규칙:

- 공통 컴포넌트 fragment는 `components/*` 아래에 둔다.
- 업무 단위 UI 조각은 `features/{feature}/components.html` 아래에 둔다.
- 기능 전용 overlay나 modal shell은 `features/{feature}/overlays.html`처럼 기능 디렉터리 안에 둔다.
- htmx가 직접 교체하는 region wrapper는 `features/{feature}/regions.html` 아래에 두고, 내부 마크업은 feature component와 공통 component를 조합한다.
- 페이지 템플릿은 feature component를 조합만 하고 테이블 행, 폼 필드, 상태 badge 같은 세부 마크업을 직접 갖지 않는다.
- id는 htmx target, form association, accessibility에 필요한 경우에만 사용한다.
- class는 Bootstrap class만 사용한다.
- 프로젝트 전용 class는 htmx target, 접근성, 테스트 목적을 Bootstrap class로 표현할 수 없을 때만 예외적으로 검토한다.

### 4.2 Component root

모든 htmx 교체 대상은 component root를 가진다.

```html
<section id="command-list-region"
         class="border border-secondary-subtle rounded p-3"
         data-component="command-list">
    ...
</section>
```

규칙:

- root id는 `{feature}-{component}-region` 형태를 사용한다.
- root는 loading, error, empty 상태를 모두 표현할 수 있어야 한다.
- root 내부의 action은 가능한 한 root 자신 또는 가까운 region만 교체한다.

### 4.3 기능 유지 계약

컴포넌트화는 기능 삭제가 아니라 구현 방식 변경이다.

규칙:

- 기존 탭, 모달, 목록, 히스토리, 출석체크, 룰렛, 명령어 관리 기능은 동등 동작이 준비되기 전까지 제거하지 않는다.
- 레거시 CSS/JavaScript는 같은 기능을 제공하는 Bootstrap/Thymeleaf/htmx 컴포넌트가 검증된 뒤 제거한다.
- 전환 중 임시 호환 코드가 필요하면 기능 단위로 경계를 명확히 하고, 제거 조건을 테스트로 둔다.
- 테이블, 폼, badge, alert, empty state, pagination은 실제 기능 페이지에서 공통 fragment를 재사용한다.

## 5. Layout 컴포넌트

### 5.1 App Shell

용도:

- 인증 사용자 화면과 관리자 화면의 공통 외곽.

구조:

- `body`는 `bg-black text-light`를 기본으로 한다.
- `header`는 Bootstrap spacing과 border utility로 구성한다.
- `main`은 `container-xxl` 또는 `container-fluid`를 사용한다.
- `footer`는 필요한 경우에만 사용한다.

Bootstrap 기준:

- `container-fluid` 또는 `container-xxl`
- `d-flex`, `align-items-center`, `justify-content-between`
- responsive spacing utility

UX 규칙:

- 첫 화면에서 현재 사용자, 권한, 주요 위치를 바로 보여준다.
- 관리자 화면은 별도 배경이나 과한 장식 없이 기능 밀도를 우선한다.
- mobile에서는 header action을 overflow menu 또는 세로 버튼 그룹으로 전환한다.

### 5.2 Page Header

용도:

- 화면 제목, 설명, 주요 action, 현재 context 표시.

구조:

- title
- subtitle 또는 context
- primary action group
- optional meta badge

규칙:

- 제목은 한 줄을 우선하고, 모바일에서는 2줄까지 허용한다.
- primary action은 데스크톱 오른쪽, 모바일 하단 full width로 이동한다.
- 설명 문구는 기능 안내가 아니라 현재 화면의 상태나 범위를 설명한다.

### 5.3 Region

용도:

- htmx 교체 가능한 작업 단위.

상태:

- default
- loading
- empty
- error
- disabled

규칙:

- region 안에 다시 큰 카드형 region을 중첩하지 않는다.
- region title은 작은 heading으로 유지한다.
- htmx request 중에는 region 전체를 흔들지 말고 button/indicator 중심으로 상태를 보여준다.

### 5.4 Toolbar

용도:

- 검색, 필터, refresh, bulk action.

Bootstrap 기준:

- `d-flex`
- `gap-*`
- `flex-wrap`
- `input-group`
- `btn-group`

규칙:

- 검색/필터는 목록보다 작게 보인다.
- bulk action은 선택 대상 수와 함께 보여준다.
- action이 3개를 넘으면 priority를 정해 secondary menu로 보낸다.

### 5.5 Tabs

용도:

- 같은 권한과 같은 화면 안의 작업 전환.

Bootstrap 기준:

- `nav`
- `nav-tabs` 또는 `nav-pills`
- `tab-content`
- `tab-pane`

htmx 기준:

- tab content가 크거나 독립 데이터가 필요하면 tab click 시 `hx-get`으로 pane fragment를 로드한다.
- 한번 로드한 pane을 캐시할지 매번 새로 받을지는 feature별로 명시한다.

규칙:

- 탭은 최대 6개를 넘기지 않는다.
- 서로 다른 사용자 목표는 탭보다 별도 page 후보로 분리한다.
- active tab은 CHZZK Neon Green border 또는 background로 표시한다.

## 6. Action 컴포넌트

### 6.1 Button

역할:

| 역할 | Bootstrap 기반 | 용도 |
| --- | --- | --- |
| Primary | `btn btn-success` | 저장, 적용, 활성화, 발급 |
| Secondary | `btn btn-secondary` | 취소, 닫기, 낮은 우선순위 실행 |
| Ghost | `btn btn-outline-success` 또는 `btn btn-outline-secondary` | 새로고침, 검증, 미리보기 |
| Danger | `btn btn-outline-danger` | 삭제, 폐기, 위험 비활성화 |
| Icon | `btn btn-outline-secondary` + `aria-label` | 닫기, 복사, 새로고침, 펼침 |

규칙:

- 버튼 height는 같은 toolbar 안에서 동일해야 한다.
- primary는 화면 또는 region당 하나를 우선한다.
- 관리자 전용 primary는 위치, label, 권한 badge로 구분한다.
- icon button은 `aria-label`을 필수로 둔다.
- htmx request를 보내는 버튼은 `hx-disabled-elt="this"` 또는 form 단위 disabled를 사용한다.

htmx 예:

```html
<button type="submit"
        class="btn btn-success"
        hx-post="/admin/commands"
        hx-target="#command-editor-region"
        hx-swap="outerHTML"
        hx-disabled-elt="this"
        hx-indicator="#command-save-indicator">
    저장
</button>
```

### 6.2 Button Group

용도:

- 같은 대상에 대한 보조 action 묶음.

규칙:

- primary와 danger를 같은 button group에 붙이지 않는다.
- danger는 오른쪽 끝 또는 별도 confirm 영역에 둔다.
- mobile에서는 세로 stack으로 전환한다.

### 6.3 Link Action

용도:

- 페이지 이동 또는 상세 보기.

규칙:

- 상태 변경에는 link style을 쓰지 않는다.
- htmx로 region만 바꾸는 이동은 사용자가 page 이동으로 착각하지 않게 active 상태를 표시한다.

## 7. Form 컴포넌트

### 7.1 Form Layout

Bootstrap 기준:

- `form-label`
- `form-control`
- `form-select`
- `form-check`
- `invalid-feedback`
- grid utility

규칙:

- 모든 input은 label을 가진다.
- placeholder는 예시로만 사용한다.
- 필수값은 label과 error message 양쪽에서 알 수 있어야 한다.
- submit 결과가 같은 form을 갱신하면 form root를 `hx-target`으로 삼는다.
- submit 결과가 목록도 바꿔야 하면 목록은 out-of-band swap 또는 `HX-Trigger`로 갱신한다.

### 7.2 Field

구조:

- label
- control
- helper text
- validation message

상태:

- default
- focus
- disabled
- readonly
- invalid
- valid
- loading

규칙:

- helper text는 입력 전 판단에 필요한 내용만 쓴다.
- validation message는 원인과 조치를 함께 쓴다.
- backend raw exception message를 그대로 노출하지 않는다.

### 7.3 Search Field

htmx 기준:

```html
<input class="form-control"
       name="keyword"
       type="search"
       hx-get="/admin/commands"
       hx-trigger="keyup changed delay:300ms, search"
       hx-target="#command-list-region"
       hx-swap="outerHTML"
       placeholder="명령어 또는 설명 검색">
```

규칙:

- 검색은 최소 300ms delay를 둔다.
- 검색어가 비었을 때 기본 목록을 복구한다.
- 검색 결과 없음은 empty state로 보여준다.

### 7.4 Validation Summary

용도:

- form 전체 검증 결과, 명령어 충돌, 룰렛 확률 합계 같은 복합 검증 표시.

규칙:

- field-level 오류와 summary를 동시에 사용할 수 있다.
- summary는 저장 버튼 근처 또는 preview 영역 위에 둔다.
- 성공 상태도 명확하게 표시해 사용자가 저장 가능 여부를 판단하게 한다.

## 8. Data Display 컴포넌트

### 8.1 Table

용도:

- 행과 열 비교가 중요한 관리자 목록.

Bootstrap 기준:

- `table`
- `table-responsive`
- `align-middle`

규칙:

- 숫자 열은 우측 정렬한다.
- 상태 열은 badge와 텍스트를 함께 사용한다.
- 행 hover는 약한 surface 변화만 사용한다.
- row action은 오른쪽 끝에 모은다.
- 모바일에서는 `table-responsive` 또는 card row 중 하나를 선택하고, 둘을 섞지 않는다.

### 8.2 List Row

용도:

- 명령어, 룰렛 항목, 히스토리처럼 행 내부 정보 묶음이 중요한 목록.

규칙:

- row 전체 click과 내부 button click이 충돌하지 않아야 한다.
- 선택된 row는 brand border 또는 subtle brand background로 표시한다.
- 비활성 row는 opacity만 낮추지 말고 `비활성` badge를 둔다.

### 8.3 Badge

역할:

| 역할 | 용도 |
| --- | --- |
| Brand | 활성, 선택, primary state |
| Neutral | 조회, 일반 meta |
| Warning | 주의, 확인 필요 |
| Danger | 실패, 음수, 삭제 예정 |
| Success | 완료. primary 강조로 쓰지 않음 |

규칙:

- badge text는 2단어 이하로 유지한다.
- badge는 action button처럼 보이면 안 된다.
- status는 badge만 두지 말고 필요한 경우 helper text를 추가한다.

### 8.4 Empty State

구조:

- 짧은 제목
- 현재 조건 설명
- 다음 action 1개

규칙:

- 빈 상태에 긴 기능 설명을 넣지 않는다.
- 검색 결과 없음은 검색어와 초기화 action을 제공한다.
- 권한 때문에 비어 보이는 상태와 실제 데이터 없음은 구분한다.

### 8.5 Pagination

규칙:

- page 이동은 목록 region만 갱신한다.
- 현재 page와 total count를 표시한다.
- mobile에서는 이전/다음 중심으로 줄인다.

htmx 기준:

- `hx-get`으로 다음 page fragment 요청.
- `hx-target`은 목록 region.
- `hx-push-url`은 사용자가 공유해야 하는 목록 화면에만 사용한다.

## 9. Feedback 컴포넌트

### 9.1 Alert

용도:

- region 내부 오류, 권한 없음, 검증 실패.

규칙:

- alert는 사용자가 조치할 위치 근처에 둔다.
- API 실패는 재시도 action을 제공한다.
- 권한 없음은 숨겨진 기능처럼 보이지 않게 명확히 표시한다.

### 9.2 Toast

용도:

- 저장 완료, 복사 완료, 재송출 요청 완료 같은 짧은 결과.

htmx 기준:

- 서버가 `hx-swap-oob` toast fragment를 함께 반환할 수 있다.
- 또는 `HX-Trigger`로 클라이언트 toast hook을 호출한다.

규칙:

- 검증 실패를 toast만으로 처리하지 않는다.
- toast는 1줄 메시지를 우선한다.
- 같은 action이 연속 실행되면 toast를 누적하지 않고 최신 상태로 갱신한다.

### 9.3 Loading

규칙:

- 버튼 요청은 버튼 내부 spinner 또는 인접 indicator를 사용한다.
- 목록 요청은 기존 목록을 지우지 않고 상단/하단 loading indicator를 보여준다.
- 최초 로딩은 skeleton을 사용할 수 있다.
- request 중 action 중복 클릭을 막는다.

### 9.4 Confirm

용도:

- 삭제, 폐기, 위험 비활성화, 대량 적용.

규칙:

- 대상, 결과, 되돌릴 수 있는지 여부를 보여준다.
- danger button은 confirm 안에서만 최종 실행한다.
- 간단 확인은 `hx-confirm`을 허용하지만, 복합 위험 action은 modal confirm을 사용한다.

## 10. Overlay 컴포넌트

### 10.1 Modal

Bootstrap 기준:

- Bootstrap modal 구조와 focus trap을 사용한다.

규칙:

- modal은 복합 입력, 확인, preview에만 사용한다.
- 단순 상태 변경은 inline confirm 또는 alert를 우선한다.
- modal body만 scroll되고 header/action은 고정 위치를 유지한다.
- htmx로 modal body를 로드할 수 있지만, modal shell은 공통 컴포넌트로 유지한다.

### 10.2 Drawer

용도:

- 상세 히스토리, 로그, preview처럼 현재 목록 context를 유지해야 하는 보조 화면.

규칙:

- drawer는 오른쪽에서 열리는 보조 panel로 제한한다.
- 저장/삭제 같은 핵심 action을 drawer에 과도하게 넣지 않는다.
- mobile에서는 full-screen panel로 전환한다.

## 11. Feature 컴포넌트 조합

### 11.1 관리자 명령어 관리

권장 구성:

1. Page Header: `명령어 관리`, 새 명령어 action.
2. Toolbar: type filter, active filter, search.
3. Command List Region: 목록, empty, loading, pagination.
4. Command Editor Region: form, validation summary, preview.
5. Toast Region: 저장/비활성화 결과.

htmx 흐름:

- filter/search 변경: list region 갱신.
- row 선택: editor region 갱신.
- 검증: validation summary fragment 갱신.
- 미리보기: preview fragment 갱신.
- 저장: editor region 갱신, list region out-of-band 갱신, toast 표시.

### 11.2 호감도 보드

권장 구성:

1. Page Header: 현재 사용자와 권한.
2. Search Toolbar.
3. Ranking List Region.
4. History Drawer 또는 inline detail region.
5. Pagination.

htmx 흐름:

- 검색/페이지 이동: ranking list region 갱신.
- 행 상세 열기: 해당 row detail region 갱신.
- 관리자 조정 완료: 해당 row와 toast out-of-band 갱신.

### 11.3 출석체크

권장 구성:

1. Status Summary.
2. Participant List Region.
3. Bulk Selection Toolbar.
4. Apply Form.
5. Result Alert.

규칙:

- 선택 대상 수와 적용 값을 항상 함께 보여준다.
- 대량 적용은 confirm을 통과해야 한다.
- 적용 완료 후 선택 상태는 서버 응답 기준으로 재계산한다.

### 11.4 룰렛 관리

권장 구성:

1. Table List Region.
2. Roulette Item List Region.
3. Probability Validation Summary.
4. Simulation Preview.
5. Overlay Action Toolbar.
6. Recent Event List Region.

규칙:

- 확률 합계, 꽝 존재 여부, 활성화 가능 여부를 저장/활성화 버튼 근처에 둔다.
- 시뮬레이션은 실제 저장과 명확히 분리한다.
- 재송출은 icon button과 label을 함께 제공한다.

## 12. 컴포넌트 검증

별도 Storybook 또는 `/ui-catalog` 화면은 두지 않는다.
컴포넌트 검증은 실제 기능 페이지 렌더링 테스트와 fragment 단위 테스트로 수행한다.

규칙:

- 공통 fragment는 이를 사용하는 실제 페이지 또는 fragment 렌더링 테스트에서 검증한다.
- 입력, 버튼, 테이블, pagination, alert, badge는 실제 기능 모델을 넣어 렌더링한다.
- htmx fragment는 성공, 실패, 빈 상태, 권한 실패를 controller/template 테스트로 확인한다.
- 디자인 검증은 로컬 실행 화면과 스크린샷 확인으로 수행하되 별도 카탈로그 route는 만들지 않는다.

필수 variant는 테스트나 실제 기능 화면에서 확인한다.

| Component | 필수 variant |
| --- | --- |
| Button | primary, secondary, ghost, danger, icon, loading, disabled |
| Field | default, focused, invalid, disabled, readonly, helper text |
| Table/List | default, selected, inactive, empty, loading, error |
| Badge | brand, neutral, warning, danger, success |
| Alert/Toast | info, warning, danger, success |
| Modal | small form, confirm, long content |

## 13. 접근성

필수 기준:

- 모든 interactive element는 keyboard focus가 보여야 한다.
- icon button은 `aria-label`을 가진다.
- form control은 label과 연결한다.
- modal은 title과 close button을 가진다.
- 색상 대비는 dark background에서 확인한다.
- hover에만 정보가 존재하면 안 된다.

htmx 주의:

- fragment 교체 후 focus 위치를 정한다.
- validation 실패 후 첫 오류 field로 focus 이동을 고려한다.
- loading indicator는 screen reader에서 과도하게 반복되지 않게 한다.

## 14. 반응형 기준

Viewport 기준:

| 크기 | 기준 |
| --- | --- |
| 360px | 최소 모바일. 텍스트와 버튼 overflow 금지 |
| 768px | tablet. toolbar wrap과 1열/2열 전환 확인 |
| 1280px | 관리자 기본 검증 |
| 1920px | 방송 운영 중 넓은 화면 검증 |

규칙:

- toolbar는 wrap되어도 action 우선순위가 유지되어야 한다.
- table은 모바일에서 `table-responsive` 또는 card row 중 하나로 정한다.
- modal은 모바일에서 full width에 가깝게 열리고 내부 scroll을 사용한다.
- 버튼 텍스트가 길면 줄바꿈보다 label을 짧게 고친다.

## 15. 구현 체크리스트

새 컴포넌트는 아래 기준을 통과해야 한다.

- Spring controller가 page와 fragment 반환을 분리했다.
- Thymeleaf fragment가 독립적으로 렌더링된다.
- 공통 컴포넌트는 실제 기능 페이지 또는 fragment 렌더링 테스트에서 variant가 검증되어 있다.
- Bootstrap component/utility를 먼저 사용했다.
- htmx target과 swap 범위가 component root로 제한되어 있다.
- mutating request에 CSRF가 포함되어 있다.
- loading, empty, error, disabled 상태가 있다.
- CHZZK Neon Green과 Black 방향의 Bootstrap 색상 조합을 사용했다.
- semantic color는 danger/warning/success/info에만 제한했다.
- mobile 360px에서 overflow가 없다.
- keyboard focus와 `aria-label`이 빠지지 않았다.
- UI용 전역 JS 상태를 새로 만들지 않았다.
- `git diff --check`가 통과한다.
