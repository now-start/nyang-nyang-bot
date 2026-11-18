# PostgreSQL 6.0 전환

6.x는 PostgreSQL 18 전용이다. 드라이버, Flyway, 쿼리, 로컬 실행, 테스트를 함께 전환한다.
기존 업무 데이터는 삭제하거나 요약하지 않는다. 16개 업무 테이블과 Flyway baseline 이력의
모든 행·컬럼을 포함한다. 원본은 별도 보관하며 대상은 빈 전용 DB만 허용한다.

## 데이터 계약

- ID와 기존 AUTO_INCREMENT 다음 번호를 보존한다. 삭제된 ID도 재사용하지 않는다.
- 원본 TIMESTAMP(6)는 UTC 세션으로 읽고 PostgreSQL timestamptz(6)로 적재한다.
  마이크로초와 실제 시각을 보존하며 달력/화면 기준은 Asia/Seoul이다.
- OAuth 토큰, 문자열, NULL, 정수, 포인트 장부, 생성 컬럼을 포함한 모든 값을 대조한다.
- PK/FK/UNIQUE/CHECK, 생성 컬럼과 외래 키 조회 인덱스를 PostgreSQL 스키마에 구성한다.
- 사용자 ID·상태·중복 방지 키는 C collation, 일반 문자열은 ICU 대소문자/악센트
  비구분 collation을 사용한다. 엔진별 Unicode 정렬 순서 자체는 동일하지 않을 수 있다.
- 기존 Flyway V1 BASELINE 행을 그대로 보존한다. 신규 DB의 V1 SQL도 별도로 검증한다.

## 실행과 검증

`scripts/full_postgresql_transfer.py`는 운영과 분리된 도구 컨테이너에서만 실행한다.
의존성은 PyMySQL 1.1.2, psycopg[binary] 3.2.13이다. 파일은 접근 제한된 서버 폴더에
0600으로 생성하며 원문 데이터와 자격증명을 로그나 Git에 남기지 않는다.

1. 별도 리허설 DB로 전체 복제하고 모든 테이블의 정규화 SHA-256과 행 수를 비교한다.
2. 앱 및 모든 쓰기 작업을 멈추고 실행 컨테이너 종료를 확인한다.
3. `export`로 최종 스냅샷을 생성한다. 읽기 전용 consistent snapshot을 사용한다.
4. `import --database nyang_nyang_bot --schema schema.sql`로 빈 대상에 원자적으로 적재한다.
   새 설치 기본 명령은 원본 명령 데이터로 대체한다. 생성 컬럼은 DB가 재계산하고 대조한다.
5. `verify`로 커밋 후 전체 데이터를 다시 비교하고 `verify-source`로 원본 불변을 확인한다.
6. Config URL/TLS/primary, 세션 시간대를 전환하고 6.x를 배포한다.
7. health, 실제 DB 접속, 오류와 중복 실행 여부를 확인한다.

현재 리허설: 전체 17테이블 8,592행의 전 컬럼 대조 일치. 운영 쓰기가 진행 중인
사전 스냅샷이므로 최종 이관 시 다시 추출하고 결과를 운영 기록에 남긴다.

참고: [PostgreSQL 시간 타입](https://www.postgresql.org/docs/18/datatype-datetime.html),
[문자열 비교 규칙](https://www.postgresql.org/docs/18/collation.html).
