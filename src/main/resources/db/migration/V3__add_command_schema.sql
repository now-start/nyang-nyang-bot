CREATE TABLE command (
    id BIGINT NOT NULL AUTO_INCREMENT,
    create_date DATETIME(6),
    modify_date DATETIME(6),
    type VARCHAR(255),
    trigger_token VARCHAR(255),
    action_key VARCHAR(255),
    message_template LONGTEXT,
    timer_interval_minutes INTEGER,
    timer_min_chat_count INTEGER,
    active BOOLEAN NOT NULL,
    required_role VARCHAR(255),
    user_cooldown_seconds INTEGER,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    PRIMARY KEY (id)
);

INSERT INTO command (
    create_date,
    modify_date,
    type,
    trigger_token,
    action_key,
    active,
    required_role,
    user_cooldown_seconds,
    created_by,
    updated_by
) VALUES (
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'TRIGGER',
    '!호감도',
    'FAVORITE_STATUS',
    TRUE,
    'USER',
    30,
    'system',
    'system'
);

INSERT INTO command (
    create_date,
    modify_date,
    type,
    trigger_token,
    action_key,
    active,
    required_role,
    user_cooldown_seconds,
    created_by,
    updated_by
) VALUES (
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'TRIGGER',
    '!룰렛결과',
    'ROULETTE_RESULT',
    TRUE,
    'USER',
    30,
    'system',
    'system'
);

INSERT INTO command (
    create_date,
    modify_date,
    type,
    trigger_token,
    action_key,
    active,
    required_role,
    user_cooldown_seconds,
    created_by,
    updated_by
)
SELECT
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'TRIGGER',
    LOWER(TRIM(command)),
    'ROULETTE_DONATION',
    TRUE,
    'USER',
    0,
    'system',
    'system'
FROM roulette_table
WHERE command IS NOT NULL
  AND TRIM(command) <> ''
  AND LOWER(TRIM(command)) NOT IN (
      SELECT existing.trigger_token
      FROM (
          SELECT trigger_token
          FROM command
          WHERE trigger_token IS NOT NULL
      ) existing
  )
ORDER BY active DESC, id DESC
LIMIT 1;

INSERT INTO command (
    create_date,
    modify_date,
    type,
    trigger_token,
    action_key,
    active,
    required_role,
    user_cooldown_seconds,
    created_by,
    updated_by
)
SELECT
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'TRIGGER',
    '!룰렛',
    'ROULETTE_DONATION',
    TRUE,
    'USER',
    0,
    'system',
    'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM command
    WHERE action_key = 'ROULETTE_DONATION'
);

UPDATE roulette_table
SET active = FALSE
WHERE active = TRUE
  AND id <> (
      SELECT latest.id
      FROM (
          SELECT MAX(id) AS id
          FROM roulette_table
          WHERE active = TRUE
      ) latest
  );

ALTER TABLE roulette_table
    ADD COLUMN active_slot INTEGER GENERATED ALWAYS AS (CASE WHEN active THEN 1 ELSE NULL END);

CREATE UNIQUE INDEX uk_roulette_table_single_active
    ON roulette_table (active_slot);

CREATE UNIQUE INDEX uk_command_trigger
    ON command (trigger_token);

CREATE UNIQUE INDEX uk_command_action_key
    ON command (action_key);
