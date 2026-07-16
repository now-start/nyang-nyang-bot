# Design QA

## Visuals

- Reference: `/Users/moon/.codex/generated_images/019f694b-cb64-74b2-8c22-3dde01de77f8/exec-e6aeda20-65ed-49d8-985a-6338b863abe8.png`
- Desktop command implementation: `/Users/moon/.codex/visualizations/2026/07/16/019f694b-cb64-74b2-8c22-3dde01de77f8/command-desktop.png`
- Desktop timer implementation: `/Users/moon/.codex/visualizations/2026/07/16/019f694b-cb64-74b2-8c22-3dde01de77f8/timer-desktop-final.png`
- Mobile command overview: `/Users/moon/.codex/visualizations/2026/07/16/019f694b-cb64-74b2-8c22-3dde01de77f8/command-mobile-top.png`
- Mobile variable picker: `/Users/moon/.codex/visualizations/2026/07/16/019f694b-cb64-74b2-8c22-3dde01de77f8/command-mobile.png`
- Mobile timer overview: `/Users/moon/.codex/visualizations/2026/07/16/019f694b-cb64-74b2-8c22-3dde01de77f8/timer-mobile-final.png`

## Viewports and states

- Desktop: `1487 x 1058`, local administrator, command edit state with the grouped variable picker open. The reference and implementation were opened together at this viewport for comparison.
- Desktop timer: `1487 x 1058`, persisted timer edit state with the time-variable picker open. The reference and final implementation were opened together in this matching state for the final comparison.
- Mobile: `390 x 844`, command and timer states checked; the timer screenshot records the list state and horizontal tab navigation.

## Comparison

- The implementation keeps the reference's dark surface hierarchy, green active accent, two-column desktop workspace, compact management table, and variable picker anchored to the message field.
- The picker preserves grouped discovery and examples while using the existing Bootstrap and project theme components.
- Differences from the reference are intentional follow-up requirements: validation and preview are one action, create and update labels are distinct, and timer messages use a separate tab, table, editor, and variable scope.
- On mobile, tabs remain single-line and horizontally scrollable, the workspace stacks into cards, the table scrolls inside its container, and the variable picker stays within the viewport without creating page-level horizontal overflow.

## Interaction and console checks

- Command: initial placeholder, new/edit states, grouped variable insertion, combined validation/preview, and create/update/deactivate action visibility checked.
- Timer: separate list/editor, time-only variable picker, successful preview, unsupported-variable validation failure, create/list refresh, and inactive edit state checked.
- A preview was cleared immediately after a form change, preventing a stale validation result from remaining visible.
- An active timer was created, edited without saving, and then deactivated. The saved message and 45-minute interval were restored, confirming that deactivation changes only the active state.
- Missing command and timer editor IDs render user-facing error fragments instead of server-error pages.
- Inactive table rows retain readable content and edit-button contrast; desktop and mobile layouts have no page-level horizontal overflow.
- HTMX action buttons no longer inherit the invalid `find button` disabled selector. Repeating preview after the fix produced no new browser console errors.
- Local startup applied Flyway migrations V1 through V5 and validated the JPA queries.

## Final result

passed
