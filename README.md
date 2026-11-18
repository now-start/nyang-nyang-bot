# Nyang-Nyang Bot

CHZZK 채널용 Spring Boot 봇입니다. 핵심 기능은 호감도 원장, 리워드 보유 목록, 생존자 보상, 룰렛, OBS 오버레이, 관리자 명령어와 타이머 메시지 관리입니다.

## Local Run

```powershell
.\gradlew.bat bootRun
```

- 기본 profile은 `local`입니다.
- 기본 URL: `http://localhost:8080`
- 로컬 테스트 로그인: `http://localhost:8080/local/test-login`
- DB: PostgreSQL 18. `NYANG_DATABASE_URL`, `NYANG_DATABASE_USERNAME`, `NYANG_DATABASE_PASSWORD`로 연결을 지정합니다.
- 기본 로컬 URL: `jdbc:postgresql://localhost:5432/nyang_nyang_bot?sslmode=disable`
- 운영은 TLS(`sslmode=require`)와 primary 전용 접속을 사용합니다.

로컬 계정:

| 계정 | 권한 | 용도 |
| --- | --- | --- |
| `local-channel` | ADMIN | 관리자 화면 확인 |
| `local-viewer` | USER | 일반 사용자 제한 확인 |
| `user-006` | USER | 음수 잔액 확인 |

실제 CHZZK 연동을 확인할 때만 아래 값을 설정합니다.

```powershell
$env:CHZZK_CLIENT_ID=""
$env:CHZZK_CLIENT_SECRET=""
```

## Test

Docker가 필요합니다. Testcontainers가 격리 PostgreSQL 18을 실행하며 운영 DB를 사용하지 않습니다.

```powershell
.\gradlew.bat test
```

## Docs

- [문서 인덱스](docs/README.md)
- [핵심 명세](docs/SPEC.md)

문서는 핵심 정책과 현재 구현 기준만 유지합니다. 오래된 계획, 중복 PRD, 와이어프레임 초안은 코드와 맞지 않으면 문서로 남기지 않습니다.
