package db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V8__backfill_canonical_data extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), false);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (CanonicalMigrationSupport.isMariaDb(context)) {
            CanonicalMigrationSupport.requireCutoverApproval();
            CanonicalMigrationSupport.requireLegacyDateTimeUtcApproval();
            CanonicalMigrationSupport.requireStableSnapshotIsolation(connection);
        }
        String sourceSnapshotChecksum = CanonicalMigrationSupport.databaseChecksum(
                connection, CanonicalMigrationSupport.legacyTables(), "source");
        LocalDateTime cutoverAt = CanonicalMigrationSupport.databaseNow(connection);
        captureCutoverMetadata(connection, cutoverAt, sourceSnapshotChecksum);
        backfillUsers(connection, cutoverAt);
        backfillOauthCredentials(connection);
        backfillSimpleCanonicalTables(connection);
    }

    private void captureCutoverMetadata(
            Connection connection,
            LocalDateTime cutoverAt,
            String sourceSnapshotChecksum
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO migration_cutover_metadata "
                        + "(singleton_id, cutover_at, source_checksum) VALUES (1, ?, ?)")) {
            statement.setTimestamp(1, Timestamp.valueOf(cutoverAt));
            statement.setString(2, sourceSnapshotChecksum);
            statement.executeUpdate();
        }
    }

    private void backfillUsers(Connection connection, LocalDateTime cutoverAt) throws SQLException {
        Map<String, UserAggregate> users = new LinkedHashMap<>();
        String candidates = """
                SELECT channel_id AS user_id, channel_name AS display_name,
                       create_date AS created_at, modify_date AS updated_at,
                       COALESCE(modify_date, create_date) AS observed_at,
                       0 AS source_priority, 0 AS source_row_id
                  FROM authorization_account
                UNION ALL
                SELECT user_id, nick_name, create_date, modify_date,
                       COALESCE(modify_date, create_date), 1, 0
                  FROM favorite_account
                UNION ALL
                SELECT favorite_account_user_id, CAST(NULL AS VARCHAR(100)), create_date, modify_date,
                       COALESCE(modify_date, create_date), 1, id
                  FROM favorite_history
                UNION ALL
                SELECT user_id, nick_name, create_date, modify_date,
                       COALESCE(modify_date, create_date), 2, id
                  FROM weekly_chat_rank
                UNION ALL
                SELECT channel_id, CAST(NULL AS VARCHAR(100)), create_date, modify_date,
                       COALESCE(modify_date, create_date), 3, id
                  FROM donation
                UNION ALL
                SELECT donator_channel_id, donator_nickname, create_date, modify_date,
                       COALESCE(modify_date, create_date), 3, id
                  FROM donation
                 WHERE donator_channel_id IS NOT NULL AND TRIM(donator_channel_id) <> ''
                UNION ALL
                SELECT user_id, nick_name_snapshot, create_date, modify_date,
                       COALESCE(modify_date, create_date), 4, id
                  FROM roulette_event
                 WHERE user_id IS NOT NULL AND TRIM(user_id) <> ''
                UNION ALL
                SELECT user_id, nick_name_snapshot, create_date, modify_date,
                       COALESCE(modify_date, create_date), 5, id
                  FROM user_upbo
                """;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(candidates)) {
            while (resultSet.next()) {
                String userId = resultSet.getString("user_id");
                validateUserId(userId);
                UserAggregate aggregate = users.computeIfAbsent(userId, ignored -> new UserAggregate());
                aggregate.add(
                        resultSet.getString("display_name"),
                        localDateTime(resultSet, "created_at"),
                        localDateTime(resultSet, "updated_at"),
                        localDateTime(resultSet, "observed_at"),
                        resultSet.getInt("source_priority"),
                        resultSet.getLong("source_row_id"),
                        cutoverAt
                );
            }
        }

        rejectCaseFoldCollisions(users);

        String insert = """
                INSERT INTO next_user_account
                    (user_id, display_name, is_admin, last_login_at, created_at, updated_at)
                VALUES (?, ?, FALSE, NULL, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (Map.Entry<String, UserAggregate> user : users.entrySet()) {
                UserAggregate aggregate = user.getValue();
                statement.setString(1, user.getKey());
                statement.setString(2, aggregate.displayName());
                statement.setTimestamp(3, Timestamp.valueOf(aggregate.createdAt(cutoverAt)));
                statement.setTimestamp(4, Timestamp.valueOf(aggregate.updatedAt(cutoverAt)));
                statement.addBatch();
            }
            statement.executeBatch();
        }

        executeUpdate(connection, """
                UPDATE next_user_account target
                   SET is_admin = (
                           SELECT source.admin
                             FROM authorization_account source
                            WHERE source.channel_id = target.user_id
                       ),
                       last_login_at = (
                           SELECT source.last_login_at
                             FROM authorization_account source
                            WHERE source.channel_id = target.user_id
                       ),
                       updated_at = target.updated_at
                 WHERE EXISTS (
                           SELECT 1
                             FROM authorization_account source
                            WHERE source.channel_id = target.user_id
                       )
                """);
    }

    private void backfillOauthCredentials(Connection connection) throws SQLException {
        String select = """
                SELECT channel_id, access_token, refresh_token, token_type, scope, expires_in,
                       create_date, modify_date
                  FROM authorization_account
                 ORDER BY channel_id
                """;
        String insert = """
                INSERT INTO next_oauth_credential
                    (user_id, access_token, refresh_token, token_type, scope,
                     access_token_expires_at, credential_version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)
                """;
        try (Statement query = connection.createStatement();
             ResultSet rows = query.executeQuery(select);
             PreparedStatement statement = connection.prepareStatement(insert)) {
            while (rows.next()) {
                LocalDateTime createdAt = requiredDateTime(rows, "create_date");
                LocalDateTime updatedAt = requiredDateTime(rows, "modify_date");
                int expiresIn = rows.getInt("expires_in");
                if (rows.wasNull() || expiresIn <= 0) {
                    throw new SQLException("authorization_account.expires_in must be positive for "
                            + rows.getString("channel_id"));
                }
                statement.setString(1, rows.getString("channel_id"));
                statement.setString(2, requiredText(rows, "access_token"));
                statement.setString(3, requiredText(rows, "refresh_token"));
                statement.setString(4, requiredText(rows, "token_type"));
                statement.setString(5, rows.getString("scope"));
                statement.setTimestamp(6, Timestamp.valueOf(updatedAt.plusSeconds(expiresIn)));
                statement.setTimestamp(7, Timestamp.valueOf(createdAt));
                statement.setTimestamp(8, Timestamp.valueOf(updatedAt));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void backfillSimpleCanonicalTables(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO next_command
                    (id, trigger_token, message_template, is_active, execution_policy,
                     user_cooldown_seconds, created_by_user_id, updated_by_user_id,
                     created_at, updated_at)
                SELECT id, trigger_token,
                       REPLACE(message_template, '{favorite.balance}', '{point.balance}'),
                       active, 'USER_INTERVAL', user_cooldown_seconds,
                       CASE WHEN created_by IS NULL OR LOWER(TRIM(created_by)) = 'system'
                            THEN NULL ELSE created_by END,
                       CASE WHEN updated_by IS NULL OR LOWER(TRIM(updated_by)) = 'system'
                            THEN NULL ELSE updated_by END,
                       create_date, COALESCE(modify_date, create_date)
                  FROM command
                 ORDER BY id
                """);

        executeUpdate(connection, """
                INSERT INTO next_timer_message
                    (id, message_template, interval_minutes, min_chat_count, is_active,
                     next_run_at, chat_count_since_last_send, claimed_chat_count,
                     claim_token, claim_expires_at, last_sent_at,
                     created_by_user_id, updated_by_user_id, created_at, updated_at)
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
                  FROM timer_message
                 ORDER BY id
                """);

        executeUpdate(connection, """
                INSERT INTO next_point_ledger_entry
                    (id, user_id, delta, source_type, source_reference, description,
                     private_note, correction_of_entry_id, actor_user_id,
                     idempotency_key, created_at)
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
                  FROM favorite_history
                 ORDER BY id
                """);

        executeUpdate(connection, """
                INSERT INTO next_point_adjustment_preset (id, label, amount, created_at)
                SELECT id, label, amount, create_date
                  FROM favorite_adjustment
                 ORDER BY id
                """);

        executeUpdate(connection, """
                INSERT INTO next_weekly_chat_count (id, week_start_date, user_id, chat_count)
                SELECT id, week_start_date, user_id, chat_count
                  FROM weekly_chat_rank
                 ORDER BY id
                """);

        executeUpdate(connection, """
                INSERT INTO next_donation
                    (id, ingestion_key, donation_type, recipient_user_id, donor_user_id,
                     donor_display_name, amount, message, received_at)
                SELECT id, donation_event_id, donation_type, channel_id,
                       CASE WHEN donator_channel_id IS NULL OR TRIM(donator_channel_id) = ''
                            THEN NULL ELSE donator_channel_id END,
                       donator_nickname, pay_amount, donation_text, create_date
                  FROM donation
                 ORDER BY id
                """);

        executeUpdate(connection, """
                INSERT INTO next_overlay_access_token
                    (id, token_hash, issued_by_user_id, issued_at, revoked_at)
                SELECT id, token_hash,
                       CASE WHEN issued_by IS NULL OR TRIM(issued_by) = ''
                                      OR LOWER(TRIM(issued_by)) = 'system'
                            THEN NULL ELSE issued_by END,
                       create_date,
                       CASE WHEN active = TRUE THEN NULL
                            ELSE COALESCE(revoked_at, modify_date, create_date) END
                  FROM overlay_token
                 ORDER BY id
                """);
    }

    private void validateUserId(String userId) throws SQLException {
        if (userId == null || userId.isEmpty() || !userId.equals(userId.trim())
                || userId.codePointCount(0, userId.length()) > 64) {
            throw new SQLException("Invalid canonical user ID in legacy data: " + userId);
        }
    }

    private void rejectCaseFoldCollisions(Map<String, UserAggregate> users) throws SQLException {
        Map<String, String> canonical = new LinkedHashMap<>();
        for (String userId : users.keySet()) {
            String equivalenceKey = userId.trim().toLowerCase(Locale.ROOT);
            String previous = canonical.putIfAbsent(equivalenceKey, userId);
            if (previous != null && !previous.equals(userId)) {
                throw new SQLException("Ambiguous user IDs under case-folding: "
                        + previous + " and " + userId);
            }
        }
    }

    private LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDateTime requiredDateTime(ResultSet resultSet, String column) throws SQLException {
        LocalDateTime value = localDateTime(resultSet, column);
        if (value == null) {
            throw new SQLException(column + " is required for legacy row");
        }
        return value;
    }

    private String requiredText(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException(column + " is required for legacy row");
        }
        return value;
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static int compareBinary(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        return Arrays.compareUnsigned(leftBytes, rightBytes);
    }

    private record DisplayCandidate(
            String value,
            LocalDateTime observedAt,
            int sourcePriority,
            long sourceRowId
    ) {
        boolean isPreferredTo(DisplayCandidate other) {
            int observed = observedAt.compareTo(other.observedAt);
            if (observed != 0) {
                return observed > 0;
            }
            if (sourcePriority != other.sourcePriority) {
                return sourcePriority < other.sourcePriority;
            }
            if (sourceRowId != other.sourceRowId) {
                return sourceRowId > other.sourceRowId;
            }
            return compareBinary(value, other.value) < 0;
        }
    }

    private static final class UserAggregate {
        private LocalDateTime earliest;
        private LocalDateTime latest;
        private DisplayCandidate displayCandidate;

        void add(
                String displayName,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                LocalDateTime observedAt,
                int sourcePriority,
                long sourceRowId,
                LocalDateTime cutoverAt
        ) {
            includeTimestamp(createdAt);
            includeTimestamp(updatedAt);
            if (displayName == null || displayName.trim().isEmpty()) {
                return;
            }
            DisplayCandidate candidate = new DisplayCandidate(
                    displayName,
                    observedAt == null ? cutoverAt : observedAt,
                    sourcePriority,
                    sourceRowId
            );
            if (displayCandidate == null || candidate.isPreferredTo(displayCandidate)) {
                displayCandidate = candidate;
            }
        }

        private void includeTimestamp(LocalDateTime timestamp) {
            if (timestamp == null) {
                return;
            }
            if (earliest == null || timestamp.isBefore(earliest)) {
                earliest = timestamp;
            }
            if (latest == null || timestamp.isAfter(latest)) {
                latest = timestamp;
            }
        }

        String displayName() {
            return displayCandidate == null ? null : displayCandidate.value();
        }

        LocalDateTime createdAt(LocalDateTime fallback) {
            return earliest == null ? fallback : earliest;
        }

        LocalDateTime updatedAt(LocalDateTime fallback) {
            return latest == null ? createdAt(fallback) : latest;
        }
    }
}
