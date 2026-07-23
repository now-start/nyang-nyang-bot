-- nyang-nyang-bot target schema specification
-- Dialect: MariaDB / InnoDB / utf8mb4
-- Time contract: TIMESTAMP(6) stores an absolute instant in UTC internally and is
-- read/written through a required Asia/Seoul (+09:00) database session.
-- This file describes the post-cutover schema. It is not a Flyway migration.

CREATE TABLE user_account
(
    user_id       VARCHAR(64) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT 'CHZZK 사용자 식별자',
    display_name  VARCHAR(100)                    NULL COMMENT '현재 표시 이름',
    is_admin      BOOLEAN                         NOT NULL DEFAULT FALSE COMMENT '서비스 관리자 여부',
    last_login_at TIMESTAMP(6)                     NULL COMMENT '마지막 OAuth 로그인 성공 시각',
    created_at    TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at    TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '최종 변경 시각',
    PRIMARY KEY (user_id),
    CONSTRAINT ck_user_account__user_id
        CHECK (CHAR_LENGTH(user_id) BETWEEN 1 AND 64
            AND OCTET_LENGTH(user_id) = OCTET_LENGTH(TRIM(user_id))),
    CONSTRAINT ck_user_account__admin
        CHECK (is_admin IN (FALSE, TRUE))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '서비스가 인지한 CHZZK 사용자 계정';

CREATE TABLE oauth_credential
(
    user_id                 VARCHAR(64) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '인증된 사용자 식별자',
    access_token            TEXT                            NOT NULL COMMENT 'OAuth access token 원문; 로그와 응답에 노출하지 않음',
    refresh_token           TEXT                            NOT NULL COMMENT 'OAuth refresh token 원문; 로그와 응답에 노출하지 않음',
    token_type              VARCHAR(32)                     NOT NULL COMMENT 'OAuth 토큰 유형',
    scope                   TEXT                            NULL COMMENT '승인된 OAuth scope 문자열',
    access_token_expires_at TIMESTAMP(6)                     NOT NULL COMMENT 'access token 만료 시각',
    credential_version      BIGINT                          NOT NULL DEFAULT 0 COMMENT '동시 토큰 갱신 충돌 방지 버전',
    created_at              TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '최초 발급 시각',
    updated_at              TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '최종 갱신 시각',
    PRIMARY KEY (user_id),
    CONSTRAINT fk_oauth_credential__user_account
        FOREIGN KEY (user_id) REFERENCES user_account (user_id) ON DELETE CASCADE,
    CONSTRAINT ck_oauth_credential__version
        CHECK (credential_version >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자별 CHZZK OAuth 자격 증명';

CREATE TABLE command
(
    id                    BIGINT                          NOT NULL AUTO_INCREMENT COMMENT '명령어 식별자',
    trigger_token         VARCHAR(20)                     NOT NULL COMMENT '채팅에서 명령어를 식별하는 토큰',
    message_template      VARCHAR(1000)                   NOT NULL COMMENT '명령 실행 시 렌더링할 메시지 템플릿',
    is_active             BOOLEAN                         NOT NULL DEFAULT TRUE COMMENT '명령어 사용 여부',
    execution_policy      VARCHAR(32)                     NOT NULL DEFAULT 'USER_INTERVAL' COLLATE utf8mb4_nopad_bin
        COMMENT '사용자별 재실행 제한 기준',
    user_cooldown_seconds INTEGER                         NULL DEFAULT 30 COMMENT '사용자별 재실행 제한 시간(초); 달력일 기준이면 NULL',
    created_by_user_id    VARCHAR(64) COLLATE utf8mb4_nopad_bin NULL COMMENT '생성 사용자; NULL은 시스템 생성',
    updated_by_user_id    VARCHAR(64) COLLATE utf8mb4_nopad_bin NULL COMMENT '최종 변경 사용자; NULL은 시스템 변경',
    created_at            TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at            TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '최종 변경 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_command__trigger_token UNIQUE (trigger_token),
    CONSTRAINT fk_command__created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES user_account (user_id) ON DELETE SET NULL,
    CONSTRAINT fk_command__updated_by_user
        FOREIGN KEY (updated_by_user_id) REFERENCES user_account (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_command__trigger_token
        CHECK (CHAR_LENGTH(trigger_token) BETWEEN 2 AND 20),
    CONSTRAINT ck_command__active
        CHECK (is_active IN (FALSE, TRUE)),
    CONSTRAINT ck_command__cooldown
        CHECK ((execution_policy = 'USER_INTERVAL'
                    AND user_cooldown_seconds IS NOT NULL
                    AND user_cooldown_seconds BETWEEN 5 AND 3600)
            OR (execution_policy = 'USER_CALENDAR_DAY'
                    AND user_cooldown_seconds IS NULL))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자 정의 채팅 명령어';

CREATE TABLE command_execution
(
    id                        BIGINT                          NOT NULL AUTO_INCREMENT COMMENT '명령 실행 이벤트 식별자',
    command_id                BIGINT                          NOT NULL COMMENT '실행한 명령어 식별자',
    user_id                   VARCHAR(64) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '명령을 실행한 사용자 식별자',
    executed_at               TIMESTAMP(6)                     NOT NULL COMMENT '명령 실행 확정 절대시각; Asia/Seoul 세션에서 달력일을 판정',
    execution_policy_snapshot VARCHAR(32) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '실행 시점의 사용자별 재실행 제한 기준 스냅샷',
    cooldown_seconds_snapshot INTEGER                         NULL COMMENT '실행 시점의 사용자별 재실행 제한 초 스냅샷; 달력일 기준이면 NULL',
    calendar_date             DATE                            NULL COMMENT 'Asia/Seoul 기준 명령 실행 일자; 달력일 기준에서만 저장',
    PRIMARY KEY (id),
    CONSTRAINT uk_command_execution__command_user_date UNIQUE (command_id, user_id, calendar_date),
    CONSTRAINT fk_command_execution__command
        FOREIGN KEY (command_id) REFERENCES command (id) ON DELETE RESTRICT,
    CONSTRAINT fk_command_execution__user_account
        FOREIGN KEY (user_id) REFERENCES user_account (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_command_execution__policy
        CHECK ((execution_policy_snapshot = 'USER_INTERVAL'
                    AND cooldown_seconds_snapshot IS NOT NULL
                    AND cooldown_seconds_snapshot BETWEEN 5 AND 3600
                    AND calendar_date IS NULL)
            OR (execution_policy_snapshot = 'USER_CALENDAR_DAY'
                    AND cooldown_seconds_snapshot IS NULL
                    AND calendar_date IS NOT NULL)),
    INDEX idx_command_execution__user_command_time (user_id, command_id, executed_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '전체·사용자별 횟수와 일별 연속 실행을 계산하는 명령 실행 이벤트';

CREATE TABLE timer_message
(
    id                         BIGINT                          NOT NULL AUTO_INCREMENT COMMENT '타이머 메시지 식별자',
    message_template           VARCHAR(1000)                   NOT NULL COMMENT '주기적으로 렌더링할 메시지 템플릿',
    interval_minutes           INTEGER                         NOT NULL COMMENT '전송 주기(분)',
    min_chat_count             INTEGER                         NOT NULL COMMENT '전송에 필요한 최소 채팅 수',
    is_active                  BOOLEAN                         NOT NULL DEFAULT FALSE COMMENT '타이머 활성 여부',
    next_run_at                TIMESTAMP(6)                     NULL COMMENT '다음 전송 대상 시각',
    chat_count_since_last_send BIGINT                          NOT NULL DEFAULT 0 COMMENT '마지막 성공 전송 이후 채팅 수',
    claimed_chat_count         BIGINT                          NOT NULL DEFAULT 0 COMMENT '현재 선점 시점의 채팅 수',
    claim_token                VARCHAR(36) COLLATE utf8mb4_nopad_bin NULL COMMENT '실행 선점 UUID',
    claim_expires_at           TIMESTAMP(6)                     NULL COMMENT '실행 선점 만료 시각',
    last_sent_at               TIMESTAMP(6)                     NULL COMMENT '마지막 성공 전송 시각',
    created_by_user_id         VARCHAR(64) COLLATE utf8mb4_nopad_bin NULL COMMENT '생성 사용자; NULL은 시스템 생성',
    updated_by_user_id         VARCHAR(64) COLLATE utf8mb4_nopad_bin NULL COMMENT '최종 변경 사용자; NULL은 시스템 변경',
    created_at                 TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    updated_at                 TIMESTAMP(6)                     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '최종 변경 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_timer_message__created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES user_account (user_id) ON DELETE SET NULL,
    CONSTRAINT fk_timer_message__updated_by_user
        FOREIGN KEY (updated_by_user_id) REFERENCES user_account (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_timer_message__interval
        CHECK (interval_minutes BETWEEN 5 AND 1440),
    CONSTRAINT ck_timer_message__minimum_chat
        CHECK (min_chat_count BETWEEN 1 AND 10000),
    CONSTRAINT ck_timer_message__chat_counts
        CHECK (chat_count_since_last_send >= 0 AND claimed_chat_count >= 0),
    CONSTRAINT ck_timer_message__active
        CHECK (is_active IN (FALSE, TRUE)),
    CONSTRAINT ck_timer_message__claim_pair
        CHECK ((claim_token IS NULL AND claim_expires_at IS NULL)
            OR (claim_token IS NOT NULL AND claim_expires_at IS NOT NULL)),
    CONSTRAINT ck_timer_message__active_schedule
        CHECK (is_active = FALSE OR next_run_at IS NOT NULL),
    INDEX idx_timer_message__due (is_active, next_run_at, claim_expires_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '채팅량과 주기를 기준으로 전송하는 타이머 메시지';

CREATE TABLE point_ledger_entry
(
    id                     BIGINT                           NOT NULL AUTO_INCREMENT COMMENT '포인트 원장 항목 식별자',
    user_id                VARCHAR(64) COLLATE utf8mb4_nopad_bin  NOT NULL COMMENT '포인트 소유 사용자 식별자',
    delta                  BIGINT                           NOT NULL COMMENT '이번 거래의 포인트 증감량',
    source_type            VARCHAR(32) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '포인트 변경 원인 유형',
    source_reference       VARCHAR(191)                     NULL COMMENT '외부 이벤트나 업무 요청의 추적용 참조값',
    description            VARCHAR(500)                     NOT NULL COMMENT '사용자에게 표시 가능한 변경 설명',
    private_note           VARCHAR(500)                     NULL COMMENT '관리자 전용 내부 메모',
    correction_of_entry_id BIGINT                           NULL COMMENT '정정 대상 원장 항목 식별자',
    actor_user_id          VARCHAR(64) COLLATE utf8mb4_nopad_bin  NULL COMMENT '변경 수행 사용자; NULL은 시스템 수행',
    idempotency_key        VARCHAR(191) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '중복 반영 방지 키',
    created_at             TIMESTAMP(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '거래 생성 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_point_ledger_entry__id_user UNIQUE (id, user_id),
    CONSTRAINT uk_point_ledger_entry__idempotency UNIQUE (idempotency_key),
    CONSTRAINT uk_point_ledger_entry__correction UNIQUE (correction_of_entry_id),
    CONSTRAINT fk_point_ledger_entry__user_account
        FOREIGN KEY (user_id) REFERENCES user_account (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_point_ledger_entry__correction
        FOREIGN KEY (correction_of_entry_id, user_id)
            REFERENCES point_ledger_entry (id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_point_ledger_entry__actor_user
        FOREIGN KEY (actor_user_id) REFERENCES user_account (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_point_ledger_entry__delta
        CHECK (delta <> 0),
    CONSTRAINT ck_point_ledger_entry__source
        CHECK (source_type IN (
                               'ADMIN_ADJUSTMENT', 'PRESENCE_REWARD', 'GOOGLE_SHEET_SYNC',
                               'REWARD_MANUAL', 'REWARD_ROULETTE', 'CORRECTION'
            )),
    CONSTRAINT ck_point_ledger_entry__correction_source
        CHECK ((source_type = 'CORRECTION' AND correction_of_entry_id IS NOT NULL)
            OR (source_type <> 'CORRECTION' AND correction_of_entry_id IS NULL)),
    INDEX idx_point_ledger_entry__user_created (user_id, created_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '수정하지 않는 포인트 증감 원장';

CREATE TABLE point_adjustment_preset
(
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '포인트 조정 프리셋 식별자',
    label      VARCHAR(100) NOT NULL COMMENT '관리 화면에 표시할 조정 사유',
    amount     BIGINT       NOT NULL COMMENT '프리셋 선택 시 적용할 증감량',
    created_at TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_point_adjustment_preset__label_amount UNIQUE (label, amount),
    CONSTRAINT ck_point_adjustment_preset__amount
        CHECK (amount <> 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '관리자 포인트 조정에 재사용하는 프리셋';

CREATE TABLE weekly_chat_count
(
    id              BIGINT                          NOT NULL AUTO_INCREMENT COMMENT '주간 채팅 집계 식별자',
    week_start_date DATE                            NOT NULL COMMENT 'Asia/Seoul 기준 집계 주의 월요일',
    user_id         VARCHAR(64) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '채팅 사용자 식별자',
    chat_count      BIGINT                          NOT NULL DEFAULT 0 COMMENT '해당 주의 누적 채팅 수',
    PRIMARY KEY (id),
    CONSTRAINT uk_weekly_chat_count__week_user UNIQUE (week_start_date, user_id),
    CONSTRAINT fk_weekly_chat_count__user_account
        FOREIGN KEY (user_id) REFERENCES user_account (user_id) ON DELETE CASCADE,
    CONSTRAINT ck_weekly_chat_count__monday
        CHECK (DAYOFWEEK(week_start_date) = 2),
    CONSTRAINT ck_weekly_chat_count__value
        CHECK (chat_count >= 0),
    INDEX idx_weekly_chat_count__ranking (week_start_date, chat_count DESC, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'Asia/Seoul 기준 사용자별 주간 채팅 수';

CREATE TABLE donation
(
    id                 BIGINT                           NOT NULL AUTO_INCREMENT COMMENT '후원 식별자',
    ingestion_key      VARCHAR(255) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '수신 프레임별 애플리케이션 생성 식별자 또는 이관된 레거시 이벤트 키',
    donation_type      VARCHAR(32) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT 'CHZZK 후원 이벤트 유형',
    recipient_user_id  VARCHAR(64) COLLATE utf8mb4_nopad_bin  NOT NULL COMMENT '후원을 수신한 방송 사용자 식별자',
    donor_user_id      VARCHAR(64) COLLATE utf8mb4_nopad_bin  NULL COMMENT '후원 사용자 식별자; 익명 또는 미식별이면 NULL',
    donor_display_name VARCHAR(100)                     NULL COMMENT '후원 시점에 수신한 후원자 표시 이름',
    amount             BIGINT                           NOT NULL COMMENT '후원 금액',
    message            LONGTEXT                         NULL COMMENT '후원 메시지 원문',
    received_at        TIMESTAMP(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '후원 이벤트 수신 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_donation__ingestion_key UNIQUE (ingestion_key),
    CONSTRAINT fk_donation__recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES user_account (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_donation__donor_user
        FOREIGN KEY (donor_user_id) REFERENCES user_account (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_donation__amount
        CHECK (amount >= 0),
    INDEX idx_donation__recipient_received (recipient_user_id, received_at, id),
    INDEX idx_donation__donor_received (donor_user_id, received_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'CHZZK에서 수신한 후원 이벤트';

CREATE TABLE roulette_config
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '불변 룰렛 설정 버전 식별자',
    title                VARCHAR(100) NOT NULL COMMENT '관리용 룰렛 설정 이름',
    trigger_token        VARCHAR(20)  NOT NULL COMMENT '후원 메시지에서 룰렛을 시작하는 토큰',
    price_per_round      BIGINT       NOT NULL COMMENT '룰렛 1회당 후원 금액',
    high_round_threshold INTEGER      NOT NULL DEFAULT 100 COMMENT '고회차 경고 기준',
    status               VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COLLATE utf8mb4_nopad_bin COMMENT '설정 생명주기 상태',
    active_slot          TINYINT GENERATED ALWAYS AS (
        CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END
        ) COMMENT '활성 설정을 하나로 제한하는 생성 컬럼',
    created_at           TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '설정 버전 생성 시각',
    updated_at           TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'DRAFT 또는 상태 최종 변경 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_roulette_config__single_active UNIQUE (active_slot),
    CONSTRAINT ck_roulette_config__trigger_token
        CHECK (CHAR_LENGTH(trigger_token) BETWEEN 2 AND 20 AND LEFT(trigger_token, 1) = '!'),
    CONSTRAINT ck_roulette_config__price
        CHECK (price_per_round > 0),
    CONSTRAINT ck_roulette_config__threshold
        CHECK (high_round_threshold > 0),
    CONSTRAINT ck_roulette_config__status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    INDEX idx_roulette_config__status (status, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '활성화 이후 내용을 변경하지 않는 룰렛 설정 버전';

CREATE TABLE roulette_option
(
    id                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '불변 룰렛 옵션 식별자',
    roulette_config_id       BIGINT       NOT NULL COMMENT '소속 룰렛 설정 버전 식별자',
    label                    VARCHAR(100) NOT NULL COMMENT '룰렛 결과에 표시할 옵션 이름',
    probability_basis_points INTEGER      NOT NULL COMMENT '선택 확률; 10000이 100퍼센트',
    is_losing                BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '꽝 옵션 여부',
    reward_type              VARCHAR(32) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '옵션이 부여하는 보상 유형',
    conversion_mode          VARCHAR(16) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '포인트 변환 처리 방식',
    point_delta              BIGINT       NULL COMMENT '포인트 변환 시 적용할 증감량',
    display_order            INTEGER      NOT NULL DEFAULT 0 COMMENT '선택 및 관리 화면 표시 순서',
    created_at               TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '옵션 생성 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_roulette_option__id_config UNIQUE (id, roulette_config_id),
    CONSTRAINT fk_roulette_option__roulette_config
        FOREIGN KEY (roulette_config_id) REFERENCES roulette_config (id) ON DELETE CASCADE,
    CONSTRAINT ck_roulette_option__probability
        CHECK (probability_basis_points BETWEEN 0 AND 10000),
    CONSTRAINT ck_roulette_option__losing_flag
        CHECK (is_losing IN (FALSE, TRUE)),
    CONSTRAINT ck_roulette_option__display_order
        CHECK (display_order >= 0),
    CONSTRAINT ck_roulette_option__reward_type
        CHECK (reward_type IN ('POINT', 'COUPON', 'MISSION', 'PARTICIPATION_PRIORITY', 'CUSTOM')),
    CONSTRAINT ck_roulette_option__conversion_mode
        CHECK (conversion_mode IN ('AUTO', 'MANUAL', 'NONE')),
    CONSTRAINT ck_roulette_option__point_value
        CHECK ((conversion_mode = 'AUTO' AND point_delta IS NOT NULL AND point_delta <> 0)
            OR conversion_mode = 'MANUAL'
            OR (conversion_mode = 'NONE' AND point_delta IS NULL)),
    CONSTRAINT ck_roulette_option__losing
        CHECK (is_losing = FALSE OR (conversion_mode = 'NONE' AND point_delta IS NULL)),
    INDEX idx_roulette_option__config_order (roulette_config_id, display_order, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '특정 불변 룰렛 설정에 속한 선택 옵션';

CREATE TABLE roulette_run
(
    donation_id        BIGINT      NOT NULL COMMENT '룰렛을 시작한 후원 식별자',
    roulette_config_id BIGINT      NOT NULL COMMENT '실행에 사용한 불변 룰렛 설정 식별자',
    status             VARCHAR(16) NOT NULL DEFAULT 'BUILDING' COLLATE utf8mb4_nopad_bin COMMENT '회차 구성 생명주기; BUILDING에서 READY로 한 번만 전환',
    created_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '룰렛 실행 생성 시각',
    updated_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '회차 구성이 READY로 확정된 시각을 포함한 최종 변경 시각',
    PRIMARY KEY (donation_id),
    CONSTRAINT uk_roulette_run__id_config UNIQUE (donation_id, roulette_config_id),
    CONSTRAINT fk_roulette_run__donation
        FOREIGN KEY (donation_id) REFERENCES donation (id) ON DELETE RESTRICT,
    CONSTRAINT fk_roulette_run__roulette_config
        FOREIGN KEY (roulette_config_id) REFERENCES roulette_config (id) ON DELETE RESTRICT,
    CONSTRAINT ck_roulette_run__status
        CHECK (status IN ('BUILDING', 'READY')),
    INDEX idx_roulette_run__config_created (roulette_config_id, created_at, donation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '한 후원에 대해 한 번 생성되는 룰렛 실행';

CREATE TABLE roulette_round
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '룰렛 회차 식별자',
    roulette_run_id    BIGINT       NOT NULL COMMENT '소속 룰렛 실행의 후원 식별자',
    roulette_config_id BIGINT       NOT NULL COMMENT '실행 설정과 선택 옵션의 소속 일치를 보장하는 식별자',
    roulette_option_id BIGINT       NOT NULL COMMENT '선택된 불변 룰렛 옵션 식별자',
    round_no           INTEGER      NOT NULL COMMENT '실행 안에서 1부터 시작하는 회차 번호',
    ticket             INTEGER      NOT NULL COMMENT '1부터 10000 사이의 난수 추첨값',
    status             VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED' COLLATE utf8mb4_nopad_bin COMMENT '회차 보상 처리 상태',
    failure_reason     VARCHAR(500) NULL COMMENT '회차 처리 실패 사유',
    created_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '회차 생성 시각',
    updated_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '회차 상태 최종 변경 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_roulette_round__run_round UNIQUE (roulette_run_id, round_no),
    CONSTRAINT fk_roulette_round__roulette_run
        FOREIGN KEY (roulette_run_id, roulette_config_id)
            REFERENCES roulette_run (donation_id, roulette_config_id) ON DELETE RESTRICT,
    CONSTRAINT fk_roulette_round__roulette_option
        FOREIGN KEY (roulette_option_id, roulette_config_id)
            REFERENCES roulette_option (id, roulette_config_id) ON DELETE RESTRICT,
    CONSTRAINT ck_roulette_round__number
        CHECK (round_no >= 1),
    CONSTRAINT ck_roulette_round__ticket
        CHECK (ticket BETWEEN 1 AND 10000),
    CONSTRAINT ck_roulette_round__status
        CHECK (status IN ('CONFIRMED', 'APPLIED', 'FAILED')),
    CONSTRAINT ck_roulette_round__failure_reason
        CHECK (status = 'FAILED' OR failure_reason IS NULL),
    INDEX idx_roulette_round__option_config (roulette_option_id, roulette_config_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '불변 옵션을 참조하는 룰렛의 개별 추첨 회차';

CREATE TABLE reward_grant
(
    id                    BIGINT                           NOT NULL AUTO_INCREMENT COMMENT '사용자에게 지급된 보상 식별자',
    user_id               VARCHAR(64) COLLATE utf8mb4_nopad_bin  NOT NULL COMMENT '보상을 받은 사용자 식별자',
    roulette_round_id     BIGINT                           NULL COMMENT '룰렛 지급의 원인이 된 회차 식별자',
    point_ledger_entry_id BIGINT                           NULL COMMENT '자동 포인트 전환 원장 항목 식별자',
    label                 VARCHAR(100)                     NOT NULL COMMENT '실제로 지급된 보상 이름',
    reward_type           VARCHAR(32) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '실제로 지급된 보상 유형',
    conversion_mode       VARCHAR(16) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '지급 당시 포인트 변환 처리 방식',
    point_delta           BIGINT                           NULL COMMENT '지급 당시 포인트 변환 증감량',
    status                VARCHAR(16) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '보상 보유 및 처리 상태',
    description           VARCHAR(500)                     NOT NULL COMMENT '사용자에게 표시 가능한 지급 설명',
    private_note          VARCHAR(500)                     NULL COMMENT '관리자 전용 내부 메모',
    actor_user_id         VARCHAR(64) COLLATE utf8mb4_nopad_bin  NULL COMMENT '수동 지급 사용자; NULL은 시스템 지급',
    idempotency_key       VARCHAR(191) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '중복 지급 방지 키',
    created_at            TIMESTAMP(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '지급 시각',
    updated_at            TIMESTAMP(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '상태 최종 변경 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_reward_grant__round UNIQUE (roulette_round_id),
    CONSTRAINT uk_reward_grant__ledger_user UNIQUE (point_ledger_entry_id, user_id),
    CONSTRAINT uk_reward_grant__idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_reward_grant__user_account
        FOREIGN KEY (user_id) REFERENCES user_account (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_reward_grant__roulette_round
        FOREIGN KEY (roulette_round_id) REFERENCES roulette_round (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reward_grant__point_ledger_entry
        FOREIGN KEY (point_ledger_entry_id, user_id)
            REFERENCES point_ledger_entry (id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_reward_grant__actor_user
        FOREIGN KEY (actor_user_id) REFERENCES user_account (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_reward_grant__origin
        CHECK ((roulette_round_id IS NULL AND actor_user_id IS NOT NULL)
            OR (roulette_round_id IS NOT NULL AND actor_user_id IS NULL)),
    CONSTRAINT ck_reward_grant__reward_type
        CHECK (reward_type IN ('POINT', 'COUPON', 'MISSION', 'PARTICIPATION_PRIORITY', 'CUSTOM')),
    CONSTRAINT ck_reward_grant__conversion_mode
        CHECK (conversion_mode IN ('AUTO', 'MANUAL', 'NONE')),
    CONSTRAINT ck_reward_grant__point_value
        CHECK ((conversion_mode = 'AUTO' AND point_delta IS NOT NULL
            AND point_delta <> 0 AND point_ledger_entry_id IS NOT NULL)
            OR (conversion_mode = 'MANUAL' AND point_ledger_entry_id IS NULL)
            OR (conversion_mode = 'NONE' AND point_delta IS NULL
                AND point_ledger_entry_id IS NULL)),
    CONSTRAINT ck_reward_grant__status
        CHECK (status IN ('OWNED', 'USED', 'CONVERTED', 'CORRECTED')),
    CONSTRAINT ck_reward_grant__conversion_status
        CHECK ((conversion_mode = 'AUTO' AND status IN ('CONVERTED', 'CORRECTED'))
            OR (conversion_mode <> 'AUTO' AND status IN ('OWNED', 'USED'))),
    INDEX idx_reward_grant__user_status_created (user_id, status, created_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자에게 실제 지급된 보상 인스턴스';

CREATE TABLE overlay_access_token
(
    id                BIGINT                                         NOT NULL AUTO_INCREMENT COMMENT '오버레이 접근 토큰 식별자',
    token_hash        CHAR(43) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '원문 토큰 SHA-256 해시의 Base64URL 문자열',
    issued_by_user_id VARCHAR(64) COLLATE utf8mb4_nopad_bin                NULL COMMENT '토큰 발급 사용자; NULL은 시스템 발급',
    issued_at         TIMESTAMP(6)                                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '발급 시각',
    revoked_at        TIMESTAMP(6)                                    NULL COMMENT '명시적 폐기 시각',
    active_slot       TINYINT GENERATED ALWAYS AS (
        CASE WHEN revoked_at IS NULL THEN 1 ELSE NULL END
        ) COMMENT '폐기되지 않은 토큰을 하나로 제한하는 생성 컬럼',
    PRIMARY KEY (id),
    CONSTRAINT uk_overlay_access_token__hash UNIQUE (token_hash),
    CONSTRAINT uk_overlay_access_token__single_active UNIQUE (active_slot),
    CONSTRAINT fk_overlay_access_token__issued_by_user
        FOREIGN KEY (issued_by_user_id) REFERENCES user_account (user_id) ON DELETE SET NULL,
    CONSTRAINT ck_overlay_access_token__hash
        CHECK (CHAR_LENGTH(token_hash) = 43 AND token_hash NOT REGEXP '[^A-Za-z0-9_-]'),
    CONSTRAINT ck_overlay_access_token__revocation
        CHECK (revoked_at IS NULL OR revoked_at >= issued_at),
    INDEX idx_overlay_access_token__revoked (revoked_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '브라우저 오버레이 접근에 사용하는 해시 토큰';

CREATE TABLE overlay_display_job
(
    id               BIGINT                           NOT NULL AUTO_INCREMENT COMMENT '오버레이 표시 작업 식별자',
    roulette_run_id  BIGINT                           NOT NULL COMMENT '표시할 룰렛 실행의 후원 식별자',
    replay_of_job_id BIGINT                           NULL COMMENT '재표시 대상 원본 작업 식별자',
    idempotency_key  VARCHAR(191) COLLATE utf8mb4_nopad_bin NOT NULL COMMENT '중복 enqueue 방지 키',
    status           VARCHAR(16) NOT NULL DEFAULT 'PENDING' COLLATE utf8mb4_nopad_bin COMMENT '표시 작업 처리 상태',
    expires_at       TIMESTAMP(6)                      NOT NULL COMMENT '표시 대기 만료 시각',
    claim_token      VARCHAR(36) COLLATE utf8mb4_nopad_bin  NULL COMMENT '표시 작업 선점 UUID',
    claim_expires_at TIMESTAMP(6)                      NULL COMMENT '표시 작업 선점 만료 시각',
    displayed_at     TIMESTAMP(6)                      NULL COMMENT '브라우저 표시 완료 시각',
    created_at       TIMESTAMP(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작업 생성 시각',
    updated_at       TIMESTAMP(6)                      NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '상태 최종 변경 시각',
    PRIMARY KEY (id),
    CONSTRAINT uk_overlay_display_job__idempotency UNIQUE (idempotency_key),
    CONSTRAINT uk_overlay_display_job__id_run UNIQUE (id, roulette_run_id),
    CONSTRAINT fk_overlay_display_job__roulette_run
        FOREIGN KEY (roulette_run_id) REFERENCES roulette_run (donation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_overlay_display_job__replay
        FOREIGN KEY (replay_of_job_id, roulette_run_id)
            REFERENCES overlay_display_job (id, roulette_run_id) ON DELETE RESTRICT,
    CONSTRAINT ck_overlay_display_job__status
        CHECK (status IN ('PENDING', 'DISPLAYING', 'DISPLAYED', 'MISSED')),
    CONSTRAINT ck_overlay_display_job__claim_pair
        CHECK ((status = 'DISPLAYING' AND claim_token IS NOT NULL AND claim_expires_at IS NOT NULL)
            OR (status <> 'DISPLAYING' AND claim_token IS NULL AND claim_expires_at IS NULL)),
    CONSTRAINT ck_overlay_display_job__displayed_at
        CHECK ((status = 'DISPLAYED' AND displayed_at IS NOT NULL)
            OR (status <> 'DISPLAYED' AND displayed_at IS NULL)),
    CONSTRAINT ck_overlay_display_job__lifetime
        CHECK (expires_at > created_at),
    INDEX idx_overlay_display_job__queue (status, created_at, id),
    INDEX idx_overlay_display_job__expiry (status, expires_at, id),
    INDEX idx_overlay_display_job__claim_expiry (status, claim_expires_at, id),
    INDEX idx_overlay_display_job__run_created (roulette_run_id, created_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '오버레이가 원자적으로 선점하여 표시하는 룰렛 작업';
