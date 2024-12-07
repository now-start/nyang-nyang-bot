# NyangNyangBot

[![Build and Push Docker Image](https://github.com/now-start/nyang-nyang-bot/actions/workflows/build.yaml/badge.svg)](https://github.com/now-start/nyang-nyang-bot/actions/workflows/build.yaml)

https://chzzk.nowstart.org

## dependency

- Chzzk API
  - chzzk4j:0.0.12
- Play Wright
  - playwright:1.49.0
- Google API
  - google-api-services-sheets:v4-rev20241008-2.0.0
- Security
  - jasypt-spring-boot-starter:3.0.5
  - spring-boot-starter-security
- Repository
  - spring-boot-starter-data-jpa
  - spring-cloud-starter-openfeign
- Admin Page
  - spring-boot-starter-web
  - spring-boot-starter-actuator
  - spring-boot-admin-starter-server
  - spring-boot-admin-starter-client

## Environment

| key             | required |
|-----------------|----------|
| SERVER_BASE_URL | O        |
| DB_URL          | O        |
| DB_USERNAME     | O        |
| DB_PASSWORD     | O        |
| CHZZK_CHANNEL   | O        |
| CHZZK_ID        | O        |
| CHZZK_PASSWORD  | O        |
| GOOGLE_PATH     |          |

## docker-compose

```
version: '3.8'

services:
  nyang-nyang-bot:
    user: root
    image: ghcr.io/now-start/nyang-nyang-bot:latest
    ports:
      - "8080:8080"
    environment:
      - TZ=Asia/Seoul
      - SERVER_BASE_URL=
      - DB_URL=
      - DB_USERNAME=
      - DB_PASSWORD=
      - CHZZK_CHANNEL=
      - CHZZK_ID=
      - CHZZK_PASSWORD=
    volumes:
      - ./resources:/resources
```
