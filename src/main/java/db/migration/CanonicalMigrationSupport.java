package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.Context;

final class CanonicalMigrationSupport {

    // Frozen together with V6.1-V10 after their first release. New migrations must use a new support class.

    private static final String INVARIANTS_RESOURCE =
            "db/canonical-invariants.sql";
    private static final Pattern TRIGGER_STATEMENT =
            Pattern.compile("(?s)(CREATE\\s+TRIGGER\\s+([a-zA-Z0-9_]+).*?END)//");
    private static final List<String> CANONICAL_TABLES = List.of(
            "point_adjustment_preset",
            "overlay_access_token",
            "overlay_display_job",
            "point_ledger_entry",
            "command_execution",
            "oauth_credential",
            "weekly_chat_count",
            "roulette_config",
            "roulette_option",
            "roulette_round",
            "roulette_run",
            "reward_grant",
            "timer_message",
            "user_account",
            "donation",
            "command"
    );
    private static final List<String> LEGACY_TABLES = List.of(
            "authorization_account", "command", "donation", "favorite_account",
            "favorite_adjustment", "favorite_history", "overlay_display_event",
            "overlay_token", "roulette_event", "roulette_item", "roulette_round_result",
            "roulette_table", "subscription", "timer_message", "upbo_template",
            "user_upbo", "weekly_chat_rank"
    );
    private static final Map<String, List<String>> LEGACY_TIMESTAMP_COLUMNS = Map.ofEntries(
            Map.entry("authorization_account", List.of("create_date", "modify_date", "last_login_at")),
            Map.entry("command", List.of("create_date", "modify_date")),
            Map.entry("donation", List.of("create_date", "modify_date")),
            Map.entry("favorite_account", List.of("create_date", "modify_date")),
            Map.entry("favorite_adjustment", List.of("create_date")),
            Map.entry("favorite_history", List.of("create_date", "modify_date")),
            Map.entry("overlay_display_event",
                    List.of("expires_at", "displayed_at", "create_date", "modify_date")),
            Map.entry("overlay_token", List.of("create_date")),
            Map.entry("roulette_event", List.of("create_date", "modify_date")),
            Map.entry("roulette_item", List.of("create_date")),
            Map.entry("roulette_round_result", List.of("create_date", "modify_date")),
            Map.entry("roulette_table", List.of("create_date", "modify_date")),
            Map.entry("timer_message",
                    List.of("next_run_at", "claim_expires_at", "last_sent_at", "create_date", "modify_date")),
            Map.entry("user_upbo", List.of("create_date", "modify_date")),
            Map.entry("weekly_chat_rank", List.of("create_date", "modify_date", "week_start_date"))
    );
    private static final List<String> CURRENT_TIMESTAMP_DEFAULT_COLUMNS = List.of(
            "user_account.created_at", "user_account.updated_at",
            "oauth_credential.created_at", "oauth_credential.updated_at",
            "command.created_at", "command.updated_at",
            "timer_message.created_at", "timer_message.updated_at",
            "point_ledger_entry.created_at", "point_adjustment_preset.created_at",
            "donation.received_at",
            "roulette_config.created_at", "roulette_config.updated_at",
            "roulette_option.created_at",
            "roulette_run.created_at", "roulette_run.updated_at",
            "roulette_round.created_at", "roulette_round.updated_at",
            "reward_grant.created_at", "reward_grant.updated_at",
            "overlay_access_token.issued_at",
            "overlay_display_job.created_at", "overlay_display_job.updated_at"
    );
    private static final List<String> ON_UPDATE_TIMESTAMP_COLUMNS = List.of(
            "user_account.updated_at", "oauth_credential.updated_at", "command.updated_at",
            "timer_message.updated_at", "roulette_config.updated_at", "roulette_run.updated_at",
            "roulette_round.updated_at", "reward_grant.updated_at", "overlay_display_job.updated_at"
    );

    private CanonicalMigrationSupport() {
    }

    static boolean isMariaDb(Context context) throws SQLException {
        return isMariaDb(context.getConnection());
    }

