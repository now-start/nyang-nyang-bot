-- nyang-nyang-bot V6 -> canonical schema preflight
-- Dialect: MariaDB 10.11
-- Read-only: this file contains SELECT statements only.
-- Run against a consistent production snapshot before generating any destructive cutover migration.
-- Unless a section says MANUAL REVIEW, every problem_count must be 0.

-- 1. Schema and Flyway inventory
SELECT
    table_name,
    engine,
    table_collation,
    table_rows,
    table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT 'failed_flyway_migration' AS check_name, COUNT(*) AS problem_count
FROM flyway_schema_history
WHERE success = FALSE;

SELECT 'flyway_v6_not_current' AS check_name,
       CASE
           WHEN SUM(version = '6' AND success = TRUE) = 1
            AND MAX(CASE WHEN version IS NOT NULL AND success = TRUE THEN installed_rank END)
                = MAX(CASE WHEN version = '6' AND success = TRUE THEN installed_rank END)
           THEN 0
           ELSE 1
       END AS problem_count
FROM flyway_schema_history;

SELECT 'missing_required_v6_column' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT 'authorization_account' AS table_name, 'last_login_at' AS column_name
) expected
LEFT JOIN information_schema.columns actual
  ON actual.table_schema = DATABASE()
 AND actual.table_name = expected.table_name
 AND actual.column_name = expected.column_name
WHERE actual.column_name IS NULL;

SELECT 'unexpected_business_table' AS check_name, COUNT(*) AS problem_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_name NOT IN (
      'flyway_schema_history',
      'authorization_account', 'command', 'donation',
      'favorite_account', 'favorite_adjustment', 'favorite_history',
      'overlay_display_event', 'overlay_token',
      'roulette_event', 'roulette_item', 'roulette_round_result', 'roulette_table',
      'subscription', 'timer_message', 'upbo_template', 'user_upbo',
      'weekly_chat_rank'
  );

SELECT 'missing_expected_table' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT 'authorization_account' AS table_name
    UNION ALL SELECT 'command'
    UNION ALL SELECT 'donation'
    UNION ALL SELECT 'favorite_account'
    UNION ALL SELECT 'favorite_adjustment'
    UNION ALL SELECT 'favorite_history'
    UNION ALL SELECT 'overlay_display_event'
    UNION ALL SELECT 'overlay_token'
    UNION ALL SELECT 'roulette_event'
    UNION ALL SELECT 'roulette_item'
    UNION ALL SELECT 'roulette_round_result'
    UNION ALL SELECT 'roulette_table'
    UNION ALL SELECT 'subscription'
    UNION ALL SELECT 'timer_message'
    UNION ALL SELECT 'upbo_template'
    UNION ALL SELECT 'user_upbo'
    UNION ALL SELECT 'weekly_chat_rank'
) expected
LEFT JOIN information_schema.tables actual
  ON actual.table_schema = DATABASE()
 AND actual.table_name = expected.table_name
WHERE actual.table_name IS NULL;

