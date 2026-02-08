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