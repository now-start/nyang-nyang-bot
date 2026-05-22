# 🐾 Nyang-Nyang Bot

Spring Boot 기반 서비스 봇  
CI/CD(GitHub Actions + Docker) 자동 배포

---

## Stack

- Java / Spring Boot
- MySQL
- Docker
- GitHub Actions
- GitHub Container Registry (GHCR)

---

## Repository

- https://github.com/now-start/nyang-nyang-bot

---

## Workflow

Fork → feature/* 개발 → PR → main merge
→ GitHub Actions 자동 실행
→ Build/Test → Docker Image → GHCR Push → Deploy

- 모든 변경은 PR을 통해서만 `main`에 반영
- `main` 병합 이후 배포는 전부 자동

**Failure / Rollback**

- Release 버전을 PreRelease로 변경하면 자동 롤백 수행

---

## Environment (DEV)

## Local Run

로컬 단독 실행은 기본적으로 `local` profile을 사용합니다.

```bash
./gradlew bootRun
```

- 기본 DB는 파일 기반 H2입니다.
- 기본 화면: http://localhost:8080/
- DBeaver JDBC URL: `jdbc:h2:file:/Users/moon/IdeaProjects/nyang-nyang-bot/build/local-h2/nyangnyangbot;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MONTH;AUTO_SERVER=TRUE`
- 로컬에서도 CHZZK OAuth와 채팅 연동을 실제로 사용합니다.

로컬 OAuth 앱 값은 아래 환경변수로 직접 설정해야 합니다.

```bash
export CHZZK_CLIENT_ID=
export CHZZK_CLIENT_SECRET=
export CHZZK_REDIRECT_URI=http://localhost:8080/token
```

Google Sheets 마이그레이션 API를 직접 호출하려면 아래 값도 필요합니다.

```bash
export GOOGLE_SPREADSHEET_ID=
export GOOGLE_SPREADSHEET_KEY='서비스 계정 JSON'
```

### Database

| Key     | Value       |
|---------|-------------|
| DB_URL  | *********** |
| DB_USER | *********** |
| DB_PASS | *********** |

### OAuth

| Key          | Value                       |
|--------------|-----------------------------|
| clientId     | ***********                 |
| clientSecret | ***********                 |
| redirectUrl  | http://localhost:8080/token |

---

## Monitoring

- Grafana
  https://dev-grafana.spring.nowstart.org

---

## Access

| Role | Scope                 |
|------|-----------------------|
| DEV  | 코드, PR, DEV DB, CI 로그 |
| OPS  | Secrets, 배포, 운영 DB    |

---