    private static boolean isMariaDb(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName()
                .toLowerCase(Locale.ROOT)
                .contains("mariadb");
    }

    static void requireCutoverApproval() throws SQLException {
        String approval = System.getProperty("nyang.migration.canonical-cutover-approved");
        if (approval == null) {
            approval = System.getenv("NYANG_CANONICAL_CUTOVER_APPROVED");
        }
        if (!Boolean.parseBoolean(approval)) {
            throw new SQLException("Canonical migration requires explicit approval via "
                    + "-Dnyang.migration.canonical-cutover-approved=true or "
                    + "NYANG_CANONICAL_CUTOVER_APPROVED=true");
        }
    }

    static void requireBackfillApproval() throws SQLException {
        String approval = System.getProperty("nyang.migration.canonical-backfill-approved");
        if (approval == null) {
            approval = System.getenv("NYANG_CANONICAL_BACKFILL_APPROVED");
        }
        if (!Boolean.parseBoolean(approval)) {
            throw new SQLException("Canonical backfill requires explicit approval via "
                    + "-Dnyang.migration.canonical-backfill-approved=true or "
                    + "NYANG_CANONICAL_BACKFILL_APPROVED=true");
        }
    }

    static void requireLegacyDateTimeZone() throws SQLException {
        String legacyDateTimeZone = System.getProperty("nyang.migration.legacy-datetime-zone");
        if (legacyDateTimeZone == null) {
            legacyDateTimeZone = System.getenv("NYANG_LEGACY_DATETIME_ZONE");
        }
        if (!"Asia/Seoul".equals(legacyDateTimeZone)) {
            throw new SQLException("Canonical backfill requires the verified legacy DATETIME zone via "
                    + "-Dnyang.migration.legacy-datetime-zone=Asia/Seoul or "
                    + "NYANG_LEGACY_DATETIME_ZONE=Asia/Seoul");
        }
    }

