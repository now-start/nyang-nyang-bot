CREATE TABLE timer_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    message_template LONGTEXT NOT NULL,
    interval_minutes INTEGER NOT NULL,
    min_chat_count INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    next_run_at DATETIME(6),
    chat_count_since_last_send BIGINT NOT NULL DEFAULT 0,
    claimed_chat_count BIGINT NOT NULL DEFAULT 0,
    claim_token VARCHAR(36),
    claim_expires_at DATETIME(6),
    last_sent_at DATETIME(6),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_timer_message_due
    ON timer_message (active, next_run_at, claim_expires_at);
