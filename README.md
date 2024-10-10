# Chzzk-favorite-bot

[![Build and Push Docker Image](https://github.com/now-start/chzzk-favorite-bot/actions/workflows/build.yaml/badge.svg)](https://github.com/now-start/chzzk-favorite-bot/actions/workflows/build.yaml)

https://chzzk.nowstart.org

## dependency

- Chzzk API
    - chzzk4j:0.0.9
- Google API
    - google-api-client:1.25.0'
    - google-oauth-client:1.34.1'
    - google-api-services-sheets:v4-rev612-1.25.0'
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

## Enviromnet

| key             | describe          | required |
|-----------------|-------------------|----------|
| PASS            | Encrypt Password  | O        |
| SERVER_BASE_URL | Base URL          | O        |
| PATH            | Google Key        | O        |
| CHANNEL         | Apply Channel     | O        |
| AUT             | Bot Authenticated | O        |
| SES             | Bot Session       | O        |

## docker-compose

```
version: '3.8'

services:
  favorite-bot:
    user: root
    restart: always
    image: ghcr.io/now-start/chzzk-favorite-bot:latest
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Djasypt.encryptor.password=******
      - SERVER_BASE_URL=https://chzzk.nowstart.org
      - KEY=/resources/google_spread_sheet_key.json
    volumes:
      - ./resources:/resources
```
