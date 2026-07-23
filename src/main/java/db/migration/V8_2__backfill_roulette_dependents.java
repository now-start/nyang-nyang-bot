package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V8_2__backfill_roulette_dependents extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), false);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (CanonicalMigrationSupport.isMariaDb(context)) {
            CanonicalMigrationSupport.requireAsiaSeoulSession(connection);
        }
        backfillRewardGrants(connection);
        restoreRoundStatuses(connection);
        backfillOverlayDisplayJobs(connection);
    }

    private void backfillRewardGrants(Connection connection) throws SQLException {
        String query = """
                SELECT id, user_id, upbo_template_id, label, status,
                       exchange_favorite_value, reward_type, conversion_mode,
                       source_type, ledger_id, public_description, private_memo,
                       actor_id, create_date, modify_date
                  FROM user_upbo
                 ORDER BY id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rewards = statement.executeQuery(query)) {
            while (rewards.next()) {
                insertRewardGrant(connection, LegacyReward.from(rewards), findLinkedRound(connection,
                        rewards.getLong("id")));
            }
        }
    }

    private Long findLinkedRound(Connection connection, long rewardId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM roulette_round_result WHERE user_upbo_id = ? ORDER BY id")) {
            statement.setLong(1, rewardId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                long id = resultSet.getLong(1);
                if (resultSet.next()) {
                    throw new SQLException("user_upbo " + rewardId + " is linked to multiple rounds");
                }
                return id;
            }
        }
    }

    private void insertRewardGrant(
            Connection connection,
            LegacyReward reward,
            Long linkedRoundId
    ) throws SQLException {
        boolean manual = "UPBO_MANUAL".equals(reward.sourceType());
        boolean roulette = "UPBO_ROULETTE".equals(reward.sourceType());
        if (!manual && !roulette) {
            throw new SQLException("Unknown user_upbo source_type for id " + reward.id());
        }
        String normalizedActor = normalizeActor(reward.actorId());
        if ((manual && (normalizedActor == null || linkedRoundId != null))
                || (roulette && (normalizedActor != null || linkedRoundId == null))) {
            throw new SQLException("user_upbo origin is ambiguous for id " + reward.id());
        }
        if (reward.templateId() != null) {
            throw new SQLException("user_upbo " + reward.id()
                    + " references upbo_template; provenance cannot be discarded");
        }

        Long pointDelta = normalizePointDelta(reward.conversionMode(), reward.exchangeFavoriteValue());
        if (("AUTO".equals(reward.conversionMode()) && reward.ledgerId() == null)
                || (!"AUTO".equals(reward.conversionMode()) && reward.ledgerId() != null)) {
            throw new SQLException("user_upbo ledger linkage is invalid for id " + reward.id());
        }
        validateRewardStatus(reward);

        String insert = """
                INSERT INTO next_reward_grant
                    (id, user_id, roulette_round_id, point_ledger_entry_id,
                     label, reward_type, conversion_mode, point_delta, status,
                     description, private_note, actor_user_id, idempotency_key,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setLong(1, reward.id());
            statement.setString(2, reward.userId());
            setNullableLong(statement, 3, linkedRoundId);
            setNullableLong(statement, 4, reward.ledgerId());
            statement.setString(5, reward.label());
            statement.setString(6, targetRewardType(reward.rewardType()));
            statement.setString(7, reward.conversionMode());
            setNullableLong(statement, 8, pointDelta);
            statement.setString(9, reward.status());
            statement.setString(10, reward.description());
            statement.setString(11, reward.privateNote());
            statement.setString(12, manual ? normalizedActor : null);
            statement.setString(13, "legacy-reward-grant:" + reward.id());
            statement.setObject(14, reward.createdAt());
            statement.setObject(15, reward.updatedAt() == null ? reward.createdAt() : reward.updatedAt());
            statement.executeUpdate();
        }
    }

    private void validateRewardStatus(LegacyReward reward) throws SQLException {
        boolean auto = "AUTO".equals(reward.conversionMode());
        boolean valid = auto
                ? List.of("CONVERTED", "CORRECTED").contains(reward.status())
                : List.of("OWNED", "USED").contains(reward.status());
        if (!valid) {
            throw new SQLException("user_upbo status/conversion mismatch for id " + reward.id());
        }
    }

    private void restoreRoundStatuses(Connection connection) throws SQLException {
        String query = """
                SELECT id, status, failure_reason
                  FROM roulette_round_result
                 ORDER BY id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rounds = statement.executeQuery(query);
             PreparedStatement update = connection.prepareStatement("""
                     UPDATE next_roulette_round
                        SET status = ?, failure_reason = ?, updated_at = updated_at
                      WHERE id = ?
                     """)) {
            while (rounds.next()) {
                long id = rounds.getLong("id");
                String status = requiredText(rounds, "status", "roulette_round_result", id);
                String failureReason = rounds.getString("failure_reason");
                if (!List.of("CONFIRMED", "APPLIED", "FAILED").contains(status)) {
                    throw new SQLException("Unknown roulette round status for id " + id);
                }
                if (!"FAILED".equals(status) && failureReason != null) {
                    throw new SQLException("Non-failed roulette round has failure_reason: " + id);
                }
                if ("CONFIRMED".equals(status)) {
                    continue;
                }
                update.setString(1, status);
                update.setString(2, failureReason);
                update.setLong(3, id);
                if (update.executeUpdate() != 1) {
                    throw new SQLException("Missing canonical roulette round " + id);
                }
            }
        }
    }

    private void backfillOverlayDisplayJobs(Connection connection) throws SQLException {
        String query = """
                SELECT id, roulette_event_id, replay_of_display_event_id, status,
                       expires_at, displayed_at, create_date, modify_date
                  FROM overlay_display_event
                 ORDER BY id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet jobs = statement.executeQuery(query)) {
            while (jobs.next()) {
                insertOverlayDisplayJob(connection, LegacyDisplayJob.from(jobs));
            }
        }
    }

    private void insertOverlayDisplayJob(Connection connection, LegacyDisplayJob job) throws SQLException {
        long runId = findRunId(connection, job.legacyEventId());
        if ("DISPLAYING".equals(job.status())) {
            throw new SQLException("Legacy DISPLAYING overlay job must be drained before cutover: " + job.id());
        }
        if (!List.of("PENDING", "DISPLAYED", "MISSED").contains(job.status())) {
            throw new SQLException("Unknown overlay status for id " + job.id());
        }
        if (("DISPLAYED".equals(job.status()) && job.displayedAt() == null)
                || (!"DISPLAYED".equals(job.status()) && job.displayedAt() != null)
                || !job.expiresAt().isAfter(job.createdAt())) {
            throw new SQLException("Invalid overlay display state for id " + job.id());
        }

        String insert = """
                INSERT INTO next_overlay_display_job
                    (id, roulette_run_id, replay_of_job_id, idempotency_key,
                     status, expires_at, claim_token, claim_expires_at,
                     displayed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setLong(1, job.id());
            statement.setLong(2, runId);
            setNullableLong(statement, 3, job.replayOfJobId());
            statement.setString(4, "legacy-overlay-display:" + job.id());
            statement.setString(5, job.status());
            statement.setObject(6, job.expiresAt());
            if (job.displayedAt() == null) {
                statement.setNull(7, java.sql.Types.TIMESTAMP);
            } else {
                statement.setObject(7, job.displayedAt());
            }
            statement.setObject(8, job.createdAt());
            statement.setObject(9, job.updatedAt() == null ? job.createdAt() : job.updatedAt());
            statement.executeUpdate();
        }
    }

    private long findRunId(Connection connection, long eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT donation_id FROM migration_roulette_event_map WHERE old_event_id = ?")) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No canonical roulette run mapping for event " + eventId);
                }
                return resultSet.getLong(1);
            }
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static String normalizeActor(String actorId) {
        if (actorId == null || actorId.trim().isEmpty()
                || "system".equalsIgnoreCase(actorId.trim())) {
            return null;
        }
        return actorId;
    }

    private static Long normalizePointDelta(String conversionMode, Long value) throws SQLException {
        return switch (conversionMode) {
            case "AUTO" -> {
                if (value == null || value == 0) {
                    throw new SQLException("AUTO reward requires non-zero point value");
                }
                yield value;
            }
            case "MANUAL" -> value;
            case "NONE" -> {
                if (value != null && value != 0) {
                    throw new SQLException("NONE reward cannot contain point value");
                }
                yield null;
            }
            default -> throw new SQLException("Unknown legacy conversion_mode: " + conversionMode);
        };
    }

    private static String targetRewardType(String legacyRewardType) throws SQLException {
        return switch (legacyRewardType) {
            case "FAVORITE" -> "POINT";
            case "COUPON", "MISSION", "PARTICIPATION_PRIORITY", "CUSTOM" -> legacyRewardType;
            default -> throw new SQLException("Unknown legacy reward_type: " + legacyRewardType);
        };
    }

    private static String requiredText(ResultSet row, String column, String table, long id)
            throws SQLException {
        String value = row.getString(column);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException(table + "." + column + " is required for id " + id);
        }
        return value;
    }

    private static LocalDateTime requiredDateTime(
            ResultSet row,
            String column,
            String table,
            long id
    ) throws SQLException {
        LocalDateTime value = row.getObject(column, LocalDateTime.class);
        if (value == null) {
            throw new SQLException(table + "." + column + " is required for id " + id);
        }
        return value;
    }

    private static LocalDateTime nullableDateTime(ResultSet row, String column) throws SQLException {
        return row.getObject(column, LocalDateTime.class);
    }

    private record LegacyReward(
            long id,
            String userId,
            Long templateId,
            String label,
            String status,
            Long exchangeFavoriteValue,
            String rewardType,
            String conversionMode,
            String sourceType,
            Long ledgerId,
            String description,
            String privateNote,
            String actorId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static LegacyReward from(ResultSet row) throws SQLException {
            long id = row.getLong("id");
            return new LegacyReward(
                    id,
                    requiredText(row, "user_id", "user_upbo", id),
                    nullableLong(row, "upbo_template_id"),
                    requiredText(row, "label", "user_upbo", id),
                    requiredText(row, "status", "user_upbo", id),
                    nullableLong(row, "exchange_favorite_value"),
                    requiredText(row, "reward_type", "user_upbo", id),
                    requiredText(row, "conversion_mode", "user_upbo", id),
                    requiredText(row, "source_type", "user_upbo", id),
                    nullableLong(row, "ledger_id"),
                    requiredText(row, "public_description", "user_upbo", id),
                    row.getString("private_memo"),
                    row.getString("actor_id"),
                    requiredDateTime(row, "create_date", "user_upbo", id),
                    nullableDateTime(row, "modify_date")
            );
        }
    }

    private record LegacyDisplayJob(
            long id,
            long legacyEventId,
            Long replayOfJobId,
            String status,
            LocalDateTime expiresAt,
            LocalDateTime displayedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static LegacyDisplayJob from(ResultSet row) throws SQLException {
            long id = row.getLong("id");
            long eventId = row.getLong("roulette_event_id");
            if (row.wasNull()) {
                throw new SQLException("overlay_display_event.roulette_event_id is required for id " + id);
            }
            return new LegacyDisplayJob(
                    id,
                    eventId,
                    nullableLong(row, "replay_of_display_event_id"),
                    requiredText(row, "status", "overlay_display_event", id),
                    requiredDateTime(row, "expires_at", "overlay_display_event", id),
                    nullableDateTime(row, "displayed_at"),
                    requiredDateTime(row, "create_date", "overlay_display_event", id),
                    nullableDateTime(row, "modify_date")
            );
        }
    }

    private static Long nullableLong(ResultSet row, String column) throws SQLException {
        long value = row.getLong(column);
        return row.wasNull() ? null : value;
    }
}