    static void requireAsiaSeoulSession(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT @@session.time_zone,
                            TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                            @@session.explicit_defaults_for_timestamp,
                            @@session.sql_mode
                     """)) {
            if (!resultSet.next()) {
                throw new SQLException("Database did not return the session time zone");
            }
            String sessionTimeZone = resultSet.getString(1);
            int offsetSeconds = resultSet.getInt(2);
            if (offsetSeconds != 9 * 60 * 60) {
                throw new SQLException("Canonical migration requires an Asia/Seoul (+09:00) DB session; found "
                        + sessionTimeZone + " with offset seconds " + offsetSeconds);
            }
            if (!resultSet.getBoolean(3)) {
                throw new SQLException("Canonical migration requires "
                        + "explicit_defaults_for_timestamp=ON");
            }
            String sqlMode = resultSet.getString(4);
            if (sqlMode != null
                    && Pattern.compile("(?i)(?:^|,)\\s*MAXDB\\s*(?:,|$)")
                            .matcher(sqlMode)
                            .find()) {
                throw new SQLException("Canonical migration cannot run with SQL_MODE=MAXDB because "
                        + "MariaDB would silently convert TIMESTAMP columns to DATETIME");
            }
        }
    }

    static void requireStableSnapshotIsolation(Connection connection) throws SQLException {
        int isolation = connection.getTransactionIsolation();
        if (isolation != Connection.TRANSACTION_REPEATABLE_READ
                && isolation != Connection.TRANSACTION_SERIALIZABLE) {
            throw new SQLException("Canonical V8 backfill requires REPEATABLE READ or SERIALIZABLE "
                    + "transaction isolation for a stable legacy snapshot");
        }
    }

    static void requireLegacyTimestampRange(Connection connection) throws SQLException {
        LocalDateTime minimum = LocalDateTime.of(1970, 1, 1, 9, 0, 1);
        LocalDateTime exclusiveMaximum = LocalDateTime.of(2038, 1, 19, 12, 14, 8);
        for (Map.Entry<String, List<String>> tableColumns : LEGACY_TIMESTAMP_COLUMNS.entrySet()) {
            String table = tableColumns.getKey();
            for (String column : tableColumns.getValue()) {
                requireIdentifier(table);
                requireIdentifier(column);
                try (PreparedStatement invalid = connection.prepareStatement(
                        "SELECT COUNT(*) FROM `" + table + "` WHERE `" + column
                                + "` IS NOT NULL AND (`" + column + "` < ? OR `" + column + "` >= ?)")) {
                    invalid.setObject(1, minimum);
                    invalid.setObject(2, exclusiveMaximum);
                    try (ResultSet count = invalid.executeQuery()) {
                        if (!count.next()) {
                            throw new SQLException("Legacy TIMESTAMP range check returned no row for "
                                    + table + "." + column);
                        }
                        long invalidRows = count.getLong(1);
                        if (invalidRows != 0) {
                            throw new SQLException("Legacy " + table + "." + column + " contains "
                                    + invalidRows + " value(s) outside the MariaDB 10.11 TIMESTAMP range "
                                    + "when interpreted as Asia/Seoul");
                        }
                    }
                }
            }
        }

        try (PreparedStatement invalidRevokedAt = connection.prepareStatement("""
                SELECT COUNT(*)
                  FROM overlay_token
                 WHERE active = FALSE
                   AND COALESCE(revoked_at, modify_date, create_date) IS NOT NULL
                   AND (COALESCE(revoked_at, modify_date, create_date) < ?
                        OR COALESCE(revoked_at, modify_date, create_date) >= ?)
                """)) {
            invalidRevokedAt.setObject(1, minimum);
            invalidRevokedAt.setObject(2, exclusiveMaximum);
            try (ResultSet count = invalidRevokedAt.executeQuery()) {
                if (!count.next()) {
                    throw new SQLException("Overlay revoked-at TIMESTAMP range check returned no row");
                }
                long invalidRows = count.getLong(1);
                if (invalidRows != 0) {
                    throw new SQLException("Legacy overlay_token revoked-at mapping contains "
                            + invalidRows + " value(s) outside the MariaDB 10.11 TIMESTAMP range "
                            + "when interpreted as Asia/Seoul");
                }
            }
        }

        try (PreparedStatement invalidExpiry = connection.prepareStatement("""
                SELECT COUNT(*)
                  FROM authorization_account
                 WHERE modify_date IS NOT NULL
                   AND expires_in IS NOT NULL
                   AND DATE_ADD(modify_date, INTERVAL expires_in SECOND) >= ?
                """)) {
            invalidExpiry.setObject(1, exclusiveMaximum);
            try (ResultSet count = invalidExpiry.executeQuery()) {
                if (!count.next()) {
                    throw new SQLException("OAuth expiry TIMESTAMP range check returned no row");
                }
                long invalidRows = count.getLong(1);
                if (invalidRows != 0) {
                    throw new SQLException("Legacy OAuth expiry calculation contains " + invalidRows
                            + " value(s) outside the MariaDB 10.11 TIMESTAMP range");
                }
            }
        }
    }

    static void requireCanonicalTimestampColumns(Connection connection, boolean shadow)
            throws SQLException {
        List<String> tables = new ArrayList<>();
        for (String table : CANONICAL_TABLES) {
            tables.add(shadow ? "next_" + table : table);
        }
        if (shadow) {
            tables.add("migration_cutover_metadata");
        }

        int temporalColumns = 0;
        try (PreparedStatement columns = connection.prepareStatement("""
                SELECT table_name, column_name, data_type, datetime_precision,
                       column_default, extra
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND data_type IN ('datetime', 'timestamp')
                 ORDER BY table_name, ordinal_position
                """);
             ResultSet resultSet = columns.executeQuery()) {
            while (resultSet.next()) {
                String table = resultSet.getString("table_name");
                if (!tables.contains(table)) {
                    continue;
                }
                temporalColumns++;
                String dataType = resultSet.getString("data_type");
                int precision = resultSet.getInt("datetime_precision");
                if (!"timestamp".equalsIgnoreCase(dataType) || precision != 6) {
                    throw new SQLException("Canonical temporal column " + table + "."
                            + resultSet.getString("column_name")
                            + " must be TIMESTAMP(6), but found " + dataType + "(" + precision + ")");
                }
                String column = resultSet.getString("column_name");
                String logicalTable = shadow && table.startsWith("next_")
                        ? table.substring("next_".length())
                        : table;
                String logicalColumn = logicalTable + "." + column;
                String columnDefault = resultSet.getString("column_default");
                boolean currentTimestampDefault = columnDefault != null
                        && columnDefault.toLowerCase(Locale.ROOT).startsWith("current_timestamp");
                boolean expectedCurrentTimestamp =
                        CURRENT_TIMESTAMP_DEFAULT_COLUMNS.contains(logicalColumn);
                if (currentTimestampDefault != expectedCurrentTimestamp) {
                    throw new SQLException("Canonical TIMESTAMP default mismatch for " + table + "." + column
                            + ": " + columnDefault);
                }
                String extra = resultSet.getString("extra");
                boolean onUpdate = extra != null
                        && extra.toLowerCase(Locale.ROOT).contains("on update current_timestamp");
                boolean expectedOnUpdate = ON_UPDATE_TIMESTAMP_COLUMNS.contains(logicalColumn);
                if (onUpdate != expectedOnUpdate) {
                    throw new SQLException("Canonical TIMESTAMP ON UPDATE mismatch for " + table + "." + column
                            + ": " + extra);
                }
            }
        }
        int expectedColumns = shadow ? 36 : 35;
        if (temporalColumns != expectedColumns) {
            throw new SQLException("Expected " + expectedColumns + " canonical TIMESTAMP(6) columns but found "
                    + temporalColumns);
        }

        try (PreparedStatement columns = connection.prepareStatement("""
                SELECT table_name, column_name
                  FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND data_type = 'date'
                 ORDER BY table_name, ordinal_position
                """);
             ResultSet resultSet = columns.executeQuery()) {
            while (resultSet.next()) {
                String table = resultSet.getString("table_name");
                if (!tables.contains(table)) {
                    continue;
                }
                String qualifiedColumn = table + "." + resultSet.getString("column_name");
                throw new SQLException("Unexpected canonical DATE column " + qualifiedColumn);
            }
        }
    }

    static LocalDateTime databaseNow(Connection connection) throws SQLException {
        String expression = "CURRENT_TIMESTAMP(6)";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT " + expression)) {
            if (!resultSet.next()) {
                throw new SQLException("Database did not return " + expression);
            }
            return resultSet.getObject(1, LocalDateTime.class);
        }
    }

    private static void requireIdentifier(String identifier) throws SQLException {
        if (!identifier.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Unsafe database identifier in migration metadata: " + identifier);
        }
    }

    static void installInvariants(Connection connection, boolean shadow) throws SQLException, IOException {
        List<TriggerDefinition> triggers = triggerDefinitions(shadow);
        if (triggers.size() != 24) {
            throw new SQLException("Expected 24 canonical triggers but parsed " + triggers.size());
        }

        try (Statement statement = connection.createStatement()) {
            for (TriggerDefinition trigger : triggers) {
                statement.execute(trigger.sql());
            }
        }
    }

    static void dropInvariants(Connection connection, boolean shadow) throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            for (TriggerDefinition trigger : triggerDefinitions(shadow)) {
                statement.execute("DROP TRIGGER IF EXISTS " + trigger.name());
            }
        }
    }

    static List<String> canonicalTables() {
        return CANONICAL_TABLES;
    }

    static List<String> legacyTables() {
        return LEGACY_TABLES;
    }

    static List<String> invariantTriggerNames(boolean shadow) throws IOException {
        return triggerDefinitions(shadow).stream().map(TriggerDefinition::name).toList();
    }

    static String databaseChecksum(
            Connection connection,
            List<String> tables,
            String namespace
    ) throws Exception {
        return databaseChecksum(connection, tables, tables, namespace);
    }

    static String databaseChecksum(
            Connection connection,
            List<String> logicalTables,
            List<String> physicalTables,
            String namespace
    ) throws Exception {
        if (logicalTables.size() != physicalTables.size()) {
            throw new IllegalArgumentException("Logical and physical checksum table counts differ");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int tableIndex = 0; tableIndex < logicalTables.size(); tableIndex++) {
            String logicalTable = logicalTables.get(tableIndex);
            String physicalTable = physicalTables.get(tableIndex);
            updateDigest(digest, namespace + ":" + logicalTable);
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT * FROM " + physicalTable + " ORDER BY 1")) {
                ResultSetMetaData metadata = rows.getMetaData();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    updateDigest(digest, metadata.getColumnName(index));
                }
                while (rows.next()) {
                    for (int index = 1; index <= metadata.getColumnCount(); index++) {
                        updateDigest(digest, canonicalColumnValue(rows, metadata, index));
                    }
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static int checksum(Class<?> migrationClass, boolean includeInvariants) {
        CRC32 crc32 = new CRC32();
        try {
            updateClassTreeChecksum(crc32, CanonicalMigrationSupport.class);
            updateClassTreeChecksum(crc32, migrationClass);
            if (includeInvariants) {
                crc32.update(readInvariants().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot checksum canonical migration bytecode", exception);
        }
        return (int) crc32.getValue();
    }

    private static void updateClassTreeChecksum(CRC32 crc32, Class<?> rootClass) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        collectDeclaredClasses(rootClass, classes);
        classes.sort(Comparator.comparing(Class::getName));
        for (Class<?> type : classes) {
            crc32.update(type.getName().getBytes(StandardCharsets.UTF_8));
            crc32.update(readClassBytes(type));
        }
    }

    private static void collectDeclaredClasses(Class<?> type, List<Class<?>> classes) {
        classes.add(type);
        for (Class<?> declaredClass : type.getDeclaredClasses()) {
            collectDeclaredClasses(declaredClass, classes);
        }
    }

    private static byte[] readClassBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream inputStream = type.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Missing migration class resource: " + type.getName());
            }
            return inputStream.readAllBytes();
        }
    }

    private static List<TriggerDefinition> triggerDefinitions(boolean shadow) throws IOException {
        String source = readInvariants();
        Matcher matcher = TRIGGER_STATEMENT.matcher(source);
        List<TriggerDefinition> triggers = new ArrayList<>();
        while (matcher.find()) {
            String originalName = matcher.group(2);
            String sql = matcher.group(1);
            if (shadow) {
                sql = prefixCanonicalTableReferences(sql);
                sql = sql.replaceFirst(
                        "(?i)CREATE\\s+TRIGGER\\s+" + Pattern.quote(originalName),
                        "CREATE TRIGGER trg_next_" + originalName.substring("trg_".length())
                );
                triggers.add(new TriggerDefinition(
                        "trg_next_" + originalName.substring("trg_".length()),
                        sql
                ));
            } else {
                triggers.add(new TriggerDefinition(originalName, sql));
            }
        }
        return triggers;
    }

    private static String prefixCanonicalTableReferences(String sql) {
        String transformed = sql;
        for (String table : CANONICAL_TABLES) {
            transformed = transformed.replaceAll(
                    "(?<![a-zA-Z0-9_])" + Pattern.quote(table) + "(?![a-zA-Z0-9_])",
                    "next_" + table
            );
        }
        return transformed;
    }

    private static String readInvariants() throws IOException {
        ClassLoader classLoader = CanonicalMigrationSupport.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(INVARIANTS_RESOURCE)) {
            if (inputStream == null) {
                throw new IOException("Missing canonical invariant resource: " + INVARIANTS_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        if (value == null) {
            digest.update((byte) 'N');
            digest.update((byte) '\n');
            return;
        }
        digest.update((byte) 'V');
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) ':');
        digest.update(bytes);
        digest.update((byte) '\n');
    }

    static String canonicalColumnValue(
            ResultSet rows,
            ResultSetMetaData metadata,
            int index
    ) throws SQLException {
        int sqlType = metadata.getColumnType(index);
        Object value = switch (sqlType) {
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ->
                    rows.getObject(index, LocalDateTime.class);
            default -> rows.getObject(index);
        };
        return canonicalValue(value);
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

    private record TriggerDefinition(String name, String sql) {
    }
}
