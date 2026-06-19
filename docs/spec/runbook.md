# 운영/배포 Runbook

- 문서 상태: Draft v1
- 작성일: 2026-05-08
- 상위 문서: [Nyang-Nyang Bot Spec](index.md)
- 관련 문서:
  - [기술/운영 부록](technical-appendix.md)
  - [API 명세](api.md)
  - [이벤트 명세](events.md)
  - [테스트 전략](test-strategy.md)

## 1. 목적

운영자가 배포, 장애 대응, 마이그레이션, 토큰 관리, 모니터링 확인 시 따라야 할 절차를 정의한다.

## 2. 배포 전 체크리스트

- `./gradlew test --no-daemon` 통과.
- WSL에서 wrapper 실행 권한 또는 line ending 문제가 있으면 `java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --no-daemon`로 동일 검증을 수행한다.
- CHZZK OAuth 로그인 수동 검증.
- 관리자 권한 계정으로 관리자 화면/API 접근 검증.
- 일반 사용자 계정으로 관리자 API 거부 검증.
- 민감 설정 값이 로그에 출력되지 않는지 확인.
- OAuth state, access token, refresh token, overlay token 원문이 로그에 출력되지 않는지 확인.
- DB migration 적용 계획 확인.
- Google Sheets 마이그레이션 실행 여부 확인.
- 후원 룰렛 `!룰렛` 명령어 exact token 매칭과 중복 후원 이벤트 ID 재수신을 확인.
- OBS 오버레이 URL이 `/overlay/roulette#token=...` fragment 형식인지 확인.
- Grafana/Actuator 상태 확인.
- 롤백 대상 release 또는 image tag 확인.

## 3. 배포 절차

1. PR을 통해 `main`에 병합한다.
2. GitHub Actions Build/Test 결과를 확인한다.
3. Docker image 생성과 GHCR push를 확인한다.
4. 배포 완료 후 Actuator 상태를 확인한다.
5. OAuth 로그인과 호감도 보드 조회를 smoke test로 확인한다.
6. 관리자 호감도 조정 또는 staging equivalent를 확인한다.
7. Grafana/Loki에서 오류 로그를 확인한다.

## 4. 롤백 절차

- Release 버전을 PreRelease로 변경하면 자동 롤백을 수행한다.
- 롤백 전후 확인 항목:
  - 현재 배포 image tag
  - DB migration 적용 여부
  - 새 schema가 이전 앱과 호환되는지
  - 원장/업보/룰렛 이벤트의 중복 반영 여부

원칙:

- 원장 거래는 삭제하지 않는다.
- 잘못 지급된 호감도나 룰렛 결과는 관리자 보정 거래로 정정한다.
- 외부 이벤트와 연결된 데이터는 물리 삭제하지 않는다.

## 5. Flyway 마이그레이션 사전 점검

적용 전:

- 운영 DB 백업을 생성한다.
- config server에 `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`, `spring.jpa.hibernate.ddl-auto=validate`를 반영한다.
- `flyway_schema_history`가 이미 있으면 version/status를 확인하고, 없으면 첫 배포에서 baseline이 생성되는 것을 전제로 한다.
- 운영 DB 버전이 dump 기준과 같은 MariaDB 10.11 계열인지 확인한다.
- 운영 DB에 물리 FK가 남아 있는지 확인한다. 현재 최종 정책은 FK 없이 UNIQUE/INDEX와 application transaction으로 관리하는 것이다.

MariaDB/MySQL FK 확인 예:

```sql
SELECT
    table_name,
    constraint_name,
    referenced_table_name
FROM information_schema.referential_constraints
WHERE constraint_schema = DATABASE();
```

FK가 남아 있으면:

- `favorite_history_entity`의 FK 이름이 `FKnjkqgrcjhyhbc9544fedyj0a0`인지 확인한다.
- 이름이 dump와 같으면 `V2`가 FK와 같은 이름의 보조 인덱스를 제거한다.
- 이름이 다르면 배포를 중단하고 dump를 다시 받아 migration을 재검토한다.
- 이름을 모르는 FK drop SQL을 운영에 직접 실행하지 않는다.

