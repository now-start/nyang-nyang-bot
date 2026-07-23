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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.flywaydb.core.api.migration.Context;

final class CanonicalMigrationSupport {

    // Frozen together with V7.1-V10 after their first release. New migrations must use a new support class.

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

    static void requireLegacyDateTimeUtcApproval() throws SQLException {
        String approval = System.getProperty("nyang.migration.legacy-datetime-utc-approved");
        if (approval == null) {
            approval = System.getenv("NYANG_LEGACY_DATETIME_UTC_APPROVED");
        }
        if (!Boolean.parseBoolean(approval)) {
            throw new SQLException("Canonical migration requires explicit confirmation that all legacy "
                    + "DATETIME values are UTC wall-clock values via "
                    + "-Dnyang.migration.legacy-datetime-utc-approved=true or "
                    + "NYANG_LEGACY_DATETIME_UTC_APPROVED=true");
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

    static LocalDateTime databaseNow(Connection connection) throws SQLException {
        String expression = isMariaDb(connection) ? "UTC_TIMESTAMP(6)" : "CURRENT_TIMESTAMP(6)";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT " + expression)) {
            if (!resultSet.next()) {
                throw new SQLException("Database did not return " + expression);
            }
            return resultSet.getTimestamp(1).toLocalDateTime();
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
                        updateDigest(digest, canonicalValue(rows.getObject(index)));
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

    private record TriggerDefinition(String name, String sql) {
    }
}
