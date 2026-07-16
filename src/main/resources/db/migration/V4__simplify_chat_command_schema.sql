-- Irreversible cutover: V3 command type/action/timer/role columns are removed.
-- Back up the production database before deployment; a V3 application cannot run after this migration.
-- Existing production databases can still use utf8mb3, but command templates may contain 4-byte emoji.

SET NAMES utf8mb4;

ALTER TABLE command CONVERT TO CHARACTER SET utf8mb4;

UPDATE roulette_table
SET command = (
        SELECT trigger_token
        FROM command
        WHERE action_key = 'ROULETTE_DONATION'
          AND active = TRUE
          AND trigger_token IS NOT NULL
          AND TRIM(trigger_token) <> ''
    ),
    modify_date = CURRENT_TIMESTAMP(6)
WHERE active = TRUE
  AND EXISTS (
      SELECT 1
      FROM command
      WHERE action_key = 'ROULETTE_DONATION'
        AND active = TRUE
        AND trigger_token IS NOT NULL
        AND TRIM(trigger_token) <> ''
  );

UPDATE roulette_table
SET active = FALSE,
    modify_date = CURRENT_TIMESTAMP(6)
WHERE active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM command
      WHERE action_key = 'ROULETTE_DONATION'
        AND active = TRUE
        AND trigger_token IS NOT NULL
        AND TRIM(trigger_token) <> ''
  );

UPDATE command
SET type = 'TEXT',
    action_key = NULL,
    message_template = '{viewer.nickname}님의 호감도는 {favorite.balance} 입니다.💛',
    modify_date = CURRENT_TIMESTAMP(6),
    updated_by = 'system'
WHERE action_key = 'FAVORITE_STATUS';

UPDATE command
SET type = 'TEXT',
    action_key = NULL,
    message_template = '{viewer.nickname}님의 {roulette.recentSummary}',
    modify_date = CURRENT_TIMESTAMP(6),
    updated_by = 'system'
WHERE action_key = 'ROULETTE_RESULT';

UPDATE command
SET message_template = REPLACE(message_template, '{nickname}', '{viewer.nickname}')
WHERE message_template LIKE '%{nickname}%';

UPDATE command
SET message_template = REPLACE(message_template, '{command}', '{invocation.command}')
WHERE message_template LIKE '%{command}%';

UPDATE command
SET message_template = REPLACE(message_template, '{args}', '{invocation.args}')
WHERE message_template LIKE '%{args}%';

UPDATE command
SET message_template = REPLACE(message_template, '{arg1}', '{invocation.arg1}')
WHERE message_template LIKE '%{arg1}%';

UPDATE command
SET message_template = REPLACE(message_template, '{arg2}', '{invocation.arg2}')
WHERE message_template LIKE '%{arg2}%';

UPDATE command
SET message_template = REPLACE(message_template, '{favorite}', '{favorite.balance}')
WHERE message_template LIKE '%{favorite}%';

UPDATE command
SET message_template = REPLACE(message_template, '{datetime}', '{time.datetime}')
WHERE message_template LIKE '%{datetime}%';

UPDATE command
SET message_template = REPLACE(message_template, '{date}', '{time.date}')
WHERE message_template LIKE '%{date}%';

UPDATE command
SET message_template = REPLACE(message_template, '{time}', '{time.time}')
WHERE message_template LIKE '%{time}%';

UPDATE command
SET active = FALSE,
    modify_date = CURRENT_TIMESTAMP(6),
    updated_by = 'system'
WHERE required_role IS NOT NULL
  AND required_role <> 'USER';

UPDATE command
SET user_cooldown_seconds = 30,
    modify_date = CURRENT_TIMESTAMP(6),
    updated_by = 'system'
WHERE user_cooldown_seconds IS NULL
   OR user_cooldown_seconds < 5
   OR user_cooldown_seconds > 3600;

DELETE FROM command
WHERE action_key = 'ROULETTE_DONATION';

DELETE FROM command
WHERE type IS NULL
   OR type <> 'TEXT'
   OR trigger_token IS NULL
   OR TRIM(trigger_token) = ''
   OR message_template IS NULL
   OR TRIM(message_template) = '';

ALTER TABLE command DROP INDEX uk_command_action_key;

ALTER TABLE command DROP COLUMN type;

ALTER TABLE command DROP COLUMN action_key;

ALTER TABLE command DROP COLUMN timer_interval_minutes;

ALTER TABLE command DROP COLUMN timer_min_chat_count;

ALTER TABLE command DROP COLUMN required_role;

ALTER TABLE command MODIFY COLUMN trigger_token VARCHAR(255) NOT NULL;

ALTER TABLE command MODIFY COLUMN message_template LONGTEXT NOT NULL;