적용 후:

- `flyway_schema_history`에 baseline `1`과 `V2` 성공 이력이 있는지 확인한다.
- 기존 테이블명(`*_entity`)이 도메인 테이블명으로 rename되었는지 확인한다.
- `authorization_account`, `favorite_account`, `favorite_history`, `roulette_event`, `user_upbo` 등 핵심 테이블 row count를 확인한다.
- smoke test로 OAuth 로그인, 호감도 보드, 관리자 호감도 조정을 확인한다.

## 6. OAuth 장애 대응

증상:

- 로그인 후 `/token`에서 실패.
- 사용자가 반복적으로 로그인 페이지로 돌아감.
- CHZZK 토큰 갱신 실패.

확인:

- OAuth `clientId`, `clientSecret`, `redirectUrl` 설정.
- OAuth `state` 생성/검증 실패 로그.
- CHZZK token API 응답 상태.
- access/refresh token 원문이 로그에 남지 않는지.

조치:

- 설정 불일치가 있으면 운영 설정을 수정하고 재배포한다.
- state mismatch가 반복되면 세션 저장소와 프록시 쿠키 설정을 확인한다.
- 토큰 갱신 실패 사용자는 재로그인을 유도한다.

## 7. CHZZK 소켓 장애 대응

증상:

- 채팅 명령어 응답 없음.
- 출석체크 대상자가 수집되지 않음.
- 후원 룰렛이 실행되지 않음.

확인:

- `/chzzk/connect` 관리자 수동 연결 결과.
- CHZZK 세션 발급 API 응답.
- 채팅/후원/구독 이벤트 subscribe 성공 여부.
- WebSocket 비정상 종료 로그.
- 재연결 루프 여부.

조치:

- 관리자 수동 연결을 시도한다.
- CHZZK API 장애면 재시도 간격을 유지하고 자동 재연결 상태를 확인한다.
- 소켓 재연결 후 `!호감도` smoke test를 수행한다.

## 8. Google Sheets 마이그레이션

실행 전:

- 서비스 계정 키 파일 경로 확인.
- spreadsheet ID 확인.
- 대상 range `호감도 순위표!B2:H2000` 확인.
- 운영 DB 백업 또는 rollback point 확인.

실행 후:

- 처리 row 수 확인.
- 실패 row 수와 실패 사유 확인.
- 사용자별 마지막 원장 `balanceAfter`와 `FavoriteAccount.favorite` 일치 확인.
- 중복 실행 여부 확인.

주의:

- Google Sheets는 전환기 기능이다.
- 플랫폼 DB 원장이 최종 원본이 된 뒤에는 Sheets 의존성을 제거한다.

## 9. 호감도 원장 불일치 대응

증상:

- 사용자 제보로 현재 호감도와 히스토리 합계가 다름.
- 관리자 조회에서 의심 케이스 발견.

확인:

- 사용자별 마지막 원장 `balanceAfter`.
- 현재 `FavoriteAccount.favorite`.
- 최근 관리자 조정/출석/업보/룰렛 거래.
- 중복 `idempotencyKey` 존재 여부.

조치:

- 원본 거래는 수정/삭제하지 않는다.
- 관리자 보정 거래로 차이를 정정한다.
- 원인을 `favorite.correct` 운영 로그에 남긴다.

## 10. 룰렛 장애 대응

증상:

- 후원 메시지에 `!룰렛`이 있었지만 룰렛 미실행.
- 룰렛 결과 일부만 반영.
- 같은 후원이 중복 반영된 것으로 보임.

확인:

- 후원 이벤트 ID 저장 여부.
- 룰렛 테이블 활성 상태와 후원 시점 스냅샷.
- 명령어 토큰 정확 일치 여부.
- `roulette_round_result`의 `CONFIRMED` 잔여 회차.
- `idempotencyKey` UNIQUE 충돌 로그.

