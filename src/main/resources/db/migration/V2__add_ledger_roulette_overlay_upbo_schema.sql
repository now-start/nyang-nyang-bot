ALTER TABLE authorization_entity
    ADD COLUMN IF NOT EXISTS favorite_history_last_seen_at DATETIME(6);

ALTER TABLE donation_entity
    ADD COLUMN IF NOT EXISTS donation_event_id VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS delta INTEGER;

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS balance_after INTEGER;

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS source_id VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS display_category VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS public_description VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS private_memo VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS correction_of_ledger_id BIGINT;

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS actor_id VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

ALTER TABLE favorite_history_entity
    ADD COLUMN IF NOT EXISTS nick_name_snapshot VARCHAR(255);

UPDATE favorite_history_entity
SET balance_after = favorite
WHERE balance_after IS NULL;

UPDATE favorite_history_entity fh
SET delta = (
    SELECT computed.computed_delta
    FROM (
        SELECT
            history.id,
            COALESCE(history.favorite, 0)
                - COALESCE(
                    LAG(history.favorite) OVER (
                        PARTITION BY history.favorite_entity_user_id
                        ORDER BY history.create_date, history.id
                    ),
                    0
                ) AS computed_delta
        FROM favorite_history_entity history
    ) computed
    WHERE computed.id = fh.id
)
WHERE fh.delta IS NULL;

UPDATE favorite_history_entity
SET source_type = 'SHEET_MIGRATION'
WHERE source_type IS NULL;

UPDATE favorite_history_entity
SET source_id = CONCAT('legacy-favorite-history:', id)
WHERE source_id IS NULL;

UPDATE favorite_history_entity
SET display_category = 'MIGRATION'
WHERE display_category IS NULL;

UPDATE favorite_history_entity
SET public_description = COALESCE(history, '데이터 마이그레이션')
WHERE public_description IS NULL;

UPDATE favorite_history_entity
SET idempotency_key = CONCAT('legacy-favorite-history:', id)
WHERE idempotency_key IS NULL;

UPDATE favorite_history_entity
SET nick_name_snapshot = (
    SELECT favorite.nick_name
    FROM favorite_entity favorite
    WHERE favorite.user_id = favorite_history_entity.favorite_entity_user_id
)
WHERE nick_name_snapshot IS NULL
  AND favorite_entity_user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_donation_entity_donation_event_id
    ON donation_entity (donation_event_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_favorite_history_entity_idempotency_key
    ON favorite_history_entity (idempotency_key);

CREATE TABLE IF NOT EXISTS roulette_table (
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

CREATE TABLE IF NOT EXISTS roulette_item (
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
    PRIMARY KEY (id),
    CONSTRAINT fk_roulette_item_table
        FOREIGN KEY (roulette_table_id) REFERENCES roulette_table (id)
);

CREATE TABLE IF NOT EXISTS roulette_event (
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

CREATE TABLE IF NOT EXISTS roulette_round_result (
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
    CONSTRAINT uk_roulette_round_result_event_round UNIQUE (roulette_event_id, round_no),
    CONSTRAINT fk_roulette_round_result_event
        FOREIGN KEY (roulette_event_id) REFERENCES roulette_event (id)
);

CREATE TABLE IF NOT EXISTS overlay_token (
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

CREATE TABLE IF NOT EXISTS overlay_display_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    roulette_event_id BIGINT,
    replay_of_display_event_id BIGINT,
    status VARCHAR(255),
    expires_at DATETIME(6),
    fetched_at DATETIME(6),
    displayed_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_overlay_display_event_roulette_event
        FOREIGN KEY (roulette_event_id) REFERENCES roulette_event (id)
);

CREATE TABLE IF NOT EXISTS upbo_template_entity (
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

CREATE TABLE IF NOT EXISTS user_upbo_entity (
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
    PRIMARY KEY (id),
    CONSTRAINT fk_user_upbo_template
        FOREIGN KEY (upbo_template_id) REFERENCES upbo_template_entity (id)
);

CREATE INDEX IF NOT EXISTS idx_roulette_item_table
    ON roulette_item (roulette_table_id);

CREATE INDEX IF NOT EXISTS idx_roulette_round_result_event
    ON roulette_round_result (roulette_event_id);

CREATE INDEX IF NOT EXISTS idx_overlay_display_event_roulette_event
    ON overlay_display_event (roulette_event_id);

CREATE INDEX IF NOT EXISTS idx_user_upbo_template
    ON user_upbo_entity (upbo_template_id);
