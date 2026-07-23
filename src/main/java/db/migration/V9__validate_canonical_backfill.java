package db.migration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V9__validate_canonical_backfill extends BaseJavaMigration {

    private static final List<String> AUTO_INCREMENT_TABLES = List.of(
            "next_command",
            "next_command_execution",
            "next_timer_message",
            "next_point_ledger_entry",
            "next_point_adjustment_preset",
            "next_weekly_chat_count",
            "next_donation",
            "next_roulette_config",
            "next_roulette_option",
            "next_roulette_round",
            "next_reward_grant",
            "next_overlay_access_token",
            "next_overlay_display_job"
    );

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), false);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mariaDb = CanonicalMigrationSupport.isMariaDb(context);
        if (mariaDb) {
            CanonicalMigrationSupport.requireStableSnapshotIsolation(connection);
        }
        validateLegacySnapshotUnchanged(connection);
        validateBlockingLegacyState(connection);
        validateUserAccounts(connection, mariaDb);
        validateCardinality(connection);
        validateSimpleMappings(connection);
        validatePointLedger(connection);
        validateOauthExpiry(connection);
        validateDatabaseObjects(connection, mariaDb);
        persistChecksums(connection);
    }

    private void validateLegacySnapshotUnchanged(Connection connection) throws Exception {
        String expected;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT source_checksum
                       FROM migration_cutover_metadata
                      WHERE singleton_id = 1
                     """)) {
            if (!resultSet.next()) {
                throw new SQLException("Missing migration_cutover_metadata singleton");
            }
            expected = resultSet.getString("source_checksum");
        }
        if (expected == null || expected.length() != 64) {
            throw new SQLException("V8 legacy snapshot checksum is missing or invalid");
        }

        String actual = CanonicalMigrationSupport.databaseChecksum(
                connection, CanonicalMigrationSupport.legacyTables(), "source");
        if (!expected.equals(actual)) {
            throw new SQLException("Legacy data changed since the V8 snapshot; "
                    + "discard the shadow migration state and restart from a maintenance window");
        }
    }

    private void validateBlockingLegacyState(Connection connection) throws SQLException {
        requireCount(connection, "SELECT COUNT(*) FROM subscription", 0,
                "subscription rows require an explicit retention decision");
        requireCount(connection,
                "SELECT COUNT(*) FROM user_upbo WHERE upbo_template_id IS NOT NULL",
                0,
                "linked reward templates cannot be discarded"
        );
        requireCount(connection, "SELECT COUNT(*) FROM next_command_execution", 0,
                "command_execution must start empty");
        requireCount(connection,
                "SELECT COUNT(*) FROM overlay_display_event WHERE status = 'DISPLAYING'",
                0,
                "DISPLAYING overlay jobs must be drained before cutover"
        );
    }

    private void validateUserAccounts(Connection connection, boolean mariaDb) throws SQLException {
        Set<String> sourceIds = sourceUserIds(connection);
        Set<String> targetIds = queryStringSet(connection,
                "SELECT user_id FROM next_user_account ORDER BY user_id");
        if (!sourceIds.equals(targetIds)) {
            throw new SQLException("user_account ID set differs from legacy user candidates");
        }

        Map<String, String> folded = new LinkedHashMap<>();
        for (String userId : sourceIds) {
            String equivalenceKey = userId.trim().toLowerCase(Locale.ROOT);
            String previous = folded.putIfAbsent(equivalenceKey, userId);
            if (previous != null && !previous.equals(userId)) {
                throw new SQLException("Ambiguous user IDs under case-folding: "
                        + previous + " and " + userId);
                }
        }

        if (mariaDb) {
            requireCount(connection, """
                    WITH source_user_id AS (
                        SELECT CONVERT(channel_id USING utf8mb4)
                                   COLLATE utf8mb4_nopad_bin AS raw_value
                          FROM authorization_account
                        UNION ALL
                        SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
                          FROM favorite_account
                        UNION ALL
                        SELECT CONVERT(favorite_account_user_id USING utf8mb4)
                                   COLLATE utf8mb4_nopad_bin
                          FROM favorite_history
                        UNION ALL
                        SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
                          FROM weekly_chat_rank
                        UNION ALL
                        SELECT CONVERT(channel_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
                          FROM donation
                        UNION ALL
                        SELECT CONVERT(donator_channel_id USING utf8mb4)
                                   COLLATE utf8mb4_nopad_bin
                          FROM donation
                         WHERE donator_channel_id IS NOT NULL
                           AND TRIM(donator_channel_id) <> ''
                        UNION ALL
                        SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
                          FROM roulette_event
                         WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
                        UNION ALL
                        SELECT CONVERT(user_id USING utf8mb4) COLLATE utf8mb4_nopad_bin
                          FROM user_upbo
                    )
                    SELECT COUNT(*)
                      FROM (
                          SELECT LOWER(TRIM(raw_value)) COLLATE utf8mb4_unicode_ci
                                     AS equivalence_key
                            FROM source_user_id
                           WHERE raw_value IS NOT NULL AND raw_value <> ''
                           GROUP BY equivalence_key
                          HAVING COUNT(DISTINCT HEX(raw_value)) > 1
                      ) collision
                    """, 0, "user IDs must not collide under legacy database collation");
        }

        String sourceUserOrder = mariaDb ? "HEX(channel_id)" : "channel_id";
        String targetUserOrder = mariaDb ? "HEX(target.user_id)" : "target.user_id";
        assertProjectedRowsEqual(connection,
                """
                        SELECT channel_id, admin, last_login_at
                          FROM authorization_account
                         ORDER BY %s
                        """.formatted(sourceUserOrder),
                """
                        SELECT target.user_id, target.is_admin, target.last_login_at
                          FROM next_user_account target
                          JOIN authorization_account source ON source.channel_id = target.user_id
                         ORDER BY %s
                        """.formatted(targetUserOrder),
                "authorization user flags"
        );
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM next_user_account account
                  JOIN migration_cutover_metadata metadata ON metadata.singleton_id = 1
                 WHERE account.created_at > account.updated_at
                    OR account.updated_at > metadata.cutover_at
                """, 0, "user_account timestamps must preserve legacy observation time");
    }

    private Set<String> sourceUserIds(Connection connection) throws SQLException {
        String sql = """
                SELECT channel_id AS user_id FROM authorization_account
                UNION ALL SELECT user_id FROM favorite_account
                UNION ALL SELECT favorite_account_user_id FROM favorite_history
                UNION ALL SELECT user_id FROM weekly_chat_rank
                UNION ALL SELECT channel_id FROM donation
                UNION ALL SELECT donator_channel_id FROM donation
                    WHERE donator_channel_id IS NOT NULL AND TRIM(donator_channel_id) <> ''
                UNION ALL SELECT user_id FROM roulette_event
                    WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
                UNION ALL SELECT user_id FROM user_upbo
                """;
        return queryStringSet(connection, sql);
    }

    private void validateCardinality(Connection connection) throws SQLException {
        Map<String, String> direct = new LinkedHashMap<>();
        direct.put("next_oauth_credential", "authorization_account");
        direct.put("next_command", "command");
        direct.put("next_timer_message", "timer_message");
        direct.put("next_point_ledger_entry", "favorite_history");
        direct.put("next_point_adjustment_preset", "favorite_adjustment");
        direct.put("next_weekly_chat_count", "weekly_chat_rank");
        direct.put("next_donation", "donation");
        direct.put("next_roulette_run", "roulette_event");
        direct.put("next_roulette_round", "roulette_round_result");
        direct.put("next_reward_grant", "user_upbo");
        direct.put("next_overlay_access_token", "overlay_token");
        direct.put("next_overlay_display_job", "overlay_display_event");
        for (Map.Entry<String, String> pair : direct.entrySet()) {
            long expected = queryLong(connection, "SELECT COUNT(*) FROM " + pair.getValue());
            requireCount(connection, "SELECT COUNT(*) FROM " + pair.getKey(), expected,
                    pair.getKey() + " row count");
        }

        long expectedConfigs = queryLong(connection, "SELECT COUNT(*) FROM roulette_table")
                + queryLong(connection, "SELECT COUNT(*) FROM roulette_event");
        requireCount(connection, "SELECT COUNT(*) FROM next_roulette_config", expectedConfigs,
                "roulette_config row count");

        long expectedOptions = queryLong(connection, "SELECT COUNT(*) FROM roulette_item")
                + queryLong(connection, "SELECT COUNT(*) FROM migration_roulette_option_map");
        requireCount(connection, "SELECT COUNT(*) FROM next_roulette_option", expectedOptions,
                "roulette_option row count");
        requireCount(connection, "SELECT COUNT(*) FROM migration_roulette_event_map",
                queryLong(connection, "SELECT COUNT(*) FROM roulette_event"),
                "roulette event bridge row count");
    }

    private void validateSimpleMappings(Connection connection) throws SQLException {
        assertProjectedRowsEqual(connection,
                """
                        SELECT id, trigger_token,
                               REPLACE(message_template, '{favorite.balance}', '{point.balance}'),
                               active, 'USER_INTERVAL', user_cooldown_seconds,
                               CASE WHEN created_by IS NULL OR LOWER(TRIM(created_by)) = 'system'
                                    THEN NULL ELSE created_by END,
                               CASE WHEN updated_by IS NULL OR LOWER(TRIM(updated_by)) = 'system'
                                    THEN NULL ELSE updated_by END,
                               create_date, COALESCE(modify_date, create_date)
                          FROM command ORDER BY id
                        """,
                """
                        SELECT id, trigger_token, message_template, is_active, execution_policy,
                               user_cooldown_seconds, created_by_user_id, updated_by_user_id,
                               created_at, updated_at
                          FROM next_command ORDER BY id
                        """,
                "command mapping"
        );
        assertProjectedRowsEqual(connection,
                """
                        SELECT id,
                               REPLACE(message_template, '{favorite.balance}', '{point.balance}'),
                               interval_minutes, min_chat_count, active, next_run_at,
                               chat_count_since_last_send, claimed_chat_count,
                               claim_token, claim_expires_at, last_sent_at,
                               CASE WHEN created_by IS NULL OR LOWER(TRIM(created_by)) = 'system'
                                    THEN NULL ELSE created_by END,
                               CASE WHEN updated_by IS NULL OR LOWER(TRIM(updated_by)) = 'system'
                                    THEN NULL ELSE updated_by END,
                               create_date, COALESCE(modify_date, create_date)
                          FROM timer_message ORDER BY id
                        """,
                """
                        SELECT id, message_template, interval_minutes, min_chat_count, is_active,
                               next_run_at, chat_count_since_last_send, claimed_chat_count,
                               claim_token, claim_expires_at, last_sent_at,
                               created_by_user_id, updated_by_user_id, created_at, updated_at
                          FROM next_timer_message ORDER BY id
                        """,
                "timer mapping"
        );
        assertProjectedRowsEqual(connection,
                "SELECT id, label, amount, create_date FROM favorite_adjustment ORDER BY id",
                "SELECT id, label, amount, created_at FROM next_point_adjustment_preset ORDER BY id",
                "point adjustment preset mapping"
        );
        assertProjectedRowsEqual(connection,
                "SELECT id, week_start_date, user_id, chat_count FROM weekly_chat_rank ORDER BY id",
                "SELECT id, week_start_date, user_id, chat_count FROM next_weekly_chat_count ORDER BY id",
                "weekly chat mapping"
        );
        assertProjectedRowsEqual(connection,
                """
                        SELECT id, donation_event_id, donation_type, channel_id,
                               CASE WHEN donator_channel_id IS NULL OR TRIM(donator_channel_id) = ''
                                    THEN NULL ELSE donator_channel_id END,
                               donator_nickname, pay_amount, donation_text, create_date
                          FROM donation ORDER BY id
                        """,
                """
                        SELECT id, ingestion_key, donation_type, recipient_user_id,
                               donor_user_id, donor_display_name, amount, message, received_at
                          FROM next_donation ORDER BY id
                        """,
                "donation mapping"
        );
        assertProjectedRowsEqual(connection,
                """
                        SELECT id, token_hash,
                               CASE WHEN issued_by IS NULL OR TRIM(issued_by) = ''
                                          OR LOWER(TRIM(issued_by)) = 'system'
                                    THEN NULL ELSE issued_by END,
                               create_date,
                               CASE WHEN active = TRUE THEN NULL
                                    ELSE COALESCE(revoked_at, modify_date, create_date) END
                          FROM overlay_token ORDER BY id
                        """,
                """
                        SELECT id, token_hash, issued_by_user_id, issued_at, revoked_at
                          FROM next_overlay_access_token ORDER BY id
                        """,
                "overlay token mapping"
        );
        validateCurrentRouletteMappings(connection);
        validateHistoricalRouletteMappings(connection);
        validateRewardMappings(connection);
        validateOverlayJobMappings(connection);
    }

    private void validateCurrentRouletteMappings(Connection connection) throws SQLException {
        assertProjectedRowsEqual(connection,
                """
                        SELECT id, title, command, price_per_round, high_round_threshold,
                               CASE WHEN active = TRUE THEN 'ACTIVE' ELSE 'ARCHIVED' END,
                               create_date, COALESCE(modify_date, create_date)
                          FROM roulette_table ORDER BY id
                        """,
                """
                        SELECT target.id, target.title, target.trigger_token,
                               target.price_per_round, target.high_round_threshold,
                               target.status, target.created_at, target.updated_at
                          FROM next_roulette_config target
                          JOIN roulette_table source ON source.id = target.id
                         ORDER BY target.id
                        """,
                "current roulette config mapping"
        );
        assertProjectedRowsEqual(connection,
                """
                        SELECT id, roulette_table_id, label, probability_basis_points,
                               losing_item,
                               CASE WHEN reward_type = 'FAVORITE' THEN 'POINT' ELSE reward_type END,
                               conversion_mode,
                               CASE WHEN conversion_mode = 'NONE' THEN NULL
                                    ELSE exchange_favorite_value END,
                               display_order, create_date
                          FROM roulette_item ORDER BY id
                        """,
                """
                        SELECT target.id, target.roulette_config_id, target.label,
                               target.probability_basis_points, target.is_losing,
                               target.reward_type, target.conversion_mode, target.point_delta,
                               target.display_order, target.created_at
                          FROM next_roulette_option target
                          JOIN roulette_item source ON source.id = target.id
                         ORDER BY target.id
                        """,
                "current roulette option mapping"
        );
    }

    private void validateHistoricalRouletteMappings(Connection connection) throws SQLException {
        assertProjectedRowsEqual(connection,
                """
                        SELECT event.id, donation.id, event.command, event.price_per_round,
                               event.create_date,
                               COALESCE(event.modify_date, event.create_date),
                               COALESCE(event.modify_date, event.create_date)
                          FROM roulette_event event
                          JOIN donation ON donation.donation_event_id = event.donation_event_id
                         ORDER BY event.id
                        """,
                """
                        SELECT bridge.old_event_id, run.donation_id, config.trigger_token,
                               config.price_per_round, run.created_at,
                               config.updated_at, run.updated_at
                          FROM migration_roulette_event_map bridge
                          JOIN next_roulette_run run ON run.donation_id = bridge.donation_id
                          JOIN next_roulette_config config ON config.id = bridge.roulette_config_id
                         ORDER BY bridge.old_event_id
                        """,
                "historical roulette run mapping"
        );
        assertProjectedRowsEqual(connection,
                """
                        SELECT round_result.id, round_result.roulette_event_id,
                               round_result.round_no, round_result.ticket,
                               round_result.status, round_result.failure_reason,
                               round_result.create_date,
                               COALESCE(round_result.modify_date, round_result.create_date)
                          FROM roulette_round_result round_result
                         ORDER BY round_result.id
                        """,
                """
                        SELECT round_result.id, bridge.old_event_id,
                               round_result.round_no, round_result.ticket,
                               round_result.status, round_result.failure_reason,
                               round_result.created_at, round_result.updated_at
                          FROM next_roulette_round round_result
                          JOIN migration_roulette_event_map bridge
                            ON bridge.donation_id = round_result.roulette_run_id
                         ORDER BY round_result.id
                        """,
                "historical roulette round mapping"
        );
        assertProjectedRowsEqual(connection,
                """
                        SELECT round_result.id, round_result.item_label,
                               round_result.probability_basis_points, round_result.losing_item,
                               CASE WHEN round_result.reward_type = 'FAVORITE'
                                    THEN 'POINT' ELSE round_result.reward_type END,
                               round_result.conversion_mode,
                               CASE WHEN round_result.conversion_mode = 'NONE'
                                    THEN NULL ELSE round_result.exchange_favorite_value END
                          FROM roulette_round_result round_result
                         ORDER BY round_result.id
                        """,
                """
                        SELECT round_result.id, option.label,
                               option.probability_basis_points, option.is_losing,
                               option.reward_type, option.conversion_mode, option.point_delta
                          FROM next_roulette_round round_result
                          JOIN next_roulette_option option
                            ON option.id = round_result.roulette_option_id
                           AND option.roulette_config_id = round_result.roulette_config_id
                         ORDER BY round_result.id
                        """,
                "historical roulette option facts"
        );
        requireCount(connection,
                "SELECT COUNT(*) FROM next_roulette_run WHERE status <> 'READY'",
                0,
                "all migrated roulette runs must be READY"
        );
        requireCount(connection,
                """
                        SELECT COUNT(*)
                          FROM migration_roulette_event_map bridge
                          JOIN next_roulette_config config ON config.id = bridge.roulette_config_id
                         WHERE config.status <> 'ARCHIVED'
                        """,
                0,
                "historical roulette configs must be ARCHIVED"
        );
    }

    private void validateRewardMappings(Connection connection) throws SQLException {
        assertProjectedRowsEqual(connection,
                """
                        SELECT reward.id, reward.user_id,
                               CASE WHEN reward.source_type = 'UPBO_ROULETTE'
                                    THEN round_result.id ELSE NULL END,
                               reward.ledger_id, reward.label,
                               CASE WHEN reward.reward_type = 'FAVORITE'
                                    THEN 'POINT' ELSE reward.reward_type END,
                               reward.conversion_mode,
                               CASE WHEN reward.conversion_mode = 'NONE'
                                    THEN NULL ELSE reward.exchange_favorite_value END,
                               reward.status, reward.public_description, reward.private_memo,
                               CASE WHEN reward.source_type = 'UPBO_MANUAL'
                                    THEN reward.actor_id ELSE NULL END,
                               CONCAT('legacy-reward-grant:', reward.id), reward.create_date,
                               COALESCE(reward.modify_date, reward.create_date)
                          FROM user_upbo reward
                          LEFT JOIN roulette_round_result round_result
                            ON round_result.user_upbo_id = reward.id
                         ORDER BY reward.id
                        """,
                """
                        SELECT id, user_id, roulette_round_id, point_ledger_entry_id,
                               label, reward_type, conversion_mode, point_delta,
                               status, description, private_note, actor_user_id,
                               idempotency_key, created_at, updated_at
                          FROM next_reward_grant ORDER BY id
                        """,
                "reward grant mapping"
        );
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM next_reward_grant reward
                  JOIN next_roulette_round round_result ON round_result.id = reward.roulette_round_id
                  JOIN next_roulette_run run ON run.donation_id = round_result.roulette_run_id
                  JOIN next_donation donation ON donation.id = run.donation_id
                 WHERE reward.roulette_round_id IS NOT NULL
                   AND (donation.donor_user_id IS NULL
                        OR reward.user_id <> donation.donor_user_id)
                """, 0, "roulette reward user must match the donation donor");
        assertProjectedRowsEqual(connection,
                """
                        SELECT reward.id, reward.label, reward.reward_type,
                               reward.conversion_mode, reward.point_delta
                          FROM next_reward_grant reward
                         WHERE reward.roulette_round_id IS NOT NULL
                         ORDER BY reward.id
                        """,
                """
                        SELECT reward.id, option.label, option.reward_type,
                               option.conversion_mode, option.point_delta
                          FROM next_reward_grant reward
                          JOIN next_roulette_round round_result
                            ON round_result.id = reward.roulette_round_id
                          JOIN next_roulette_option option
                            ON option.id = round_result.roulette_option_id
                           AND option.roulette_config_id = round_result.roulette_config_id
                         ORDER BY reward.id
                        """,
                "roulette reward facts must match the selected option"
        );
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM next_reward_grant reward
                  JOIN next_point_ledger_entry ledger
                    ON ledger.id = reward.point_ledger_entry_id
                 WHERE reward.conversion_mode <> 'AUTO'
                    OR reward.status NOT IN ('CONVERTED', 'CORRECTED')
                    OR reward.user_id <> ledger.user_id
                    OR reward.point_delta <> ledger.delta
                    OR ledger.source_type <>
                       CASE WHEN reward.roulette_round_id IS NULL
                            THEN 'REWARD_MANUAL' ELSE 'REWARD_ROULETTE' END
                """, 0, "automatic reward facts must match the linked point ledger entry");
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM roulette_round_result round_result
                  JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
                 WHERE reward.source_type <> 'UPBO_ROULETTE'
                    OR (reward.ledger_id IS NULL AND round_result.ledger_id IS NOT NULL)
                    OR (reward.ledger_id IS NOT NULL AND round_result.ledger_id IS NULL)
                    OR reward.ledger_id <> round_result.ledger_id
                """, 0, "legacy roulette round and reward ledger links must agree");
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM roulette_round_result round_result
                  LEFT JOIN user_upbo reward ON reward.id = round_result.user_upbo_id
                  LEFT JOIN favorite_history ledger ON ledger.id = round_result.ledger_id
                 WHERE (round_result.status = 'APPLIED'
                        AND round_result.losing_item = TRUE
                        AND (round_result.user_upbo_id IS NOT NULL
                             OR round_result.ledger_id IS NOT NULL))
                    OR (round_result.status = 'APPLIED'
                        AND round_result.losing_item = FALSE
                        AND reward.id IS NULL)
                    OR (round_result.status IN ('CONFIRMED', 'FAILED')
                        AND (round_result.user_upbo_id IS NOT NULL
                             OR round_result.ledger_id IS NOT NULL))
                    OR (round_result.status = 'APPLIED'
                        AND round_result.losing_item = FALSE
                        AND round_result.conversion_mode = 'AUTO'
                        AND ledger.id IS NULL)
                    OR (round_result.status = 'APPLIED'
                        AND round_result.losing_item = FALSE
                        AND round_result.conversion_mode <> 'AUTO'
                        AND round_result.ledger_id IS NOT NULL)
                """, 0, "legacy roulette round status must agree with reward and ledger links");
    }

    private void validateOverlayJobMappings(Connection connection) throws SQLException {
        assertProjectedRowsEqual(connection,
                """
                        SELECT job.id, bridge.donation_id, job.replay_of_display_event_id,
                               CONCAT('legacy-overlay-display:', job.id), job.status,
                               job.expires_at, job.displayed_at, job.create_date,
                               COALESCE(job.modify_date, job.create_date)
                          FROM overlay_display_event job
                          JOIN migration_roulette_event_map bridge
                            ON bridge.old_event_id = job.roulette_event_id
                         ORDER BY job.id
                        """,
                """
                        SELECT id, roulette_run_id, replay_of_job_id, idempotency_key,
                               status, expires_at, displayed_at, created_at, updated_at
                          FROM next_overlay_display_job ORDER BY id
                        """,
                "overlay display job mapping"
        );
    }

    private void validatePointLedger(Connection connection) throws SQLException {
        assertProjectedRowsEqual(connection,
                """
                        SELECT id, favorite_account_user_id, delta,
                               CASE source_type
                                   WHEN 'ATTENDANCE' THEN 'PRESENCE_REWARD'
                                   WHEN 'UPBO_MANUAL' THEN 'REWARD_MANUAL'
                                   WHEN 'UPBO_ROULETTE' THEN 'REWARD_ROULETTE'
                                   ELSE source_type
                               END,
                               CASE WHEN source_id IS NULL OR TRIM(source_id) = ''
                                    THEN NULL ELSE source_id END,
                               COALESCE(NULLIF(TRIM(public_description), ''), NULLIF(TRIM(history), '')),
                               private_memo, correction_of_ledger_id,
                               CASE WHEN actor_id IS NULL OR TRIM(actor_id) = ''
                                          OR LOWER(TRIM(actor_id)) = 'system'
                                    THEN NULL ELSE actor_id END,
                               idempotency_key, create_date
                          FROM favorite_history ORDER BY id
                        """,
                """
                        SELECT id, user_id, delta, source_type, source_reference,
                               description, private_note, correction_of_entry_id,
                               actor_user_id, idempotency_key, created_at
                          FROM next_point_ledger_entry ORDER BY id
                        """,
                "point ledger mapping"
        );

        Map<String, Long> sourceBalances = new LinkedHashMap<>();
        String chainQuery = """
                SELECT favorite_account_user_id, delta, balance_after
                  FROM favorite_history
                 ORDER BY favorite_account_user_id, create_date, id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(chainQuery)) {
            while (rows.next()) {
                String userId = rows.getString(1);
                long nextBalance = Math.addExact(sourceBalances.getOrDefault(userId, 0L), rows.getLong(2));
                long recordedBalance = rows.getLong(3);
                if (rows.wasNull() || nextBalance != recordedBalance) {
                    throw new SQLException("Legacy point ledger chain mismatch for user " + userId);
                }
                sourceBalances.put(userId, nextBalance);
            }
        }

        try (Statement statement = connection.createStatement();
             ResultSet accounts = statement.executeQuery(
                     "SELECT user_id, favorite FROM favorite_account ORDER BY user_id")) {
            while (accounts.next()) {
                String userId = accounts.getString(1);
                long expected = accounts.getLong(2);
                if (accounts.wasNull() || sourceBalances.getOrDefault(userId, 0L) != expected) {
                    throw new SQLException("favorite_account balance mismatch for user " + userId);
                }
                long target = queryLong(connection,
                        "SELECT COALESCE(SUM(delta), 0) FROM next_point_ledger_entry WHERE user_id = ?",
                        userId);
                if (target != expected) {
                    throw new SQLException("Canonical point balance mismatch for user " + userId);
                }
            }
        }

        long sourcePresence = queryLong(connection,
                "SELECT COUNT(*) FROM favorite_history WHERE source_type = 'ATTENDANCE'");
        long targetPresence = queryLong(connection, """
                SELECT COUNT(*)
                  FROM next_point_ledger_entry target
                  JOIN favorite_history source ON source.id = target.id
                 WHERE source.source_type = 'ATTENDANCE'
                   AND target.source_type = 'PRESENCE_REWARD'
                """);
        if (sourcePresence != targetPresence) {
            throw new SQLException("ATTENDANCE to PRESENCE_REWARD conversion count mismatch");
        }
    }

    private void validateOauthExpiry(Connection connection) throws SQLException {
        String query = """
                SELECT source.channel_id, source.modify_date, source.expires_in,
                       target.access_token_expires_at,
                       source.access_token, target.access_token,
                       source.refresh_token, target.refresh_token,
                       source.token_type, target.token_type,
                       source.scope, target.scope,
                       source.create_date, target.created_at,
                       source.modify_date, target.updated_at
                  FROM authorization_account source
                  JOIN next_oauth_credential target ON target.user_id = source.channel_id
                 ORDER BY source.channel_id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                LocalDateTime modified = rows.getTimestamp(2).toLocalDateTime();
                int expiresIn = rows.getInt(3);
                LocalDateTime expectedExpiry = modified.plusSeconds(expiresIn);
                LocalDateTime targetExpiry = rows.getTimestamp(4).toLocalDateTime();
                if (!expectedExpiry.equals(targetExpiry)) {
                    throw new SQLException("OAuth expiry mismatch for " + rows.getString(1));
                }
                for (int sourceIndex = 5; sourceIndex <= 15; sourceIndex += 2) {
                    if (!Objects.equals(canonicalValue(rows.getObject(sourceIndex)),
                            canonicalValue(rows.getObject(sourceIndex + 1)))) {
                        throw new SQLException("OAuth value mismatch for " + rows.getString(1));
                    }
                }
            }
        }
    }

    private void validateDatabaseObjects(Connection connection, boolean mariaDb) throws SQLException {
        String schema = mariaDb ? connection.getCatalog() : connection.getSchema();
        long foreignKeys = queryLong(connection, """
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE constraint_schema = ?
                   AND constraint_type = 'FOREIGN KEY'
                   AND (LEFT(LOWER(table_name), 5) = 'next_'
                        OR LEFT(LOWER(table_name), 19) = 'migration_roulette_')
                """, schema);
        if (foreignKeys != 29) {
            throw new SQLException("Expected 29 shadow/bridge foreign keys but found " + foreignKeys);
        }

        if (!mariaDb) {
            return;
        }
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM information_schema.triggers
                 WHERE trigger_schema = DATABASE()
                   AND trigger_name LIKE 'trg_next!_%' ESCAPE '!'
                """, 24, "shadow trigger count");
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND table_name LIKE 'next!_%' ESCAPE '!'
                   AND table_comment <> ''
                """, 16, "shadow table comments");
        requireCount(connection, """
                SELECT COUNT(*)
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name LIKE 'next!_%' ESCAPE '!'
                   AND column_comment = ''
                """, 0, "shadow column comments");
        validateAutoIncrementValues(connection);
    }

    private void validateAutoIncrementValues(Connection connection) throws SQLException {
        for (String table : AUTO_INCREMENT_TABLES) {
            long actual = queryRequiredLong(connection, """
                    SELECT AUTO_INCREMENT
                      FROM information_schema.tables
                     WHERE table_schema = DATABASE()
                       AND table_name = ?
                    """, table);
            long expected = queryLong(connection,
                    "SELECT COALESCE(MAX(id), 0) + 1 FROM " + table);
            if (actual != expected) {
                throw new SQLException(table + " AUTO_INCREMENT mismatch: expected "
                        + expected + " but found " + actual);
            }
        }
    }

    private void persistChecksums(Connection connection) throws Exception {
        String sourceChecksum;
        List<String> targetTables = CanonicalMigrationSupport.canonicalTables().stream()
                .map(table -> "next_" + table)
                .toList();
        String targetChecksum;
        try {
            sourceChecksum = CanonicalMigrationSupport.databaseChecksum(
                    connection, CanonicalMigrationSupport.legacyTables(), "source");
            targetChecksum = CanonicalMigrationSupport.databaseChecksum(
                    connection,
                    CanonicalMigrationSupport.canonicalTables(),
                    targetTables,
                    "target"
            );
        } catch (SQLException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SQLException("Cannot calculate V9 database checksums", exception);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE migration_cutover_metadata
                   SET source_checksum = ?, target_checksum = ?
                 WHERE singleton_id = 1
                """)) {
            statement.setString(1, sourceChecksum);
            statement.setString(2, targetChecksum);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Missing migration_cutover_metadata singleton");
            }
        }
    }

    private void assertProjectedRowsEqual(
            Connection connection,
            String sourceSql,
            String targetSql,
            String label
    ) throws SQLException {
        try (Statement sourceStatement = connection.createStatement();
             Statement targetStatement = connection.createStatement();
             ResultSet source = sourceStatement.executeQuery(sourceSql);
             ResultSet target = targetStatement.executeQuery(targetSql)) {
            int sourceColumns = source.getMetaData().getColumnCount();
            int targetColumns = target.getMetaData().getColumnCount();
            if (sourceColumns != targetColumns) {
                throw new SQLException(label + " projection column count differs");
            }
            long row = 0;
            while (true) {
                boolean hasSource = source.next();
                boolean hasTarget = target.next();
                if (hasSource != hasTarget) {
                    throw new SQLException(label + " row count differs at row " + row);
                }
                if (!hasSource) {
                    return;
                }
                row++;
                for (int column = 1; column <= sourceColumns; column++) {
                    String sourceValue = canonicalValue(source.getObject(column));
                    String targetValue = canonicalValue(target.getObject(column));
                    if (!Objects.equals(sourceValue, targetValue)) {
                        throw new SQLException(label + " differs at row " + row
                                + ", column " + column);
                    }
                }
            }
        }
    }

    private static String canonicalValue(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return HexFormat.of().formatHex(bytes);
        }
        if (value instanceof Clob clob) {
            return clob.getSubString(1, Math.toIntExact(clob.length()));
        }
        if (value instanceof Blob blob) {
            return HexFormat.of().formatHex(blob.getBytes(1, Math.toIntExact(blob.length())));
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "1" : "0";
        }
        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                return number.toString();
            }
        }
        return value.toString();
    }

    private Set<String> queryStringSet(Connection connection, String sql) throws SQLException {
        Set<String> values = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String value = resultSet.getString(1);
                if (value == null || value.isEmpty()) {
                    throw new SQLException("Null/blank user ID in canonical source set");
                }
                values.add(value);
            }
        }
        return values;
    }

    private void requireCount(
            Connection connection,
            String sql,
            long expected,
            String label
    ) throws SQLException {
        long actual = queryLong(connection, sql);
        if (actual != expected) {
            throw new SQLException(label + ": expected " + expected + " but found " + actual);
        }
    }

    private long queryLong(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Count query returned no row: " + sql);
                }
                return resultSet.getLong(1);
            }
        }
    }

    private long queryRequiredLong(Connection connection, String sql, Object... arguments)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Required numeric query returned no row: " + sql);
                }
                long value = resultSet.getLong(1);
                if (resultSet.wasNull()) {
                    throw new SQLException("Required numeric query returned NULL: " + sql);
                }
                return value;
            }
        }
    }
}