조치:

- `CONFIRMED` 상태 회차는 재반영을 실행한다.
- 중복 반영이 확인되면 삭제하지 않고 보정 거래로 정정한다.
- 잘못된 확률표는 비활성화하고 수정 후 재활성화한다.

## 11. 오버레이 토큰 관리

발급:

- 관리자 페이지에서 토큰을 발급한다.
- 토큰 원문은 발급 직후 한 번만 보여준다.
- OBS 브라우저 소스에는 `/overlay/roulette#token=...` 형식으로 등록한다.

재발급:

- 기존 토큰이 유출되었거나 담당자가 바뀌면 rotate한다.
- 기존 토큰은 즉시 무효화한다.
- OBS URL을 새 토큰으로 교체한다.

폐기:

- 더 이상 사용하지 않는 토큰은 revoke한다.
- `lastUsedAt`이 오래된 토큰은 운영자가 수동 정리한다.

주의:

- 토큰을 query string에 넣지 않는다.
- 로그, 스크린샷, 이슈 본문에 토큰 원문을 남기지 않는다.

## 12. 오버레이 재송출

대상:

- `MISSED` 이벤트.
- 이미 표시되었지만 방송상 다시 보여줘야 하는 이벤트.

절차:

1. 관리자 화면에서 대상 룰렛 이벤트를 찾는다.
2. 재송출을 실행한다.
3. OBS 오버레이가 새 표시 이벤트를 가져가는지 확인한다.
4. 원장/보유 목록이 재반영되지 않았는지 확인한다.

## 13. Grafana/Loki 확인

기본 검색 키:

| 목적 | query 기준 |
| --- | --- |
| 오류 확인 | `level=ERROR` |
| 관리자 행위 | `level=AUDIT` |
| 호감도 조정 | `action=favorite.adjust` |
| 룰렛 실행 | `action=roulette.run` |
| 룰렛 고회차 | `action=roulette.high_round` |
| 오버레이 토큰 변경 | `action=overlay_token.rotate` 또는 `action=overlay_token.revoke` |
| 오버레이 재송출 | `action=overlay.replay` |
| 룰렛 테이블 활성화 | `action=roulette_table.activate` |

라벨 정책:

- Loki 라벨은 `level`, `action`, `result`처럼 카디널리티가 낮은 값 위주로 둔다.
- `actor`, `target`, `trace_id`, 닉네임은 라벨이 아니라 메시지 페이로드로 둔다.

릴리즈 직후 확인 query:

```text
{app="nyang-nyang-bot"} |= "result=success"
{app="nyang-nyang-bot"} |= "action=roulette.run"
{app="nyang-nyang-bot"} |= "action=overlay_token.rotate"
{app="nyang-nyang-bot"} |= "level=ERROR"
```

## 13. 민감 값 노출 대응

민감 값:

- CHZZK access token
- CHZZK refresh token
- OAuth state
- 오버레이 토큰 원문
- DB password
- Google service account key

노출 시 조치:

1. 노출 범위를 확인한다.
2. 해당 토큰/키를 즉시 폐기 또는 재발급한다.
3. 로그 보존 정책에 따라 삭제 가능 여부를 운영자가 판단한다.
4. 원인 코드를 수정하고 테스트를 추가한다.
5. 재발 방지를 위해 릴리즈 체크리스트에 항목을 추가한다.

## 14. 운영 미결정 항목

| 항목 | 현재 기본값 | 결정 시점 |
| --- | --- | --- |
| DB migration 도구 | 미정 | 첫 schema 변경 전 |
| Test DB 방식 | 미정 | 원장 migration 전 |
| Actuator 공개 범위 | 공개로 문서화됨, 운영 제한 검토 필요 | 배포 전 |
| 룰렛 고회차 임계치 | 기본 100회 | 룰렛 출시 전 |
| 오버레이 토큰 정리 주기 | 수동 | 운영 후 |