-- 2. Canonical user_account source quality
-- User IDs are preserved byte-for-byte. V8 never trims or folds case; whitespace,
-- case-only, accent-insensitive, or other collation-equivalent variants are blockers.
WITH source_user_id AS (
    SELECT 'authorization_account.channel_id' AS source_name,
           CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS raw_value
    FROM authorization_account
    UNION ALL SELECT 'favorite_account.user_id', CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_account
    UNION ALL SELECT 'favorite_history.account', CONVERT(favorite_account_user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_history
    UNION ALL SELECT 'weekly_chat_rank.user_id', CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM weekly_chat_rank
    UNION ALL SELECT 'donation.channel_id', CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM donation
    UNION ALL SELECT 'donation.donator_channel_id', CONVERT(donator_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM donation WHERE donator_channel_id IS NOT NULL AND TRIM(donator_channel_id) <> ''
    UNION ALL SELECT 'roulette_event.user_id', CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM roulette_event WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
    UNION ALL SELECT 'user_upbo.user_id', CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
    UNION ALL SELECT 'subscription.channel_id', CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM subscription
    UNION ALL SELECT 'subscription.subscriber_channel_id', CONVERT(subscriber_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM subscription
)
SELECT 'user_id_invalid_format' AS check_name, COUNT(*) AS problem_count
FROM source_user_id
WHERE raw_value IS NULL
   OR raw_value = ''
   OR BINARY raw_value <> BINARY TRIM(raw_value)
   OR CHAR_LENGTH(raw_value) > 64;

WITH source_user_id AS (
    SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS raw_value FROM authorization_account
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_account
    UNION ALL SELECT CONVERT(favorite_account_user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_history
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM weekly_chat_rank
    UNION ALL SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM donation
    UNION ALL SELECT CONVERT(donator_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM donation WHERE donator_channel_id IS NOT NULL AND TRIM(donator_channel_id) <> ''
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM roulette_event WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
    UNION ALL SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM subscription
    UNION ALL SELECT CONVERT(subscriber_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM subscription
)
SELECT 'user_id_equivalence_collision' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT LOWER(TRIM(raw_value)) COLLATE utf8mb4_unicode_ci AS equivalence_key
    FROM source_user_id
    WHERE raw_value IS NOT NULL AND raw_value <> ''
    GROUP BY equivalence_key
    HAVING COUNT(DISTINCT HEX(raw_value)) > 1
) collision;

WITH source_display_name AS (
    SELECT 'authorization_account.channel_name' AS source_name, channel_name AS raw_value
    FROM authorization_account
    UNION ALL SELECT 'favorite_account.nick_name', nick_name FROM favorite_account
    UNION ALL SELECT 'weekly_chat_rank.nick_name', nick_name FROM weekly_chat_rank
    UNION ALL SELECT 'donation.donator_nickname', donator_nickname FROM donation
    UNION ALL SELECT 'roulette_event.nick_name_snapshot', nick_name_snapshot FROM roulette_event
    UNION ALL SELECT 'user_upbo.nick_name_snapshot', nick_name_snapshot FROM user_upbo
    UNION ALL SELECT 'subscription.subscriber_nickname', subscriber_nickname FROM subscription
)
SELECT 'display_name_too_long' AS check_name, COUNT(*) AS problem_count
FROM source_display_name
WHERE raw_value IS NOT NULL
  AND CHAR_LENGTH(raw_value) > 100;

-- MANUAL REVIEW: V8 uses the documented timestamp/source/row-id precedence;
-- this inventory makes every display-name conflict visible before cutover.
WITH display_name_candidate AS (
    SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS user_id,
           CONVERT(channel_name USING utf8mb4) COLLATE utf8mb4_nopad_bin AS display_name
    FROM authorization_account
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin,
                     CONVERT(nick_name USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_account
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin,
                     CONVERT(nick_name USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM weekly_chat_rank
    UNION ALL SELECT CONVERT(donator_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin,
                     CONVERT(donator_nickname USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM donation
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin,
                     CONVERT(nick_name_snapshot USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM roulette_event
    UNION ALL SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin,
                     CONVERT(nick_name_snapshot USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
)
SELECT user_id, COUNT(DISTINCT HEX(display_name)) AS display_name_variant_count
FROM display_name_candidate
WHERE user_id IS NOT NULL
  AND user_id <> ''
  AND display_name IS NOT NULL
  AND TRIM(display_name) <> ''
GROUP BY BINARY user_id
HAVING display_name_variant_count > 1
ORDER BY BINARY user_id;

WITH actor AS (
    SELECT 'favorite_history.actor_id' AS source_name,
           CONVERT(actor_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS actor_id
    FROM favorite_history
    UNION ALL SELECT 'user_upbo.actor_id', CONVERT(actor_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
    UNION ALL SELECT 'command.created_by', CONVERT(created_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM command
    UNION ALL SELECT 'command.updated_by', CONVERT(updated_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM command
    UNION ALL SELECT 'timer_message.created_by', CONVERT(created_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM timer_message
    UNION ALL SELECT 'timer_message.updated_by', CONVERT(updated_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM timer_message
    UNION ALL SELECT 'overlay_token.issued_by', CONVERT(issued_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM overlay_token
), canonical_user AS (
    SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS user_id FROM authorization_account
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_account
    UNION SELECT CONVERT(favorite_account_user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_history
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM weekly_chat_rank
    UNION SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM donation
    UNION SELECT CONVERT(donator_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM donation WHERE donator_channel_id IS NOT NULL AND TRIM(donator_channel_id) <> ''
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM roulette_event WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
)
SELECT 'unknown_actor' AS check_name, COUNT(*) AS problem_count
FROM actor
WHERE actor_id IS NOT NULL
  AND TRIM(actor_id) <> ''
  AND LOWER(TRIM(actor_id)) <> 'system'
  AND NOT EXISTS (
      SELECT 1
      FROM canonical_user account
      WHERE BINARY account.user_id = BINARY actor.actor_id
  );

WITH actor AS (
    SELECT CONVERT(actor_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS actor_id FROM favorite_history
    UNION ALL SELECT CONVERT(actor_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
    UNION ALL SELECT CONVERT(created_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM command
    UNION ALL SELECT CONVERT(updated_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM command
    UNION ALL SELECT CONVERT(created_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM timer_message
    UNION ALL SELECT CONVERT(updated_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM timer_message
    UNION ALL SELECT CONVERT(issued_by USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM overlay_token
)
SELECT 'actor_id_invalid_format' AS check_name, COUNT(*) AS problem_count
FROM actor
WHERE actor_id IS NOT NULL
  AND LOWER(TRIM(actor_id)) <> 'system'
  AND (actor_id = ''
       OR BINARY actor_id <> BINARY TRIM(actor_id)
       OR CHAR_LENGTH(actor_id) > 64);

SELECT 'legacy_boolean_domain' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT 1 AS invalid_marker
    FROM authorization_account WHERE admin IS NULL OR admin NOT IN (0, 1)
    UNION ALL SELECT 1 FROM command
      WHERE active IS NULL OR active NOT IN (0, 1)
    UNION ALL SELECT 1 FROM timer_message
      WHERE active IS NULL OR active NOT IN (0, 1)
    UNION ALL SELECT 1 FROM roulette_table
      WHERE active IS NULL OR active NOT IN (0, 1)
    UNION ALL SELECT 1 FROM roulette_item
      WHERE active IS NULL OR active NOT IN (0, 1)
    UNION ALL SELECT 1 FROM roulette_item
      WHERE losing_item IS NULL OR losing_item NOT IN (0, 1)
    UNION ALL SELECT 1 FROM roulette_round_result
      WHERE losing_item IS NULL OR losing_item NOT IN (0, 1)
    UNION ALL SELECT 1 FROM upbo_template
      WHERE active IS NULL OR active NOT IN (0, 1)
    UNION ALL SELECT 1 FROM overlay_token
      WHERE active IS NULL OR active NOT IN (0, 1)
) invalid_boolean;

-- 3. OAuth split
SELECT 'oauth_required_value' AS check_name, COUNT(*) AS problem_count
FROM authorization_account
WHERE channel_id IS NULL
   OR TRIM(channel_id) = ''
   OR access_token IS NULL
   OR TRIM(access_token) = ''
   OR refresh_token IS NULL
   OR TRIM(refresh_token) = ''
   OR token_type IS NULL
   OR TRIM(token_type) = ''
   OR CHAR_LENGTH(token_type) > 32
   OR expires_in IS NULL
   OR expires_in <= 0
   OR create_date IS NULL
   OR modify_date IS NULL;

-- MANUAL REVIEW: scope is retained even though it is not currently used for authorization decisions.
SELECT scope, COUNT(*) AS row_count
FROM authorization_account
GROUP BY scope
ORDER BY row_count DESC, scope;

-- 4. Command and timer constraints
SELECT 'command_invalid' AS check_name, COUNT(*) AS problem_count
FROM command
WHERE trigger_token IS NULL
   OR CHAR_LENGTH(trigger_token) NOT BETWEEN 2 AND 20
   OR trigger_token REGEXP '[[:space:]]'
   OR trigger_token REGEXP '[[:cntrl:]]'
   OR message_template IS NULL
   OR TRIM(message_template) = ''
   OR CHAR_LENGTH(message_template) > 1000
   OR user_cooldown_seconds IS NULL
   OR user_cooldown_seconds NOT BETWEEN 5 AND 3600
   OR create_date IS NULL;

SELECT 'command_target_collation_duplicate' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT CONVERT(trigger_token USING utf8mb4) COLLATE utf8mb4_unicode_ci AS target_token
    FROM command
    GROUP BY target_token
    HAVING COUNT(*) > 1
) duplicate_command;

SELECT 'timer_message_invalid' AS check_name, COUNT(*) AS problem_count
FROM timer_message
WHERE message_template IS NULL
   OR TRIM(message_template) = ''
   OR CHAR_LENGTH(message_template) > 1000
   OR interval_minutes IS NULL
   OR interval_minutes NOT BETWEEN 5 AND 1440
   OR min_chat_count IS NULL
   OR min_chat_count NOT BETWEEN 1 AND 10000
   OR chat_count_since_last_send IS NULL
   OR chat_count_since_last_send < 0
   OR claimed_chat_count IS NULL
   OR claimed_chat_count < 0
   OR ((claim_token IS NULL) <> (claim_expires_at IS NULL))
   OR (active = TRUE AND next_run_at IS NULL)
   OR create_date IS NULL;

-- MANUAL REVIEW: every row counted here must be deterministically rewritten by V8.
SELECT 'legacy_point_variable_template' AS check_name, COUNT(*) AS rewrite_count
FROM (
    SELECT id FROM command WHERE message_template LIKE '%{favorite.balance}%'
    UNION ALL
    SELECT id FROM timer_message WHERE message_template LIKE '%{favorite.balance}%'
) legacy_template;

-- 5. Point account, ledger, and preset
SELECT 'point_account_invalid' AS check_name, COUNT(*) AS problem_count
FROM favorite_account
WHERE user_id IS NULL
   OR TRIM(user_id) = ''
   OR favorite IS NULL
   OR create_date IS NULL;

SELECT 'point_ledger_required_value' AS check_name, COUNT(*) AS problem_count
FROM favorite_history
WHERE favorite_account_user_id IS NULL
   OR TRIM(favorite_account_user_id) = ''
   OR delta IS NULL
   OR delta = 0
   OR balance_after IS NULL
   OR source_type IS NULL
   OR BINARY source_type NOT IN (
       'ADMIN_ADJUSTMENT', 'ATTENDANCE', 'SHEET_MIGRATION',
       'UPBO_MANUAL', 'UPBO_ROULETTE', 'CORRECTION'
   )
   OR idempotency_key IS NULL
   OR TRIM(idempotency_key) = ''
   OR CHAR_LENGTH(idempotency_key) > 191
   OR (COALESCE(NULLIF(TRIM(public_description), ''), NULLIF(TRIM(history), '')) IS NULL)
   OR CHAR_LENGTH(
       COALESCE(NULLIF(TRIM(public_description), ''), NULLIF(TRIM(history), ''))
   ) > 500
   OR CHAR_LENGTH(source_id) > 191
   OR CHAR_LENGTH(private_memo) > 500
   OR create_date IS NULL;

-- MANUAL REVIEW: public_description is canonical; history is used only when it is blank.
SELECT COUNT(*) AS point_ledger_description_conflict_count
FROM favorite_history
WHERE public_description IS NOT NULL
  AND TRIM(public_description) <> ''
  AND history IS NOT NULL
  AND TRIM(history) <> ''
  AND BINARY TRIM(public_description) <> BINARY TRIM(history);

SELECT 'point_ledger_duplicate_idempotency' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT idempotency_key
    FROM favorite_history
    WHERE idempotency_key IS NOT NULL
      AND TRIM(idempotency_key) <> ''
    GROUP BY idempotency_key
    HAVING COUNT(*) > 1
) duplicate_key;

SELECT 'point_ledger_orphan_account' AS check_name, COUNT(*) AS problem_count
FROM favorite_history ledger
LEFT JOIN favorite_account account
  ON BINARY account.user_id = BINARY ledger.favorite_account_user_id
WHERE account.user_id IS NULL;

SELECT 'point_ledger_legacy_balance_mismatch' AS check_name, COUNT(*) AS problem_count
FROM favorite_history
WHERE favorite IS NOT NULL
  AND balance_after IS NOT NULL
  AND favorite <> balance_after;

SELECT 'point_ledger_correction_invalid' AS check_name, COUNT(*) AS problem_count
FROM favorite_history ledger
LEFT JOIN favorite_history corrected
  ON corrected.id = ledger.correction_of_ledger_id
WHERE (BINARY ledger.source_type = BINARY 'CORRECTION' AND ledger.correction_of_ledger_id IS NULL)
   OR (BINARY ledger.source_type <> BINARY 'CORRECTION' AND ledger.correction_of_ledger_id IS NOT NULL)
   OR ledger.correction_of_ledger_id = ledger.id
   OR (ledger.correction_of_ledger_id IS NOT NULL AND corrected.id IS NULL)
   OR (corrected.id IS NOT NULL
       AND BINARY corrected.favorite_account_user_id
           <> BINARY ledger.favorite_account_user_id)
   OR ledger.correction_of_ledger_id >= ledger.id;

SELECT 'point_ledger_duplicate_correction' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT correction_of_ledger_id
    FROM favorite_history
    WHERE correction_of_ledger_id IS NOT NULL
    GROUP BY correction_of_ledger_id
    HAVING COUNT(*) > 1
) duplicate_correction;

WITH ordered_ledger AS (
    SELECT
        id,
        favorite_account_user_id,
        delta,
        balance_after,
        LAG(balance_after) OVER (
            PARTITION BY BINARY favorite_account_user_id
            ORDER BY create_date, id
        ) AS previous_balance
    FROM favorite_history
)
SELECT 'point_ledger_chain_mismatch' AS check_name, COUNT(*) AS problem_count
FROM ordered_ledger
WHERE COALESCE(previous_balance, 0) + delta <> balance_after;

WITH latest_ledger AS (
    SELECT
        favorite_account_user_id AS user_id,
        balance_after,
        ROW_NUMBER() OVER (
            PARTITION BY BINARY favorite_account_user_id
            ORDER BY create_date DESC, id DESC
        ) AS row_no
    FROM favorite_history
)
SELECT 'point_account_latest_ledger_mismatch' AS check_name, COUNT(*) AS problem_count
FROM favorite_account account
JOIN latest_ledger ledger
  ON BINARY ledger.user_id = BINARY account.user_id
 AND ledger.row_no = 1
WHERE account.favorite <> ledger.balance_after;

SELECT 'point_account_without_ledger' AS check_name, COUNT(*) AS problem_count
FROM favorite_account account
WHERE account.favorite <> 0
  AND NOT EXISTS (
    SELECT 1
    FROM favorite_history ledger
    WHERE BINARY ledger.favorite_account_user_id = BINARY account.user_id
);

-- MANUAL REVIEW: valid empty accounts are retained without inventing a zero-delta ledger row.
SELECT COUNT(*) AS zero_balance_point_account_without_ledger_count
FROM favorite_account account
WHERE account.favorite = 0
  AND NOT EXISTS (
      SELECT 1
      FROM favorite_history ledger
      WHERE BINARY ledger.favorite_account_user_id = BINARY account.user_id
  );

SELECT 'point_adjustment_preset_invalid' AS check_name, COUNT(*) AS problem_count
FROM favorite_adjustment
WHERE label IS NULL
   OR TRIM(label) = ''
   OR CHAR_LENGTH(label) > 100
   OR amount IS NULL
   OR amount = 0
   OR create_date IS NULL;

SELECT 'point_adjustment_preset_duplicate' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT CONVERT(label USING utf8mb4) COLLATE utf8mb4_unicode_ci AS target_label,
           amount
    FROM favorite_adjustment
    GROUP BY target_label, amount
    HAVING COUNT(*) > 1
) duplicate_preset;

-- MANUAL REVIEW: category is removed only after confirming source_type fully explains it.
SELECT source_type, display_category, COUNT(*) AS row_count
FROM favorite_history
GROUP BY source_type, display_category
ORDER BY source_type, display_category;

-- 6. Weekly chat
SELECT 'weekly_chat_count_invalid' AS check_name, COUNT(*) AS problem_count
FROM weekly_chat_rank
WHERE week_start_date IS NULL
   OR DAYOFWEEK(week_start_date) <> 2
   OR user_id IS NULL
   OR TRIM(user_id) = ''
   OR chat_count IS NULL
   OR chat_count < 0;

-- Existing ATTENDANCE ledger events become daily facts using the stored local DATETIME
-- as Asia/Seoul wall time. Multiple grants for one user/day are ambiguous and block cutover.
SELECT 'attendance_duplicate_user_day' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT favorite_account_user_id, DATE(create_date) AS attendance_date
    FROM favorite_history
    WHERE BINARY source_type = BINARY 'ATTENDANCE'
    GROUP BY BINARY favorite_account_user_id, DATE(create_date)
    HAVING COUNT(*) > 1
) duplicate_attendance;

-- MANUAL REVIEW: this is the exact historical attendance set V8 will preserve.
SELECT
    COUNT(*) AS attendance_source_row_count,
    COUNT(DISTINCT HEX(favorite_account_user_id)) AS attendance_user_count,
    MIN(DATE(create_date)) AS first_attendance_date,
    MAX(DATE(create_date)) AS last_attendance_date
FROM favorite_history
WHERE BINARY source_type = BINARY 'ATTENDANCE';

-- 7. Donation
SELECT 'donation_required_value' AS check_name, COUNT(*) AS problem_count
FROM donation
WHERE donation_event_id IS NULL
   OR TRIM(donation_event_id) = ''
   OR CHAR_LENGTH(donation_event_id) > 128
   OR donation_type IS NULL
   OR TRIM(donation_type) = ''
   OR CHAR_LENGTH(donation_type) > 32
   OR channel_id IS NULL
   OR TRIM(channel_id) = ''
   OR pay_amount IS NULL
   OR pay_amount < 0
   OR CHAR_LENGTH(donator_nickname) > 100
   OR create_date IS NULL;

SELECT 'donation_duplicate_external_event' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT donation_event_id
    FROM donation
    WHERE donation_event_id IS NOT NULL
      AND TRIM(donation_event_id) <> ''
    GROUP BY donation_event_id
    HAVING COUNT(*) > 1
) duplicate_event;

-- MANUAL REVIEW: blank donor ID is normalized to NULL, not converted to a fake user.
SELECT COUNT(*) AS blank_donor_user_id_count
FROM donation
WHERE donator_channel_id IS NOT NULL
  AND TRIM(donator_channel_id) = '';

-- MANUAL REVIEW: this payload is deliberately dropped because no read path consumes it.
SELECT COUNT(*) AS donation_emojis_json_drop_count
FROM donation
WHERE emojis_json IS NOT NULL
  AND TRIM(emojis_json) <> '';

-- 8. Roulette config, option, run, and round
SELECT 'roulette_config_invalid' AS check_name, COUNT(*) AS problem_count
FROM roulette_table
WHERE title IS NULL
   OR TRIM(title) = ''
   OR CHAR_LENGTH(title) > 100
   OR command IS NULL
   OR TRIM(command) = ''
   OR CHAR_LENGTH(command) NOT BETWEEN 2 AND 20
   OR LEFT(command, 1) <> '!'
   OR price_per_round IS NULL
   OR price_per_round <= 0
   OR high_round_threshold IS NULL
   OR high_round_threshold <= 0
   OR create_date IS NULL;

SELECT 'roulette_multiple_active_config' AS check_name,
       GREATEST(COUNT(*) - 1, 0) AS problem_count
FROM roulette_table
WHERE active = TRUE;

SELECT 'roulette_option_invalid' AS check_name, COUNT(*) AS problem_count
FROM roulette_item item
LEFT JOIN roulette_table config ON config.id = item.roulette_table_id
WHERE config.id IS NULL
   OR item.label IS NULL
   OR TRIM(item.label) = ''
   OR CHAR_LENGTH(item.label) > 100
   OR item.probability_basis_points IS NULL
   OR item.probability_basis_points NOT BETWEEN 0 AND 10000
   OR item.reward_type IS NULL
   OR BINARY item.reward_type NOT IN ('FAVORITE', 'COUPON', 'MISSION', 'PARTICIPATION_PRIORITY', 'CUSTOM')
   OR item.conversion_mode IS NULL
   OR BINARY item.conversion_mode NOT IN ('AUTO', 'MANUAL', 'NONE')
   OR (BINARY item.conversion_mode = BINARY 'AUTO'
       AND (item.exchange_favorite_value IS NULL OR item.exchange_favorite_value = 0))
   OR (BINARY item.conversion_mode = BINARY 'NONE'
       AND COALESCE(item.exchange_favorite_value, 0) <> 0)
   OR (item.losing_item = TRUE
       AND (BINARY item.conversion_mode <> BINARY 'NONE'
            OR COALESCE(item.exchange_favorite_value, 0) <> 0))
   OR item.display_order IS NULL
   OR item.display_order < 0
   OR item.create_date IS NULL;

SELECT 'inactive_roulette_option' AS check_name, COUNT(*) AS problem_count
FROM roulette_item
WHERE active = FALSE;

SELECT 'active_roulette_probability_total' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT config.id
    FROM roulette_table config
    LEFT JOIN roulette_item item
      ON item.roulette_table_id = config.id
     AND item.active = TRUE
    WHERE config.active = TRUE
    GROUP BY config.id
    HAVING COALESCE(SUM(item.probability_basis_points), 0) <> 10000
       OR SUM(item.losing_item = TRUE AND item.probability_basis_points > 0) = 0
) invalid_active_config;

SELECT 'roulette_run_source_invalid' AS check_name, COUNT(*) AS problem_count
FROM roulette_event event
WHERE event.donation_event_id IS NULL
   OR TRIM(event.donation_event_id) = ''
   OR event.items_snapshot_json IS NULL
   OR JSON_VALID(event.items_snapshot_json) <> 1
   OR JSON_TYPE(event.items_snapshot_json) <> 'ARRAY'
   OR JSON_LENGTH(event.items_snapshot_json) < 1
   OR event.command IS NULL
   OR TRIM(event.command) = ''
   OR CHAR_LENGTH(event.command) NOT BETWEEN 2 AND 20
   OR LEFT(event.command, 1) <> '!'
   OR event.price_per_round IS NULL
   OR event.price_per_round <= 0
   OR event.donation_amount IS NULL
   OR event.donation_amount < 0
   OR event.round_count IS NULL
   OR event.round_count < 1
   OR event.create_date IS NULL;

SELECT 'roulette_run_policy_round_count_mismatch' AS check_name, COUNT(*) AS problem_count
FROM roulette_event event
WHERE event.price_per_round > 0
  AND event.donation_amount IS NOT NULL
  AND event.round_count IS NOT NULL
  AND event.round_count <> FLOOR(event.donation_amount / event.price_per_round);

SELECT 'roulette_run_donation_mapping' AS check_name, COUNT(*) AS problem_count
FROM roulette_event event
LEFT JOIN donation
  ON BINARY donation.donation_event_id = BINARY event.donation_event_id
WHERE donation.id IS NULL;

SELECT 'roulette_run_anonymous_donor' AS check_name, COUNT(*) AS problem_count
FROM roulette_event event
JOIN donation
  ON BINARY donation.donation_event_id = BINARY event.donation_event_id
WHERE donation.donator_channel_id IS NULL
   OR TRIM(donation.donator_channel_id) = '';

SELECT 'roulette_run_donation_fact_mismatch' AS check_name, COUNT(*) AS problem_count
FROM roulette_event event
JOIN donation
  ON BINARY donation.donation_event_id = BINARY event.donation_event_id
WHERE NOT (
        BINARY NULLIF(TRIM(event.user_id), '')
        <=> BINARY NULLIF(TRIM(donation.donator_channel_id), '')
      )
   OR NOT (BINARY event.nick_name_snapshot <=> BINARY donation.donator_nickname)
   OR NOT (event.donation_amount <=> donation.pay_amount)
   OR NOT (BINARY event.donation_text <=> BINARY donation.donation_text);

SELECT 'roulette_run_round_count_mismatch' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT event.id
    FROM roulette_event event
    LEFT JOIN roulette_round_result round_result
      ON round_result.roulette_event_id = event.id
    GROUP BY event.id, event.round_count
    HAVING event.round_count <> COUNT(round_result.id)
        OR COALESCE(MIN(round_result.round_no), 0) <> 1
        OR COALESCE(MAX(round_result.round_no), 0) <> event.round_count
        OR COUNT(DISTINCT round_result.round_no) <> COUNT(round_result.id)
) mismatched_event;

SELECT 'roulette_round_invalid' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
LEFT JOIN roulette_event event ON event.id = round_result.roulette_event_id
WHERE event.id IS NULL
   OR round_result.round_no IS NULL
   OR round_result.round_no < 1
   OR round_result.item_label IS NULL
   OR TRIM(round_result.item_label) = ''
   OR CHAR_LENGTH(round_result.item_label) > 100
   OR round_result.probability_basis_points IS NULL
   OR round_result.probability_basis_points NOT BETWEEN 0 AND 10000
   OR round_result.reward_type IS NULL
   OR BINARY round_result.reward_type NOT IN ('FAVORITE', 'COUPON', 'MISSION', 'PARTICIPATION_PRIORITY', 'CUSTOM')
   OR round_result.conversion_mode IS NULL
   OR BINARY round_result.conversion_mode NOT IN ('AUTO', 'MANUAL', 'NONE')
   OR (BINARY round_result.conversion_mode = BINARY 'AUTO'
       AND (round_result.exchange_favorite_value IS NULL
            OR round_result.exchange_favorite_value = 0))
   OR (BINARY round_result.conversion_mode = BINARY 'NONE'
       AND COALESCE(round_result.exchange_favorite_value, 0) <> 0)
   OR (round_result.losing_item = TRUE
       AND (BINARY round_result.conversion_mode <> BINARY 'NONE'
            OR COALESCE(round_result.exchange_favorite_value, 0) <> 0))
   OR round_result.ticket IS NULL
   OR round_result.ticket NOT BETWEEN 1 AND 10000
   OR round_result.status IS NULL
   OR BINARY round_result.status NOT IN ('CONFIRMED', 'APPLIED', 'FAILED')
   OR (BINARY round_result.status <> BINARY 'FAILED' AND round_result.failure_reason IS NOT NULL)
   OR CHAR_LENGTH(round_result.failure_reason) > 500
   OR round_result.create_date IS NULL;

SELECT 'roulette_round_orphan_ledger' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
LEFT JOIN favorite_history ledger ON ledger.id = round_result.ledger_id
WHERE round_result.ledger_id IS NOT NULL
  AND ledger.id IS NULL;

SELECT 'roulette_round_orphan_reward' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
LEFT JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
WHERE round_result.user_upbo_id IS NOT NULL
  AND reward.id IS NULL;

SELECT 'roulette_round_reward_state_mismatch' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
LEFT JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
LEFT JOIN favorite_history ledger ON ledger.id = round_result.ledger_id
WHERE (BINARY round_result.status = BINARY 'APPLIED'
       AND round_result.losing_item = TRUE
       AND (round_result.user_upbo_id IS NOT NULL OR round_result.ledger_id IS NOT NULL))
   OR (BINARY round_result.status = BINARY 'APPLIED'
       AND round_result.losing_item = FALSE
       AND reward.id IS NULL)
   OR (BINARY round_result.status IN ('CONFIRMED', 'FAILED')
       AND (round_result.user_upbo_id IS NOT NULL OR round_result.ledger_id IS NOT NULL))
   OR (BINARY round_result.status = BINARY 'APPLIED'
       AND round_result.losing_item = FALSE
       AND BINARY round_result.conversion_mode = BINARY 'AUTO'
       AND ledger.id IS NULL)
   OR (BINARY round_result.status = BINARY 'APPLIED'
       AND round_result.losing_item = FALSE
       AND BINARY round_result.conversion_mode <> BINARY 'AUTO'
       AND round_result.ledger_id IS NOT NULL);

-- Java migration must prove each round matches exactly one option in its event JSON,
-- validate the same losing/NONE/null rule, and translate legacy FAVORITE to POINT.

-- 9. Reward grant
-- INFORMATIONAL: template selection has no production input adapter and the target
-- stores complete grant facts, so unreferenced catalog rows are intentionally not migrated.
SELECT COUNT(*) AS discarded_upbo_template_count
FROM upbo_template;

-- MUST BE ZERO: a linked template ID is source provenance that the 17-table target
-- cannot preserve. Do not cut over until the target design is revised or the link is
-- explicitly retained through an approved migration plan.
SELECT 'reward_template_provenance_present' AS check_name, COUNT(*) AS problem_count
FROM user_upbo
WHERE upbo_template_id IS NOT NULL;

SELECT 'reward_grant_invalid' AS check_name, COUNT(*) AS problem_count
FROM user_upbo reward
LEFT JOIN favorite_history ledger ON ledger.id = reward.ledger_id
WHERE reward.user_id IS NULL
   OR TRIM(reward.user_id) = ''
   OR reward.label IS NULL
   OR TRIM(reward.label) = ''
   OR CHAR_LENGTH(reward.label) > 100
   OR reward.status IS NULL
   OR BINARY reward.status NOT IN ('OWNED', 'USED', 'CONVERTED', 'CORRECTED')
   OR reward.reward_type IS NULL
   OR BINARY reward.reward_type NOT IN ('FAVORITE', 'COUPON', 'MISSION', 'PARTICIPATION_PRIORITY', 'CUSTOM')
   OR reward.conversion_mode IS NULL
   OR BINARY reward.conversion_mode NOT IN ('AUTO', 'MANUAL', 'NONE')
   OR reward.source_type IS NULL
   OR BINARY reward.source_type NOT IN ('UPBO_MANUAL', 'UPBO_ROULETTE')
   OR (reward.ledger_id IS NOT NULL AND ledger.id IS NULL)
   OR (BINARY reward.conversion_mode = BINARY 'AUTO'
       AND (reward.exchange_favorite_value IS NULL
            OR reward.exchange_favorite_value = 0
            OR reward.ledger_id IS NULL))
   OR (BINARY reward.conversion_mode = BINARY 'NONE'
       AND COALESCE(reward.exchange_favorite_value, 0) <> 0)
   OR (BINARY reward.conversion_mode <> BINARY 'AUTO' AND reward.ledger_id IS NOT NULL)
   OR (BINARY reward.conversion_mode = BINARY 'AUTO'
       AND BINARY reward.status NOT IN ('CONVERTED', 'CORRECTED'))
   OR (BINARY reward.conversion_mode <> BINARY 'AUTO'
       AND BINARY reward.status NOT IN ('OWNED', 'USED'))
   OR (BINARY reward.source_type = BINARY 'UPBO_MANUAL'
       AND (reward.actor_id IS NULL
            OR TRIM(reward.actor_id) = ''
            OR LOWER(TRIM(reward.actor_id)) = 'system'))
   OR (BINARY reward.source_type = BINARY 'UPBO_ROULETTE'
       AND reward.actor_id IS NOT NULL
       AND TRIM(reward.actor_id) <> ''
       AND LOWER(TRIM(reward.actor_id)) <> 'system')
   OR reward.public_description IS NULL
   OR TRIM(reward.public_description) = ''
   OR CHAR_LENGTH(reward.public_description) > 500
   OR CHAR_LENGTH(reward.private_memo) > 500
   OR reward.create_date IS NULL;

SELECT 'reward_grant_round_mapping' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT
        reward.id,
        reward.source_type,
        COUNT(round_result.id) AS round_link_count
    FROM user_upbo reward
    LEFT JOIN roulette_round_result round_result
      ON round_result.user_upbo_id = reward.id
    GROUP BY reward.id, reward.source_type
    HAVING (BINARY reward.source_type = BINARY 'UPBO_ROULETTE' AND round_link_count <> 1)
        OR (BINARY reward.source_type = BINARY 'UPBO_MANUAL' AND round_link_count <> 0)
) invalid_round_link;

SELECT 'reward_grant_round_ledger_mismatch' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
WHERE BINARY reward.source_type <> BINARY 'UPBO_ROULETTE'
   OR NOT (reward.ledger_id <=> round_result.ledger_id);

SELECT 'reward_grant_round_fact_mismatch' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
WHERE NOT (BINARY reward.label <=> BINARY round_result.item_label)
   OR NOT (BINARY reward.reward_type <=> BINARY round_result.reward_type)
   OR NOT (BINARY reward.conversion_mode <=> BINARY round_result.conversion_mode)
   OR NOT (
       CASE
           WHEN BINARY reward.conversion_mode = BINARY 'NONE' THEN NULL
           ELSE reward.exchange_favorite_value
       END
       <=>
       CASE
           WHEN BINARY round_result.conversion_mode = BINARY 'NONE' THEN NULL
           ELSE round_result.exchange_favorite_value
       END
   );

SELECT 'reward_grant_ledger_fact_mismatch' AS check_name, COUNT(*) AS problem_count
FROM user_upbo reward
JOIN favorite_history ledger ON ledger.id = reward.ledger_id
WHERE BINARY reward.user_id <> BINARY ledger.favorite_account_user_id
   OR NOT (reward.exchange_favorite_value <=> ledger.delta)
   OR (BINARY reward.source_type = BINARY 'UPBO_MANUAL'
       AND BINARY ledger.source_type <> BINARY 'UPBO_MANUAL')
   OR (BINARY reward.source_type = BINARY 'UPBO_ROULETTE'
       AND BINARY ledger.source_type <> BINARY 'UPBO_ROULETTE');

SELECT 'roulette_reward_recipient_mismatch' AS check_name, COUNT(*) AS problem_count
FROM roulette_round_result round_result
JOIN roulette_event event ON event.id = round_result.roulette_event_id
JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
JOIN donation ON BINARY donation.donation_event_id = BINARY event.donation_event_id
WHERE BINARY reward.source_type = BINARY 'UPBO_ROULETTE'
  AND NOT (
      BINARY reward.user_id
      <=> BINARY NULLIF(TRIM(donation.donator_channel_id), '')
  );

SELECT 'reward_grant_duplicate_ledger' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT ledger_id
    FROM user_upbo
    WHERE ledger_id IS NOT NULL
    GROUP BY ledger_id
    HAVING COUNT(*) > 1
) duplicate_ledger;

SELECT 'roulette_round_duplicate_reward' AS check_name, COUNT(*) AS problem_count
FROM (
    SELECT user_upbo_id
    FROM roulette_round_result
    WHERE user_upbo_id IS NOT NULL
    GROUP BY user_upbo_id
    HAVING COUNT(*) > 1
) duplicate_reward;

-- 10. Overlay
SELECT 'overlay_token_invalid' AS check_name, COUNT(*) AS problem_count
FROM overlay_token
WHERE token_hash IS NULL
   OR CHAR_LENGTH(token_hash) <> 43
   OR token_hash REGEXP '[^A-Za-z0-9_-]'
   OR create_date IS NULL
   OR (active = TRUE AND revoked_at IS NOT NULL)
   OR (active = FALSE
       AND COALESCE(revoked_at, modify_date, create_date) < create_date);

SELECT 'overlay_multiple_active_token' AS check_name,
       GREATEST(COUNT(*) - 1, 0) AS problem_count
FROM overlay_token
WHERE active = TRUE;

SELECT 'overlay_display_job_invalid' AS check_name, COUNT(*) AS problem_count
FROM overlay_display_event job
LEFT JOIN roulette_event event ON event.id = job.roulette_event_id
LEFT JOIN overlay_display_event replay ON replay.id = job.replay_of_display_event_id
WHERE event.id IS NULL
   OR job.status IS NULL
   OR BINARY job.status NOT IN ('PENDING', 'DISPLAYING', 'DISPLAYED', 'MISSED')
   OR job.expires_at IS NULL
   OR job.create_date IS NULL
   OR job.expires_at <= job.create_date
   OR job.replay_of_display_event_id = job.id
   OR job.replay_of_display_event_id >= job.id
   OR (job.replay_of_display_event_id IS NOT NULL AND replay.id IS NULL)
   OR (replay.id IS NOT NULL AND replay.roulette_event_id <> job.roulette_event_id)
   OR (BINARY job.status = BINARY 'DISPLAYED' AND job.displayed_at IS NULL)
   OR (BINARY job.status <> BINARY 'DISPLAYED' AND job.displayed_at IS NOT NULL);

SELECT 'overlay_displaying_job' AS check_name, COUNT(*) AS problem_count
FROM overlay_display_event
WHERE BINARY status = BINARY 'DISPLAYING';

-- Must be drained or explicitly normalized before cutover because legacy DISPLAYING has no claim token.
SELECT status, COUNT(*) AS in_flight_count
FROM overlay_display_event
WHERE BINARY status IN ('PENDING', 'DISPLAYING')
GROUP BY status
ORDER BY status;

-- 11. Removed subscription feature
-- Blocking: export/retention approval is required if this is not 0.
SELECT 'subscription_rows' AS check_name, COUNT(*) AS problem_count
FROM subscription;

-- 12. Cardinality baseline used again after shadow backfill
SELECT 'authorization_account' AS source_table, COUNT(*) AS row_count FROM authorization_account
UNION ALL SELECT 'command', COUNT(*) FROM command
UNION ALL SELECT 'donation', COUNT(*) FROM donation
UNION ALL SELECT 'favorite_account', COUNT(*) FROM favorite_account
UNION ALL SELECT 'favorite_adjustment', COUNT(*) FROM favorite_adjustment
UNION ALL SELECT 'favorite_history', COUNT(*) FROM favorite_history
UNION ALL SELECT 'overlay_display_event', COUNT(*) FROM overlay_display_event
UNION ALL SELECT 'overlay_token', COUNT(*) FROM overlay_token
UNION ALL SELECT 'roulette_event', COUNT(*) FROM roulette_event
UNION ALL SELECT 'roulette_item', COUNT(*) FROM roulette_item
UNION ALL SELECT 'roulette_round_result', COUNT(*) FROM roulette_round_result
UNION ALL SELECT 'roulette_table', COUNT(*) FROM roulette_table
UNION ALL SELECT 'subscription', COUNT(*) FROM subscription
UNION ALL SELECT 'timer_message', COUNT(*) FROM timer_message
UNION ALL SELECT 'upbo_template', COUNT(*) FROM upbo_template
UNION ALL SELECT 'user_upbo', COUNT(*) FROM user_upbo
UNION ALL SELECT 'weekly_chat_rank', COUNT(*) FROM weekly_chat_rank
ORDER BY source_table;

-- These formulas are persisted with the preflight evidence and compared with
-- next_* table counts in V9. They describe every non-1:1 transformation.
WITH canonical_user AS (
    SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin AS user_id FROM authorization_account
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_account
    UNION SELECT CONVERT(favorite_account_user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM favorite_history
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM weekly_chat_rank
    UNION SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM donation
    UNION SELECT CONVERT(donator_channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM donation WHERE donator_channel_id IS NOT NULL AND TRIM(donator_channel_id) <> ''
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
      FROM roulette_event WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
    UNION SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin FROM user_upbo
), expected_target AS (
    SELECT 'user_account' AS target_table, COUNT(*) AS expected_row_count
      FROM canonical_user WHERE user_id IS NOT NULL AND user_id <> ''
    UNION ALL SELECT 'oauth_credential', COUNT(*) FROM authorization_account
    UNION ALL SELECT 'command', COUNT(*) FROM command
    UNION ALL SELECT 'user_command_count', 0
    UNION ALL SELECT 'timer_message', COUNT(*) FROM timer_message
    UNION ALL SELECT 'point_ledger_entry', COUNT(*) FROM favorite_history
    UNION ALL SELECT 'point_adjustment_preset', COUNT(*) FROM favorite_adjustment
    UNION ALL SELECT 'weekly_chat_count', COUNT(*) FROM weekly_chat_rank
    UNION ALL SELECT 'daily_attendance', COUNT(*) FROM favorite_history WHERE BINARY source_type = BINARY 'ATTENDANCE'
    UNION ALL SELECT 'donation', COUNT(*) FROM donation
    UNION ALL SELECT 'roulette_config',
        (SELECT COUNT(*) FROM roulette_table) + (SELECT COUNT(*) FROM roulette_event)
    UNION ALL SELECT 'roulette_option',
        (SELECT COUNT(*) FROM roulette_item)
        + COALESCE((SELECT SUM(JSON_LENGTH(items_snapshot_json)) FROM roulette_event), 0)
    UNION ALL SELECT 'roulette_run', COUNT(*) FROM roulette_event
    UNION ALL SELECT 'roulette_round', COUNT(*) FROM roulette_round_result
    UNION ALL SELECT 'reward_grant', COUNT(*) FROM user_upbo
    UNION ALL SELECT 'overlay_access_token', COUNT(*) FROM overlay_token
    UNION ALL SELECT 'overlay_display_job', COUNT(*) FROM overlay_display_event
)
SELECT target_table, expected_row_count
FROM expected_target
ORDER BY target_table;
