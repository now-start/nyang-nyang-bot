ALTER TABLE favorite_history_entity DROP FOREIGN KEY FKnjkqgrcjhyhbc9544fedyj0a0;

ALTER TABLE favorite_history_entity DROP INDEX FKnjkqgrcjhyhbc9544fedyj0a0;

ALTER TABLE authorization_entity RENAME TO authorization_account;

ALTER TABLE donation_entity RENAME TO donation;

ALTER TABLE favorite_adjustment_entity RENAME TO favorite_adjustment;

ALTER TABLE favorite_entity RENAME TO favorite_account;

ALTER TABLE favorite_history_entity RENAME TO favorite_history;

ALTER TABLE favorite_history
    CHANGE COLUMN favorite_entity_user_id favorite_account_user_id VARCHAR(255);

ALTER TABLE subscription_entity RENAME TO subscription;

ALTER TABLE subscription
    CHANGE COLUMN `month` subscription_month INTEGER;

ALTER TABLE weekly_chat_rank_entity RENAME TO weekly_chat_rank;

UPDATE donation
SET donation_event_id = NULL
WHERE donation_event_id IS NOT NULL
  AND TRIM(donation_event_id) = '';

ALTER TABLE favorite_history
    ADD COLUMN source_type VARCHAR(255);

ALTER TABLE favorite_history
    ADD COLUMN source_id VARCHAR(255);

UPDATE favorite_history
SET balance_after = favorite
WHERE balance_after IS NULL;

UPDATE favorite_history fh
SET delta = (
    SELECT computed.computed_delta
    FROM (
        SELECT
            history.id,
            COALESCE(history.favorite, 0)
                - COALESCE(
                    LAG(history.favorite) OVER (
                        PARTITION BY history.favorite_account_user_id
                        ORDER BY history.create_date, history.id
                    ),
                    0
                ) AS computed_delta
        FROM favorite_history history
    ) computed
    WHERE computed.id = fh.id
)
WHERE fh.delta IS NULL;

UPDATE favorite_history
SET source_type = 'SHEET_MIGRATION'
WHERE source_type IS NULL;

UPDATE favorite_history
SET source_id = CONCAT('legacy-favorite-history:', id)
WHERE source_id IS NULL;

UPDATE favorite_history
SET display_category = 'MIGRATION'
WHERE display_category IS NULL;

UPDATE favorite_history
SET public_description = COALESCE(history, '데이터 마이그레이션')
WHERE public_description IS NULL;

UPDATE favorite_history
SET idempotency_key = NULL
WHERE idempotency_key IS NOT NULL
  AND TRIM(idempotency_key) = '';

UPDATE favorite_history
SET idempotency_key = CONCAT('legacy-favorite-history:', id)
WHERE idempotency_key IS NULL;

UPDATE favorite_history
SET nick_name_snapshot = (
    SELECT favorite.nick_name
    FROM favorite_account favorite
    WHERE favorite.user_id = favorite_history.favorite_account_user_id
)
WHERE nick_name_snapshot IS NULL
  AND favorite_account_user_id IS NOT NULL;

CREATE UNIQUE INDEX uk_donation_donation_event_id
    ON donation (donation_event_id);

CREATE UNIQUE INDEX uk_favorite_history_idempotency_key
    ON favorite_history (idempotency_key);

CREATE TABLE roulette_table (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    title VARCHAR(255),
    command VARCHAR(255),
    price_per_round BIGINT,
    active BOOLEAN NOT NULL,
    version INTEGER,
    high_round_threshold INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE roulette_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    roulette_table_id BIGINT,
    label VARCHAR(255),
    probability_basis_points INTEGER,
    losing_item BOOLEAN NOT NULL,
    reward_type VARCHAR(255),
    conversion_mode VARCHAR(255),
    exchange_favorite_value INTEGER,
    active BOOLEAN NOT NULL,
    display_order INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE roulette_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    donation_event_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    nick_name_snapshot VARCHAR(255),
    donation_amount BIGINT,
    donation_text LONGTEXT,
    roulette_table_id BIGINT,
    roulette_table_version INTEGER,
    command VARCHAR(255),
    price_per_round BIGINT,
    round_count INTEGER,
    items_snapshot_json LONGTEXT,
    status VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_roulette_event_donation_event_id UNIQUE (donation_event_id),
    CONSTRAINT uk_roulette_event_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE roulette_round_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    roulette_event_id BIGINT,
    round_no INTEGER,
    item_label VARCHAR(255),
    probability_basis_points INTEGER,
    losing_item BOOLEAN NOT NULL,
    reward_type VARCHAR(255),
    conversion_mode VARCHAR(255),
    exchange_favorite_value INTEGER,
    status VARCHAR(255),
    ledger_id BIGINT,
    user_upbo_id BIGINT,
    failure_reason VARCHAR(255),
    ticket INTEGER,
    PRIMARY KEY (id),
    CONSTRAINT uk_roulette_round_result_event_round UNIQUE (roulette_event_id, round_no)
);

CREATE TABLE overlay_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    token_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    revoked_at DATETIME(6),
    issued_by VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_overlay_token_token_hash UNIQUE (token_hash)
);

CREATE TABLE overlay_display_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    roulette_event_id BIGINT,
    replay_of_display_event_id BIGINT,
    status VARCHAR(255),
    expires_at DATETIME(6),
    fetched_at DATETIME(6),
    displayed_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE upbo_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    label VARCHAR(255),
    description VARCHAR(255),
    active BOOLEAN NOT NULL,
    display_order INTEGER,
    exchange_favorite_value INTEGER,
    reward_type VARCHAR(255),
    conversion_mode VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE user_upbo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    user_id VARCHAR(255),
    upbo_template_id BIGINT,
    nick_name_snapshot VARCHAR(255),
    label VARCHAR(255),
    status VARCHAR(255),
    exchange_favorite_value INTEGER,
    reward_type VARCHAR(255),
    conversion_mode VARCHAR(255),
    source_type VARCHAR(255),
    ledger_id BIGINT,
    public_description VARCHAR(255),
    private_memo VARCHAR(255),
    actor_id VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX idx_favorite_history_account_create_date
    ON favorite_history (favorite_account_user_id, create_date);

CREATE INDEX idx_roulette_item_table
    ON roulette_item (roulette_table_id);

CREATE INDEX idx_roulette_event_user_create_date
    ON roulette_event (user_id, create_date);

CREATE INDEX idx_roulette_round_result_event
    ON roulette_round_result (roulette_event_id);

CREATE INDEX idx_overlay_display_event_status_expires
    ON overlay_display_event (status, expires_at);

CREATE INDEX idx_overlay_display_event_roulette_event
    ON overlay_display_event (roulette_event_id);

CREATE INDEX idx_user_upbo_user_create_date
    ON user_upbo (user_id, create_date);

CREATE INDEX idx_user_upbo_user_status_create_date
    ON user_upbo (user_id, status, create_date);

CREATE INDEX idx_user_upbo_template
    ON user_upbo (upbo_template_id);
