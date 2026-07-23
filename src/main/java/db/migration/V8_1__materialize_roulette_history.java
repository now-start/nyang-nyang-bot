package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V8_1__materialize_roulette_history extends BaseJavaMigration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), true);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (CanonicalMigrationSupport.isMariaDb(context)) {
            CanonicalMigrationSupport.requireAsiaSeoulSession(connection);
            prepareMariaDbRestart(connection);
        }
        createBridgeTables(connection);

        Long activeCurrentConfigId = copyCurrentConfigs(connection);
        long nextConfigId = nextId(connection, "next_roulette_config");
        long nextOptionId = nextId(connection, "next_roulette_option");

        String eventQuery = """
                SELECT id, donation_event_id, user_id, nick_name_snapshot,
                       donation_amount, donation_text, command, price_per_round,
                       round_count, items_snapshot_json, create_date, modify_date
                  FROM roulette_event
                 ORDER BY id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet events = statement.executeQuery(eventQuery)) {
            while (events.next()) {
                EventMaterialization result = materializeEvent(
                        connection,
                        LegacyEvent.from(events),
                        nextConfigId,
                        nextOptionId
                );
                nextConfigId++;
                nextOptionId = result.nextOptionId();
            }
        }

        if (activeCurrentConfigId != null) {
            transitionConfig(connection, activeCurrentConfigId, "ACTIVE");
        }
    }

    private void prepareMariaDbRestart(Connection connection) throws Exception {
        CanonicalMigrationSupport.dropInvariants(connection, true);
        execute(connection, "DROP TABLE IF EXISTS migration_roulette_option_map");
        execute(connection, "DROP TABLE IF EXISTS migration_roulette_event_map");
        execute(connection, "DELETE FROM next_roulette_round");
        execute(connection, "DELETE FROM next_roulette_run");
        execute(connection, "DELETE FROM next_roulette_option");
        execute(connection, "DELETE FROM next_roulette_config");
        execute(connection, "ALTER TABLE next_roulette_round AUTO_INCREMENT = 1");
        execute(connection, "ALTER TABLE next_roulette_option AUTO_INCREMENT = 1");
        execute(connection, "ALTER TABLE next_roulette_config AUTO_INCREMENT = 1");
        CanonicalMigrationSupport.installInvariants(connection, true);
    }

    private void createBridgeTables(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE migration_roulette_event_map
                (
                    old_event_id       BIGINT NOT NULL COMMENT '레거시 roulette_event 식별자',
                    donation_id        BIGINT NOT NULL COMMENT '보존된 후원 식별자',
                    roulette_config_id BIGINT NOT NULL COMMENT '이력 전용 ARCHIVED 설정 식별자',
                    PRIMARY KEY (old_event_id),
                    CONSTRAINT uk_migration_roulette_event_map__donation UNIQUE (donation_id),
                    CONSTRAINT uk_migration_roulette_event_map__config UNIQUE (roulette_config_id),
                    CONSTRAINT fk_migration_roulette_event_map__donation
                        FOREIGN KEY (donation_id) REFERENCES next_donation (id) ON DELETE RESTRICT,
                    CONSTRAINT fk_migration_roulette_event_map__config
                        FOREIGN KEY (roulette_config_id) REFERENCES next_roulette_config (id) ON DELETE RESTRICT
                ) ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4
                  COLLATE = utf8mb4_unicode_ci
                    COMMENT = 'V8.1~V10 사이의 레거시 룰렛 이벤트 브리지'
                """);
        execute(connection, """
                CREATE TABLE migration_roulette_option_map
                (
                    old_event_id       BIGINT  NOT NULL COMMENT '레거시 roulette_event 식별자',
                    option_ordinal     INTEGER NOT NULL COMMENT 'JSON 배열의 0 기준 순번',
                    roulette_option_id BIGINT  NOT NULL COMMENT '물질화된 불변 옵션 식별자',
                    PRIMARY KEY (old_event_id, option_ordinal),
                    CONSTRAINT uk_migration_roulette_option_map__option UNIQUE (roulette_option_id),
                    CONSTRAINT fk_migration_roulette_option_map__event
                        FOREIGN KEY (old_event_id)
                            REFERENCES migration_roulette_event_map (old_event_id) ON DELETE CASCADE,
                    CONSTRAINT fk_migration_roulette_option_map__option
                        FOREIGN KEY (roulette_option_id)
                            REFERENCES next_roulette_option (id) ON DELETE RESTRICT,
                    CONSTRAINT ck_migration_roulette_option_map__ordinal CHECK (option_ordinal >= 0)
                ) ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4
                  COLLATE = utf8mb4_unicode_ci
                    COMMENT = 'V8.1~V10 사이의 레거시 JSON 옵션 브리지'
                """);
    }

    private Long copyCurrentConfigs(Connection connection) throws SQLException {
        int activeCount = queryInt(connection,
                "SELECT COUNT(*) FROM roulette_table WHERE active = TRUE");
        if (activeCount > 1) {
            throw new SQLException("Legacy roulette_table contains more than one active config");
        }

        Long activeConfigId = null;
        String configQuery = """
                SELECT id, title, command, price_per_round, high_round_threshold,
                       active, create_date, modify_date
                  FROM roulette_table
                 ORDER BY id
                """;
        try (Statement statement = connection.createStatement();
             ResultSet configs = statement.executeQuery(configQuery)) {
            while (configs.next()) {
                long configId = configs.getLong("id");
                boolean active = requiredBoolean(configs, "active", "roulette_table", configId);
                insertConfig(
                        connection,
                        configId,
                        requiredText(configs, "title", "roulette_table", configId),
                        requiredText(configs, "command", "roulette_table", configId),
                        requiredLong(configs, "price_per_round", "roulette_table", configId),
                        requiredInt(configs, "high_round_threshold", "roulette_table", configId),
                        requiredDateTime(configs, "create_date", "roulette_table", configId),
                        nullableDateTime(configs, "modify_date")
                );
                copyCurrentOptions(connection, configId);
                if (active) {
                    activeConfigId = configId;
                } else {
                    transitionConfig(connection, configId, "ARCHIVED");
                }
            }
        }
        return activeConfigId;
    }

    private void copyCurrentOptions(Connection connection, long configId) throws SQLException {
        String query = """
                SELECT id, label, probability_basis_points, losing_item, reward_type,
                       conversion_mode, exchange_favorite_value, active, display_order,
                       create_date
                  FROM roulette_item
                 WHERE roulette_table_id = ?
                 ORDER BY display_order, id
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, configId);
            try (ResultSet options = statement.executeQuery()) {
                while (options.next()) {
                    long optionId = options.getLong("id");
                    if (!requiredBoolean(options, "active", "roulette_item", optionId)) {
                        throw new SQLException("Inactive roulette_item cannot be silently discarded: " + optionId);
                    }
                    LegacyOption option = LegacyOption.fromColumns(options, "roulette_item", optionId);
                    insertOption(
                            connection,
                            optionId,
                            configId,
                            option,
                            requiredDateTime(options, "create_date", "roulette_item", optionId)
                    );
                }
            }
        }
    }

    private EventMaterialization materializeEvent(
            Connection connection,
            LegacyEvent event,
            long configId,
            long firstOptionId
    ) throws Exception {
        LegacyDonation donation = findDonation(connection, event.donationEventId());
        validateDonationFacts(event, donation);
        List<LegacyOption> options = parseSnapshot(event);
        List<LegacyRound> rounds = findRounds(connection, event.id());
        validateRoundSequence(event, rounds);

        insertConfig(
                connection,
                configId,
                "Legacy roulette event #" + event.id(),
                event.command(),
                event.pricePerRound(),
                100,
                event.createdAt(),
                event.updatedAt()
        );
        insertEventBridge(connection, event.id(), donation.id(), configId);

        long optionId = firstOptionId;
        List<Long> optionIds = new ArrayList<>(options.size());
        for (int index = 0; index < options.size(); index++) {
            LegacyOption option = options.get(index);
            insertOption(connection, optionId, configId, option, event.createdAt());
            insertOptionBridge(connection, event.id(), index, optionId);
            optionIds.add(optionId);
            optionId++;
        }

        transitionConfig(connection, configId, "ACTIVE");
        insertRun(connection, donation.id(), configId, event.createdAt(), event.updatedAt());
        for (LegacyRound round : rounds) {
            int optionOrdinal = selectedOptionOrdinal(options, round.ticket());
            LegacyOption selected = options.get(optionOrdinal);
            validateRoundFacts(event.id(), round, selected);
            insertRound(connection, round, donation.id(), configId, optionIds.get(optionOrdinal));
        }
        transitionRunToReady(connection, donation.id());
        transitionConfig(connection, configId, "ARCHIVED");
        return new EventMaterialization(optionId);
    }

    private List<LegacyOption> parseSnapshot(LegacyEvent event) throws Exception {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(event.itemsSnapshotJson());
        } catch (Exception exception) {
            throw new SQLException("Invalid items_snapshot_json for roulette_event " + event.id(), exception);
        }
        if (root == null || !root.isArray() || root.isEmpty()) {
            throw new SQLException("roulette_event " + event.id() + " must contain a non-empty option array");
        }

        List<LegacyOption> options = new ArrayList<>();
        long probabilityTotal = 0;
        int positiveLosingCount = 0;
        for (int index = 0; index < root.size(); index++) {
            LegacyOption option = LegacyOption.fromJson(root.get(index), event.id(), index);
            if (!option.active()) {
                throw new SQLException("roulette_event " + event.id()
                        + " snapshot contains inactive option at ordinal " + index);
            }
            probabilityTotal += option.probabilityBasisPoints();
            if (option.losing() && option.probabilityBasisPoints() > 0) {
                positiveLosingCount++;
            }
            options.add(option);
        }
        if (probabilityTotal != 10_000 || positiveLosingCount == 0) {
            throw new SQLException("roulette_event " + event.id()
                    + " snapshot must total 10000 and contain a positive losing option");
        }
        return options;
    }

    private List<LegacyRound> findRounds(Connection connection, long eventId) throws SQLException {
        String query = """
                SELECT id, round_no, item_label, probability_basis_points, losing_item,
                       reward_type, conversion_mode, exchange_favorite_value,
                       status, failure_reason, ticket, create_date, modify_date
                  FROM roulette_round_result
                 WHERE roulette_event_id = ?
                 ORDER BY round_no, id
                """;
        List<LegacyRound> rounds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, eventId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    rounds.add(LegacyRound.from(rows));
                }
            }
        }
        return rounds;
    }

    private void validateRoundSequence(LegacyEvent event, List<LegacyRound> rounds) throws SQLException {
        long expectedFromAmount = event.donationAmount() / event.pricePerRound();
        if (event.roundCount() != expectedFromAmount || rounds.size() != event.roundCount()) {
            throw new SQLException("roulette_event " + event.id() + " round count is inconsistent");
        }
        for (int index = 0; index < rounds.size(); index++) {
            if (rounds.get(index).roundNo() != index + 1) {
                throw new SQLException("roulette_event " + event.id()
                        + " round_no must be contiguous from 1");
            }
        }
    }

    private int selectedOptionOrdinal(List<LegacyOption> options, int ticket) throws SQLException {
        if (ticket < 1 || ticket > 10_000) {
            throw new SQLException("Roulette ticket must be between 1 and 10000: " + ticket);
        }
        int cumulative = 0;
        for (int index = 0; index < options.size(); index++) {
            cumulative += options.get(index).probabilityBasisPoints();
            if (ticket <= cumulative) {
                return index;
            }
        }
        throw new SQLException("Roulette ticket did not resolve to exactly one snapshot option: " + ticket);
    }

    private void validateRoundFacts(long eventId, LegacyRound round, LegacyOption option) throws SQLException {
        if (!Objects.equals(round.label(), option.label())
                || round.probabilityBasisPoints() != option.probabilityBasisPoints()
                || round.losing() != option.losing()
                || !Objects.equals(round.rewardType(), option.legacyRewardType())
                || !Objects.equals(round.conversionMode(), option.conversionMode())
                || !Objects.equals(
                        normalizePointDelta(round.conversionMode(), round.exchangeFavoriteValue()),
                        option.pointDelta()
                )) {
            throw new SQLException("roulette_round_result " + round.id()
                    + " does not match the ticket-selected option for event " + eventId);
        }
    }

    private LegacyDonation findDonation(Connection connection, String legacyEventKey) throws SQLException {
        String query = """
                SELECT id, donor_user_id, donor_display_name, amount, message
                  FROM next_donation
                 WHERE ingestion_key = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, legacyEventKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No donation for roulette event " + legacyEventKey);
                }
                LegacyDonation donation = new LegacyDonation(
                        resultSet.getLong("id"),
                        resultSet.getString("donor_user_id"),
                        resultSet.getString("donor_display_name"),
                        resultSet.getLong("amount"),
                        resultSet.getString("message")
                );
                if (resultSet.next()) {
                    throw new SQLException("Multiple donations for roulette event " + legacyEventKey);
                }
                return donation;
            }
        }
    }

    private void validateDonationFacts(LegacyEvent event, LegacyDonation donation) throws SQLException {
        if (donation.donorUserId() == null || donation.donorUserId().trim().isEmpty()) {
            throw new SQLException("Anonymous donation cannot create roulette run: " + event.id());
        }
        if (!Objects.equals(normalizeBlank(event.userId()), normalizeBlank(donation.donorUserId()))
                || !Objects.equals(event.nickNameSnapshot(), donation.donorDisplayName())
                || event.donationAmount() != donation.amount()
                || !Objects.equals(event.donationText(), donation.message())) {
            throw new SQLException("roulette_event and donation facts differ for event " + event.id());
        }
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void insertConfig(
            Connection connection,
            long id,
            String title,
            String triggerToken,
            long pricePerRound,
            int threshold,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) throws SQLException {
        String sql = """
                INSERT INTO next_roulette_config
                    (id, title, trigger_token, price_per_round, high_round_threshold,
                     status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, title);
            statement.setString(3, triggerToken);
            statement.setLong(4, pricePerRound);
            statement.setInt(5, threshold);
            statement.setObject(6, createdAt);
            statement.setObject(7, updatedAt == null ? createdAt : updatedAt);
            statement.executeUpdate();
        }
    }

    private void insertOption(
            Connection connection,
            long id,
            long configId,
            LegacyOption option,
            LocalDateTime createdAt
    ) throws SQLException {
        String sql = """
                INSERT INTO next_roulette_option
                    (id, roulette_config_id, label, probability_basis_points, is_losing,
                     reward_type, conversion_mode, point_delta, display_order, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, configId);
            statement.setString(3, option.label());
            statement.setInt(4, option.probabilityBasisPoints());
            statement.setBoolean(5, option.losing());
            statement.setString(6, option.targetRewardType());
            statement.setString(7, option.conversionMode());
            if (option.pointDelta() == null) {
                statement.setNull(8, java.sql.Types.BIGINT);
            } else {
                statement.setLong(8, option.pointDelta());
            }
            statement.setInt(9, option.displayOrder());
            statement.setObject(10, createdAt);
            statement.executeUpdate();
        }
    }

    private void insertRun(
            Connection connection,
            long donationId,
            long configId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) throws SQLException {
        String sql = """
                INSERT INTO next_roulette_run
                    (donation_id, roulette_config_id, status, created_at, updated_at)
                VALUES (?, ?, 'BUILDING', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, donationId);
            statement.setLong(2, configId);
            statement.setObject(3, createdAt);
            statement.setObject(4, updatedAt == null ? createdAt : updatedAt);
            statement.executeUpdate();
        }
    }

    private void insertRound(
            Connection connection,
            LegacyRound round,
            long runId,
            long configId,
            long optionId
    ) throws SQLException {
        String sql = """
                INSERT INTO next_roulette_round
                    (id, roulette_run_id, roulette_config_id, roulette_option_id,
                     round_no, ticket, status, failure_reason, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'CONFIRMED', NULL, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, round.id());
            statement.setLong(2, runId);
            statement.setLong(3, configId);
            statement.setLong(4, optionId);
            statement.setInt(5, round.roundNo());
            statement.setInt(6, round.ticket());
            statement.setObject(7, round.createdAt());
            statement.setObject(8, round.updatedAt() == null ? round.createdAt() : round.updatedAt());
            statement.executeUpdate();
        }
    }

    private void insertEventBridge(
            Connection connection,
            long eventId,
            long donationId,
            long configId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO migration_roulette_event_map
                    (old_event_id, donation_id, roulette_config_id)
                VALUES (?, ?, ?)
                """)) {
            statement.setLong(1, eventId);
            statement.setLong(2, donationId);
            statement.setLong(3, configId);
            statement.executeUpdate();
        }
    }

    private void insertOptionBridge(
            Connection connection,
            long eventId,
            int ordinal,
            long optionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO migration_roulette_option_map
                    (old_event_id, option_ordinal, roulette_option_id)
                VALUES (?, ?, ?)
                """)) {
            statement.setLong(1, eventId);
            statement.setInt(2, ordinal);
            statement.setLong(3, optionId);
            statement.executeUpdate();
        }
    }

    private void transitionConfig(Connection connection, long id, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE next_roulette_config SET status = ?, updated_at = updated_at WHERE id = ?")) {
            statement.setString(1, status);
            statement.setLong(2, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Missing roulette config during transition: " + id);
            }
        }
    }

    private void transitionRunToReady(Connection connection, long donationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE next_roulette_run SET status = 'READY', updated_at = updated_at WHERE donation_id = ?")) {
            statement.setLong(1, donationId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Missing roulette run during READY transition: " + donationId);
            }
        }
    }

    private long nextId(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COALESCE(MAX(id), 0) + 1 FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private int queryInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static boolean requiredBoolean(
            ResultSet row,
            String column,
            String table,
            long id
    ) throws SQLException {
        boolean value = row.getBoolean(column);
        if (row.wasNull()) {
            throw new SQLException(table + "." + column + " is required for id " + id);
        }
        return value;
    }

    private static int requiredInt(
            ResultSet row,
            String column,
            String table,
            long id
    ) throws SQLException {
        int value = row.getInt(column);
        if (row.wasNull()) {
            throw new SQLException(table + "." + column + " is required for id " + id);
        }
        return value;
    }

    private static long requiredLong(
            ResultSet row,
            String column,
            String table,
            long id
    ) throws SQLException {
        long value = row.getLong(column);
        if (row.wasNull()) {
            throw new SQLException(table + "." + column + " is required for id " + id);
        }
        return value;
    }

    private static String requiredText(
            ResultSet row,
            String column,
            String table,
            long id
    ) throws SQLException {
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
        LocalDateTime value = nullableDateTime(row, column);
        if (value == null) {
            throw new SQLException(table + "." + column + " is required for id " + id);
        }
        return value;
    }

    private static LocalDateTime nullableDateTime(ResultSet row, String column) throws SQLException {
        return row.getObject(column, LocalDateTime.class);
    }

    private static Long normalizePointDelta(String conversionMode, Long value) throws SQLException {
        return switch (conversionMode) {
            case "AUTO" -> {
                if (value == null || value == 0) {
                    throw new SQLException("AUTO roulette option requires non-zero point value");
                }
                yield value;
            }
            case "MANUAL" -> value;
            case "NONE" -> {
                if (value != null && value != 0) {
                    throw new SQLException("NONE roulette option cannot contain point value");
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

    private record EventMaterialization(long nextOptionId) {
    }

    private record LegacyDonation(
            long id,
            String donorUserId,
            String donorDisplayName,
            long amount,
            String message
    ) {
    }

    private record LegacyEvent(
            long id,
            String donationEventId,
            String userId,
            String nickNameSnapshot,
            long donationAmount,
            String donationText,
            String command,
            long pricePerRound,
            int roundCount,
            String itemsSnapshotJson,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static LegacyEvent from(ResultSet row) throws SQLException {
            long id = row.getLong("id");
            long donationAmount = requiredLong(row, "donation_amount", "roulette_event", id);
            long pricePerRound = requiredLong(row, "price_per_round", "roulette_event", id);
            int roundCount = requiredInt(row, "round_count", "roulette_event", id);
            if (pricePerRound <= 0 || roundCount < 1) {
                throw new SQLException("Invalid roulette_event policy for id " + id);
            }
            return new LegacyEvent(
                    id,
                    requiredText(row, "donation_event_id", "roulette_event", id),
                    row.getString("user_id"),
                    row.getString("nick_name_snapshot"),
                    donationAmount,
                    row.getString("donation_text"),
                    requiredText(row, "command", "roulette_event", id),
                    pricePerRound,
                    roundCount,
                    requiredText(row, "items_snapshot_json", "roulette_event", id),
                    requiredDateTime(row, "create_date", "roulette_event", id),
                    nullableDateTime(row, "modify_date")
            );
        }
    }

    private record LegacyRound(
            long id,
            int roundNo,
            String label,
            int probabilityBasisPoints,
            boolean losing,
            String rewardType,
            String conversionMode,
            Long exchangeFavoriteValue,
            String status,
            String failureReason,
            int ticket,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        static LegacyRound from(ResultSet row) throws SQLException {
            long id = row.getLong("id");
            long exchangeValue = row.getLong("exchange_favorite_value");
            Long nullableExchangeValue = row.wasNull() ? null : exchangeValue;
            String status = requiredText(row, "status", "roulette_round_result", id);
            if (!List.of("CONFIRMED", "APPLIED", "FAILED").contains(status)) {
                throw new SQLException("Unknown roulette round status for id " + id + ": " + status);
            }
            String failureReason = row.getString("failure_reason");
            if (!"FAILED".equals(status) && failureReason != null) {
                throw new SQLException("Non-failed roulette round has failure_reason: " + id);
            }
            return new LegacyRound(
                    id,
                    requiredInt(row, "round_no", "roulette_round_result", id),
                    requiredText(row, "item_label", "roulette_round_result", id),
                    requiredInt(row, "probability_basis_points", "roulette_round_result", id),
                    requiredBoolean(row, "losing_item", "roulette_round_result", id),
                    requiredText(row, "reward_type", "roulette_round_result", id),
                    requiredText(row, "conversion_mode", "roulette_round_result", id),
                    nullableExchangeValue,
                    status,
                    failureReason,
                    requiredInt(row, "ticket", "roulette_round_result", id),
                    requiredDateTime(row, "create_date", "roulette_round_result", id),
                    nullableDateTime(row, "modify_date")
            );
        }
    }

    private record LegacyOption(
            String label,
            int probabilityBasisPoints,
            boolean losing,
            String legacyRewardType,
            String targetRewardType,
            String conversionMode,
            Long pointDelta,
            boolean active,
            int displayOrder
    ) {
        static LegacyOption fromColumns(
                ResultSet row,
                String table,
                long id
        ) throws SQLException {
            String conversionMode = requiredText(row, "conversion_mode", table, id);
            long exchange = row.getLong("exchange_favorite_value");
            Long nullableExchange = row.wasNull() ? null : exchange;
            String rewardType = requiredText(row, "reward_type", table, id);
            LegacyOption option = new LegacyOption(
                    requiredText(row, "label", table, id),
                    requiredInt(row, "probability_basis_points", table, id),
                    requiredBoolean(row, "losing_item", table, id),
                    rewardType,
                    V8_1__materialize_roulette_history.targetRewardType(rewardType),
                    conversionMode,
                    normalizePointDelta(conversionMode, nullableExchange),
                    requiredBoolean(row, "active", table, id),
                    requiredInt(row, "display_order", table, id)
            );
            option.validate(table + " " + id);
            return option;
        }

        static LegacyOption fromJson(JsonNode node, long eventId, int ordinal) throws SQLException {
            if (node == null || !node.isObject()) {
                throw new SQLException("roulette_event " + eventId
                        + " option " + ordinal + " must be a JSON object");
            }
            String context = "roulette_event " + eventId + " option " + ordinal;
            String conversionMode = requiredJsonText(node, "conversionMode", context);
            Long exchangeValue = nullableJsonLong(node, "exchangeFavoriteValue", context);
            String rewardType = requiredJsonText(node, "rewardType", context);
            LegacyOption option = new LegacyOption(
                    requiredJsonText(node, "label", context),
                    requiredJsonInt(node, "probabilityBasisPoints", context),
                    requiredJsonBoolean(node, "losingItem", context),
                    rewardType,
                    V8_1__materialize_roulette_history.targetRewardType(rewardType),
                    conversionMode,
                    normalizePointDelta(conversionMode, exchangeValue),
                    requiredJsonBoolean(node, "active", context),
                    requiredJsonInt(node, "displayOrder", context)
            );
            option.validate(context);
            return option;
        }

        private void validate(String context) throws SQLException {
            if (label.codePointCount(0, label.length()) > 100
                    || probabilityBasisPoints < 0 || probabilityBasisPoints > 10_000
                    || displayOrder < 0) {
                throw new SQLException("Invalid roulette option values in " + context);
            }
            if (losing && (!"NONE".equals(conversionMode) || pointDelta != null)) {
                throw new SQLException("Losing roulette option must use NONE without value in " + context);
            }
        }

        private static String requiredJsonText(JsonNode node, String field, String context)
                throws SQLException {
            JsonNode value = node.get(field);
            if (value == null || !value.isTextual() || value.textValue().trim().isEmpty()) {
                throw new SQLException(context + " requires textual " + field);
            }
            return value.textValue();
        }

        private static int requiredJsonInt(JsonNode node, String field, String context)
                throws SQLException {
            JsonNode value = node.get(field);
            if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
                throw new SQLException(context + " requires integer " + field);
            }
            return value.intValue();
        }

        private static boolean requiredJsonBoolean(JsonNode node, String field, String context)
                throws SQLException {
            JsonNode value = node.get(field);
            if (value == null || !value.isBoolean()) {
                throw new SQLException(context + " requires boolean " + field);
            }
            return value.booleanValue();
        }

        private static Long nullableJsonLong(JsonNode node, String field, String context)
                throws SQLException {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            if (!value.isIntegralNumber() || !value.canConvertToLong()) {
                throw new SQLException(context + " requires integer or null " + field);
            }
            return value.longValue();
        }
    }
}
