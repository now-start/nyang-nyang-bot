package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V10__cutover_canonical_schema extends BaseJavaMigration {

    private static final String WRITE_FENCE_MESSAGE = "canonical cutover write fence";
    private static final List<String> LEGACY_TABLES = List.of(
            "migration_roulette_option_map",
            "migration_roulette_event_map",
            "roulette_round_result",
            "overlay_display_event",
            "user_upbo",
            "favorite_history",
            "roulette_event",
            "roulette_item",
            "roulette_table",
            "overlay_token",
            "upbo_template",
            "weekly_chat_rank",
            "favorite_adjustment",
            "favorite_account",
            "authorization_account",
            "subscription",
            "legacy_command",
            "legacy_timer_message",
            "legacy_donation",
            "migration_cutover_metadata"
    );
    private static final List<String> MUTATIONS = List.of("INSERT", "UPDATE", "DELETE");

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), true);
    }

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!CanonicalMigrationSupport.isMariaDb(context)) {
            verifyPreRenameChecksums(connection);
            sequentialH2Rename(connection);
            dropLegacyTables(connection);
            verifyFinalSchema(connection, false);
            return;
        }

        CanonicalMigrationSupport.requireCutoverApproval();
        switch (detectState(connection)) {
            case PRE_RENAME -> migratePreRename(connection);
            case POST_RENAME -> completePostRename(connection);
            case FINAL -> {
                verifyFinalTriggers(connection);
                verifyFinalSchema(connection, true);
            }
        }
    }

    private void migratePreRename(Connection connection) throws Exception {
        boolean renameAttempted = false;
        try {
            ensureSourceWriteFences(connection, false);
            ensureTargetWriteFences(connection, true);
            verifySourceWriteFences(connection, false);
            verifyTargetWriteFences(connection, true);
            verifyPreRenameChecksums(connection);

            CanonicalMigrationSupport.dropInvariants(connection, true);
            renameAttempted = true;
            atomicMariaDbRename(connection);
            completePostRename(connection);
        } catch (Exception failure) {
            if (!renameAttempted) {
                try {
                    dropWriteFences(connection);
                } catch (SQLException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    private void completePostRename(Connection connection) throws Exception {
        ensureSourceWriteFences(connection, true);
        ensureTargetWriteFences(connection, false);
        verifySourceWriteFences(connection, true);
        verifyTargetWriteFences(connection, false);
        verifyPostRenameChecksums(connection);

        CanonicalMigrationSupport.dropInvariants(connection, false);
        CanonicalMigrationSupport.installInvariants(connection, false);
        verifyCanonicalInvariantNames(connection);

        dropLegacyTables(connection);
        dropWriteFences(connection);
        verifyFinalTriggers(connection);
        verifyFinalSchema(connection, true);
    }

    private CutoverState detectState(Connection connection) throws SQLException {
        List<String> nextTables = nextCanonicalTables();
        boolean anyNext = anyTableExists(connection, nextTables);
        boolean allNext = allTablesExist(connection, nextTables);
        boolean allSource = allTablesExist(connection, CanonicalMigrationSupport.legacyTables());
        boolean allCanonical = allTablesExist(connection, CanonicalMigrationSupport.canonicalTables());

        if (allNext && allSource) {
            return CutoverState.PRE_RENAME;
        }
        if (!anyNext && allCanonical) {
            if (!hasLegacyRemnants(connection)
                    && !hasWriteFences(connection)
                    && hasExactFinalTriggerInventory(connection)) {
                return CutoverState.FINAL;
            }
            return CutoverState.POST_RENAME;
        }
        throw new SQLException("Canonical cutover table inventory is neither PRE, POST_RENAME, nor FINAL");
    }

    private void verifyPreRenameChecksums(Connection connection) throws Exception {
        StoredChecksums stored = loadStoredChecksums(connection);
        String currentSource = CanonicalMigrationSupport.databaseChecksum(
                connection, CanonicalMigrationSupport.legacyTables(), "source");
        String currentTarget = CanonicalMigrationSupport.databaseChecksum(
                connection,
                CanonicalMigrationSupport.canonicalTables(),
                nextCanonicalTables(),
                "target"
        );
        requireChecksums(stored, currentSource, currentTarget);
    }

    private void verifyPostRenameChecksums(Connection connection) throws Exception {
        if (!tableExists(connection, "migration_cutover_metadata")) {
            if (hasLegacyRemnants(connection)) {
                throw new SQLException("Cutover metadata disappeared before legacy cleanup completed");
            }
            return;
        }

        StoredChecksums stored = loadStoredChecksums(connection);
        String currentTarget = CanonicalMigrationSupport.databaseChecksum(
                connection,
                CanonicalMigrationSupport.canonicalTables(),
                CanonicalMigrationSupport.canonicalTables(),
                "target"
        );
        if (!stored.target().equals(currentTarget)) {
            throw new SQLException("Canonical data changed after V9 validation; V10 recovery is aborted");
        }

        List<String> postRenameSourceTables = CanonicalMigrationSupport.legacyTables().stream()
                .map(this::postRenameSourceTable)
                .toList();
        if (allTablesExist(connection, postRenameSourceTables)) {
            String currentSource = CanonicalMigrationSupport.databaseChecksum(
                    connection,
                    CanonicalMigrationSupport.legacyTables(),
                    postRenameSourceTables,
                    "source"
            );
            if (!stored.source().equals(currentSource)) {
                throw new SQLException("Legacy data changed after V9 validation; V10 recovery is aborted");
            }
        }
    }

    private StoredChecksums loadStoredChecksums(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT source_checksum, target_checksum
                       FROM migration_cutover_metadata
                      WHERE singleton_id = 1
                     """)) {
            if (!resultSet.next()) {
                throw new SQLException("V9 checksums are required before canonical cutover");
            }
            String source = resultSet.getString("source_checksum");
            String target = resultSet.getString("target_checksum");
            if (source == null || target == null || source.length() != 64 || target.length() != 64) {
                throw new SQLException("V9 checksum format is invalid");
            }
            return new StoredChecksums(source, target);
        }
    }

    private void requireChecksums(StoredChecksums stored, String source, String target) throws SQLException {
        if (!stored.source().equals(source) || !stored.target().equals(target)) {
            throw new SQLException("Legacy or canonical data changed after V9 validation; "
                    + "V10 cutover is aborted before rename");
        }
    }

    private void atomicMariaDbRename(Connection connection) throws SQLException {
        List<String> renamePairs = new ArrayList<>();
        renamePairs.add("`command` TO `legacy_command`");
        renamePairs.add("`timer_message` TO `legacy_timer_message`");
        renamePairs.add("`donation` TO `legacy_donation`");
        for (String table : CanonicalMigrationSupport.canonicalTables()) {
            renamePairs.add("`next_" + table + "` TO `" + table + "`");
        }
        execute(connection, "RENAME TABLE " + String.join(", ", renamePairs));
    }

    private void sequentialH2Rename(Connection connection) throws SQLException {
        execute(connection, "ALTER TABLE command RENAME TO legacy_command");
        execute(connection, "ALTER TABLE timer_message RENAME TO legacy_timer_message");
        execute(connection, "ALTER TABLE donation RENAME TO legacy_donation");
        for (String table : CanonicalMigrationSupport.canonicalTables()) {
            execute(connection, "ALTER TABLE next_" + table + " RENAME TO " + table);
        }
    }

    private void ensureSourceWriteFences(Connection connection, boolean postRename) throws SQLException {
        List<String> sourceTables = CanonicalMigrationSupport.legacyTables();
        for (int index = 0; index < sourceTables.size(); index++) {
            String table = postRename ? postRenameSourceTable(sourceTables.get(index)) : sourceTables.get(index);
            if (!tableExists(connection, table)) {
                continue;
            }
            for (String mutation : MUTATIONS) {
                ensureWriteFence(connection, fenceName("s", index, mutation), table, mutation);
            }
        }
    }

    private void ensureTargetWriteFences(Connection connection, boolean shadow) throws SQLException {
        List<String> targetTables = CanonicalMigrationSupport.canonicalTables();
        for (int index = 0; index < targetTables.size(); index++) {
            String table = shadow ? "next_" + targetTables.get(index) : targetTables.get(index);
            for (String mutation : MUTATIONS) {
                ensureWriteFence(connection, fenceName("t", index, mutation), table, mutation);
            }
        }
    }

    private void ensureWriteFence(
            Connection connection,
            String triggerName,
            String table,
            String mutation
    ) throws SQLException {
        if (writeFenceMatches(connection, triggerName, table, mutation)) {
            return;
        }
        execute(connection, "DROP TRIGGER IF EXISTS `" + triggerName + "`");
        execute(connection, "CREATE TRIGGER `" + triggerName + "` BEFORE " + mutation
                + " ON `" + table + "` FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '"
                + WRITE_FENCE_MESSAGE + "'");
    }

    private void verifySourceWriteFences(Connection connection, boolean postRename) throws SQLException {
        List<String> sourceTables = CanonicalMigrationSupport.legacyTables();
        for (int index = 0; index < sourceTables.size(); index++) {
            String table = postRename ? postRenameSourceTable(sourceTables.get(index)) : sourceTables.get(index);
            if (!tableExists(connection, table)) {
                continue;
            }
            for (String mutation : MUTATIONS) {
                requireWriteFence(connection, fenceName("s", index, mutation), table, mutation);
            }
        }
    }

    private void verifyTargetWriteFences(Connection connection, boolean shadow) throws SQLException {
        List<String> targetTables = CanonicalMigrationSupport.canonicalTables();
        for (int index = 0; index < targetTables.size(); index++) {
            String table = shadow ? "next_" + targetTables.get(index) : targetTables.get(index);
            for (String mutation : MUTATIONS) {
                requireWriteFence(connection, fenceName("t", index, mutation), table, mutation);
            }
        }
    }

    private void requireWriteFence(
            Connection connection,
            String triggerName,
            String table,
            String mutation
    ) throws SQLException {
        if (!writeFenceMatches(connection, triggerName, table, mutation)) {
            throw new SQLException("Missing or invalid cutover write fence " + triggerName);
        }
    }

    private boolean writeFenceMatches(
            Connection connection,
            String triggerName,
            String table,
            String mutation
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_object_table, event_manipulation, action_timing, action_statement
                  FROM information_schema.triggers
                 WHERE trigger_schema = DATABASE() AND trigger_name = ?
                """)) {
            statement.setString(1, triggerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                return table.equalsIgnoreCase(resultSet.getString("event_object_table"))
                        && mutation.equalsIgnoreCase(resultSet.getString("event_manipulation"))
                        && "BEFORE".equalsIgnoreCase(resultSet.getString("action_timing"))
                        && resultSet.getString("action_statement").contains(WRITE_FENCE_MESSAGE)
                        && !resultSet.next();
            }
        }
    }

    private void dropWriteFences(Connection connection) throws SQLException {
        for (String role : List.of("s", "t")) {
            int size = "s".equals(role)
                    ? CanonicalMigrationSupport.legacyTables().size()
                    : CanonicalMigrationSupport.canonicalTables().size();
            for (int index = 0; index < size; index++) {
                for (String mutation : MUTATIONS) {
                    execute(connection, "DROP TRIGGER IF EXISTS `"
                            + fenceName(role, index, mutation) + "`");
                }
            }
        }
    }

    private String fenceName(String role, int index, String mutation) {
        return "trg_cutover_" + role + "_"
                + String.format(Locale.ROOT, "%02d", index) + "_"
                + mutation.substring(0, 1).toLowerCase(Locale.ROOT);
    }

    private String postRenameSourceTable(String sourceTable) {
        return switch (sourceTable) {
            case "command" -> "legacy_command";
            case "timer_message" -> "legacy_timer_message";
            case "donation" -> "legacy_donation";
            default -> sourceTable;
        };
    }

    private void verifyCanonicalInvariantNames(Connection connection) throws Exception {
        for (String triggerName : CanonicalMigrationSupport.invariantTriggerNames(false)) {
            if (queryLong(connection, """
                    SELECT COUNT(*)
                      FROM information_schema.triggers
                     WHERE trigger_schema = DATABASE() AND trigger_name = ?
                    """, triggerName) != 1) {
                throw new SQLException("Missing canonical trigger " + triggerName);
            }
        }
    }

    private void verifyFinalTriggers(Connection connection) throws Exception {
        verifyCanonicalInvariantNames(connection);
        long triggerCount = queryLong(connection, """
                SELECT COUNT(*)
                  FROM information_schema.triggers
                 WHERE trigger_schema = DATABASE()
                   AND trigger_name LIKE 'trg!_%' ESCAPE '!'
                """);
        if (triggerCount != CanonicalMigrationSupport.invariantTriggerNames(false).size()) {
            throw new SQLException("Unexpected final canonical trigger count: " + triggerCount);
        }
    }

    private boolean hasExactFinalTriggerInventory(Connection connection) throws SQLException {
        try {
            verifyFinalTriggers(connection);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean hasWriteFences(Connection connection) throws SQLException {
        return queryLong(connection, """
                SELECT COUNT(*)
                  FROM information_schema.triggers
                 WHERE trigger_schema = DATABASE()
                   AND trigger_name LIKE 'trg_cutover!_%' ESCAPE '!'
                """) > 0;
    }

    private void dropLegacyTables(Connection connection) throws SQLException {
        for (String table : LEGACY_TABLES) {
            execute(connection, "DROP TABLE IF EXISTS `" + table + "`");
        }
    }

    private boolean hasLegacyRemnants(Connection connection) throws SQLException {
        return anyTableExists(connection, LEGACY_TABLES);
    }

    private List<String> nextCanonicalTables() {
        return CanonicalMigrationSupport.canonicalTables().stream()
                .map(table -> "next_" + table)
                .toList();
    }

    private boolean allTablesExist(Connection connection, List<String> tables) throws SQLException {
        for (String table : tables) {
            if (!tableExists(connection, table)) {
                return false;
            }
        }
        return true;
    }

    private boolean anyTableExists(Connection connection, List<String> tables) throws SQLException {
        for (String table : tables) {
            if (tableExists(connection, table)) {
                return true;
            }
        }
        return false;
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        return queryLong(connection, """
                SELECT COUNT(*)
                  FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND LOWER(table_name) = LOWER(?)
                """, table) == 1;
    }

    private void verifyFinalSchema(Connection connection, boolean mariaDb) throws SQLException {
        String schema = mariaDb ? connection.getCatalog() : connection.getSchema();
        long businessTableCount = queryLong(connection, """
                SELECT COUNT(*)
                  FROM information_schema.tables
                 WHERE table_schema = ?
                   AND LOWER(table_name) <> 'flyway_schema_history'
                """, schema);
        if (businessTableCount != 16) {
            throw new SQLException("Expected exactly 16 canonical tables but found " + businessTableCount);
        }

        for (String table : CanonicalMigrationSupport.canonicalTables()) {
            long count = queryLong(connection, """
                    SELECT COUNT(*)
                      FROM information_schema.tables
                     WHERE table_schema = ? AND LOWER(table_name) = ?
                    """, schema, table.toLowerCase(Locale.ROOT));
            if (count != 1) {
                throw new SQLException("Missing canonical table after cutover: " + table);
            }
        }

        long foreignKeys = queryLong(connection, """
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE constraint_schema = ? AND constraint_type = 'FOREIGN KEY'
                """, schema);
        if (foreignKeys != 25) {
            throw new SQLException("Expected 25 canonical foreign keys but found " + foreignKeys);
        }

        if (mariaDb) {
            long commentedTables = queryLong(connection, """
                    SELECT COUNT(*)
                      FROM information_schema.tables
                     WHERE table_schema = DATABASE()
                       AND LOWER(table_name) <> 'flyway_schema_history'
                       AND table_comment <> ''
                    """);
            if (commentedTables != 16) {
                throw new SQLException("Every canonical table must have a database comment");
            }
            long uncommentedColumns = queryLong(connection, """
                    SELECT COUNT(*)
                      FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND LOWER(table_name) <> 'flyway_schema_history'
                       AND column_comment = ''
                    """);
            if (uncommentedColumns != 0) {
                throw new SQLException("Every canonical column must have a database comment");
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long queryLong(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Query returned no row: " + sql);
                }
                return resultSet.getLong(1);
            }
        }
    }

    private enum CutoverState {
        PRE_RENAME,
        POST_RENAME,
        FINAL
    }

    private record StoredChecksums(String source, String target) {
    }
}
