CREATE TABLE IF NOT EXISTS authorization_entity (
    channel_id VARCHAR(255) NOT NULL,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    channel_name VARCHAR(255),
    access_token VARCHAR(255),
    refresh_token VARCHAR(255),
    token_type VARCHAR(255),
    expires_in INTEGER,
    scope VARCHAR(255),
    admin BOOLEAN NOT NULL,
    favorite_history_last_seen_at DATETIME(6),
    PRIMARY KEY (channel_id)
);

CREATE TABLE IF NOT EXISTS donation_entity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    donation_type VARCHAR(255),
    channel_id VARCHAR(255),
    donator_channel_id VARCHAR(255),
    donator_nickname VARCHAR(255),
    pay_amount BIGINT,
    donation_text LONGTEXT,
    emojis_json LONGTEXT,
    donation_event_id VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS favorite_adjustment_entity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    amount INTEGER,
    label VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS favorite_entity (
    user_id VARCHAR(255) NOT NULL,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    nick_name VARCHAR(255),
    favorite INTEGER,
    PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS favorite_history_entity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    history VARCHAR(255),
    favorite INTEGER,
    favorite_entity_user_id VARCHAR(255),
    actor_id VARCHAR(255),
    balance_after INTEGER,
    correction_of_ledger_id BIGINT,
    delta INTEGER,
    display_category VARCHAR(255),
    idempotency_key VARCHAR(255),
    nick_name_snapshot VARCHAR(255),
    private_memo VARCHAR(255),
    public_description VARCHAR(255),
    PRIMARY KEY (id),
    KEY FKnjkqgrcjhyhbc9544fedyj0a0 (favorite_entity_user_id),
    CONSTRAINT FKnjkqgrcjhyhbc9544fedyj0a0
        FOREIGN KEY (favorite_entity_user_id) REFERENCES favorite_entity (user_id)
);

CREATE TABLE IF NOT EXISTS subscription_entity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    channel_id VARCHAR(255),
    subscriber_channel_id VARCHAR(255),
    subscriber_nickname VARCHAR(255),
    tier_no INTEGER,
    tier_name VARCHAR(255),
    `month` INTEGER,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS weekly_chat_rank_entity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    week_start_date DATE,
    user_id VARCHAR(255),
    nick_name VARCHAR(255),
    chat_count BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_weekly_chat_rank_week_user UNIQUE (week_start_date, user_id)
);

CREATE INDEX idx_weekly_chat_rank_week
    ON weekly_chat_rank_entity (week_start_date);
