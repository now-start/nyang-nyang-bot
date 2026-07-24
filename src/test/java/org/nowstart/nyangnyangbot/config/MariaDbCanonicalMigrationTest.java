package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mariadb.MariaDBContainer;

class MariaDbCanonicalMigrationTest {

    private static final String CUTOVER_PROPERTY = "nyang.migration.canonical-cutover-approved";
    private static final String BACKFILL_PROPERTY = "nyang.migration.canonical-backfill-approved";
    private static final String LEGACY_DATETIME_ZONE_PROPERTY = "nyang.migration.legacy-datetime-zone";

    @Test
    void v6RepresentativeFixture_ShouldCutOverWithoutLegacyDataLoss() throws Exception {
        requireDocker();

        MariaDBContainer container = new MariaDBContainer("mariadb:10.11")
                .withDatabaseName("nyang_migration")
                .withUsername("nyang")
                .withPassword("nyang")
                .withCommand("--default-time-zone=+09:00");
        String originalCutoverApproval = System.getProperty(CUTOVER_PROPERTY);
        String originalBackfillApproval = System.getProperty(BACKFILL_PROPERTY);
        String originalLegacyDateTimeZone = System.getProperty(LEGACY_DATETIME_ZONE_PROPERTY);
        try {
            container.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    container.getJdbcUrl(),
                    container.getUsername(),
                    container.getPassword()
            );
            assertRuntimeSessionInitializationSql(dataSource);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject(
                    "SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(6), CURRENT_TIMESTAMP(6))",
                    Integer.class
            )).isEqualTo(9 * 60 * 60);

            migrate(dataSource, "6").migrate();
            insertRepresentativeV6Fixture(jdbc);

            assertAsiaSeoulSessionRequired(dataSource, "6.1");
            assertExplicitTimestampDefaultsRequired(dataSource, "6.1");
            assertMaxDbSqlModeRejected(dataSource, "6.1");
            assertThat(tableExists(jdbc, "next_user_account")).isFalse();
            migrate(dataSource, "7").migrate();
            assertAsiaSeoulSessionRequired(dataSource, "7.1");
            assertExplicitTimestampDefaultsRequired(dataSource, "7.1");
            migrate(dataSource, "7.1").migrate();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.triggers
                     WHERE trigger_schema = DATABASE()
                       AND trigger_name LIKE 'trg_next!_%' ESCAPE '!'
                    """, Integer.class)).isEqualTo(24);
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND (table_name LIKE 'next!_%' ESCAPE '!'
                            OR table_name = 'migration_cutover_metadata')
                       AND data_type = 'timestamp'
                       AND datetime_precision = 6
                    """, Integer.class)).isEqualTo(36);
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO next_roulette_config
                        (title, trigger_token, price_per_round, high_round_threshold, status)
                    VALUES ('invalid', '!invalid', 1, 1, 'ACTIVE')
                    """))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("roulette_config must be inserted as DRAFT");

            Flyway throughV9 = migrate(dataSource, "9");
            System.clearProperty(CUTOVER_PROPERTY);
            System.clearProperty(BACKFILL_PROPERTY);
            System.clearProperty(LEGACY_DATETIME_ZONE_PROPERTY);
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("backfill requires explicit approval");
            throughV9.repair();
            System.setProperty(BACKFILL_PROPERTY, "true");
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("legacy DATETIME zone");
            throughV9.repair();
            System.setProperty(LEGACY_DATETIME_ZONE_PROPERTY, "UTC");
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("legacy DATETIME zone");
            throughV9.repair();
            System.setProperty(LEGACY_DATETIME_ZONE_PROPERTY, "Asia/Seoul");
            jdbc.update("""
                    UPDATE authorization_account
                       SET last_login_at = '2038-01-19 12:14:08.000000'
                     WHERE channel_id = 'streamer'
                    """);
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("outside the MariaDB 10.11 TIMESTAMP range");
            throughV9.repair();
            jdbc.update("""
                    UPDATE authorization_account
                       SET last_login_at = '2026-01-02 00:30:00.123456'
                     WHERE channel_id = 'streamer'
                    """);
            jdbc.update("""
                    UPDATE weekly_chat_rank
                       SET week_start_date = '2040-01-02'
                     WHERE id = 110
                    """);
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("weekly_chat_rank.week_start_date")
                    .hasMessageContaining("outside the MariaDB 10.11 TIMESTAMP range");
            throughV9.repair();
            jdbc.update("""
                    UPDATE weekly_chat_rank
                       SET week_start_date = '2026-01-05'
                     WHERE id = 110
                    """);
            jdbc.update("""
                    UPDATE authorization_account
                       SET modify_date = '2038-01-19 11:14:08.000000'
                     WHERE channel_id = 'streamer'
                    """);
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("OAuth expiry calculation")
                    .hasMessageContaining("outside the MariaDB 10.11 TIMESTAMP range");
            throughV9.repair();
            jdbc.update("""
                    UPDATE authorization_account
                       SET modify_date = '2026-01-02 00:00:00.000000'
                     WHERE channel_id = 'streamer'
                    """);
            jdbc.update("""
                    UPDATE overlay_token
                       SET active = FALSE,
                           revoked_at = NULL,
                           modify_date = '2038-01-19 12:14:08.000000'
                     WHERE id = 80
                    """);
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("overlay_token revoked-at mapping")
                    .hasMessageContaining("outside the MariaDB 10.11 TIMESTAMP range");
            throughV9.repair();
            jdbc.update("""
                    UPDATE overlay_token
                       SET active = TRUE,
                           modify_date = '2026-01-01 00:00:00.000000'
                     WHERE id = 80
                    """);
            assertStableSnapshotIsolationRequired(dataSource, "8");
            Instant beforeBackfillInstant = Instant.now().minusSeconds(2);
            LocalDateTime beforeBackfill = jdbc.queryForObject(
                    "SELECT CURRENT_TIMESTAMP(6)", LocalDateTime.class);
            migrate(dataSource, "8.2").migrate();
            LocalDateTime cutoverAt = jdbc.queryForObject(
                    "SELECT cutover_at FROM migration_cutover_metadata WHERE singleton_id = 1",
                    LocalDateTime.class);
            LocalDateTime afterBackfill = jdbc.queryForObject(
                    "SELECT CURRENT_TIMESTAMP(6)", LocalDateTime.class);
            Instant afterBackfillInstant = Instant.now().plusSeconds(2);
            assertThat(cutoverAt).isBetween(beforeBackfill, afterBackfill);
            long cutoverEpoch = jdbc.queryForObject(
                    "SELECT UNIX_TIMESTAMP(cutover_at) FROM migration_cutover_metadata "
                            + "WHERE singleton_id = 1",
                    Long.class);
            assertThat(Instant.ofEpochSecond(cutoverEpoch))
                    .isBetween(beforeBackfillInstant, afterBackfillInstant);
            assertStableSnapshotIsolationRequired(dataSource, "9");
            migrate(dataSource, "9").migrate();

            System.clearProperty(CUTOVER_PROPERTY);
            Flyway cutover = migrate(dataSource, "10");
            assertThatThrownBy(cutover::migrate)
                    .hasMessageContaining("requires explicit approval");
            cutover.repair();
            assertThat(tableExists(jdbc, "next_user_account")).isTrue();
            assertThat(tableExists(jdbc, "authorization_account")).isTrue();

            String originalTemplate = jdbc.queryForObject(
                    "SELECT message_template FROM command WHERE trigger_token = '!호감도'",
                    String.class
            );
            jdbc.update("UPDATE command SET message_template = 'changed after validation' "
                    + "WHERE trigger_token = '!호감도'");
            System.setProperty(CUTOVER_PROPERTY, "true");
            assertThatThrownBy(cutover::migrate)
                    .hasMessageContaining("changed after V9 validation");
            cutover.repair();
            assertThat(cutoverFenceCount(jdbc)).isZero();
            jdbc.update("UPDATE command SET message_template = ? WHERE trigger_token = '!호감도'",
                    originalTemplate);

            jdbc.execute("""
                    CREATE TABLE cutover_retry_blocker
                    (
                        user_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (user_id),
                        CONSTRAINT fk_cutover_retry_blocker__authorization
                            FOREIGN KEY (user_id)
                                REFERENCES authorization_account (channel_id)
                                ON DELETE RESTRICT
                    ) ENGINE = InnoDB
                    """);
            assertThatThrownBy(cutover::migrate).isInstanceOf(Exception.class);
            assertThat(tableExists(jdbc, "user_account")).isTrue();
            assertThat(tableExists(jdbc, "next_user_account")).isFalse();
            assertThat(tableExists(jdbc, "authorization_account")).isTrue();
            assertThat(cutoverFenceCount(jdbc)).isPositive();

            cutover.repair();
            jdbc.execute("DROP TABLE cutover_retry_blocker");
            cutover.migrate();
            assertFinalCanonicalSchema(jdbc);
            assertRepresentativeData(jdbc);
            assertTimestampSessionConversion(dataSource);
            assertCanonicalTriggers(jdbc);
        } finally {
            restoreProperty(CUTOVER_PROPERTY, originalCutoverApproval);
            restoreProperty(BACKFILL_PROPERTY, originalBackfillApproval);
            restoreProperty(LEGACY_DATETIME_ZONE_PROPERTY, originalLegacyDateTimeZone);
            container.stop();
        }
    }

    @Test
    void partialShadowBuildPhases_ShouldRecoverAfterRepairAndRerun() throws Exception {
        requireDocker();

        MariaDBContainer container = new MariaDBContainer("mariadb:10.11")
                .withDatabaseName("nyang_build_retry")
                .withUsername("nyang")
                .withPassword("nyang")
                .withCommand("--default-time-zone=+09:00");
        String originalCutoverApproval = System.getProperty(CUTOVER_PROPERTY);
        String originalBackfillApproval = System.getProperty(BACKFILL_PROPERTY);
        String originalLegacyDateTimeZone = System.getProperty(LEGACY_DATETIME_ZONE_PROPERTY);
        try {
            container.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    container.getJdbcUrl(),
                    container.getUsername(),
                    container.getPassword()
            );
            assertRuntimeSessionInitializationSql(dataSource);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject(
                    "SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(6), CURRENT_TIMESTAMP(6))",
                    Integer.class
            )).isEqualTo(9 * 60 * 60);

            migrate(dataSource, "6").migrate();
            insertRepresentativeV6Fixture(jdbc);

            jdbc.execute("""
                    CREATE TABLE next_user_account
                    (
                        user_id BIGINT NOT NULL,
                        PRIMARY KEY (user_id)
                    ) ENGINE = InnoDB
                    """);
            Flyway v7 = migrate(dataSource, "7");
            assertThatThrownBy(v7::migrate).isInstanceOf(Exception.class);
            jdbc.execute("DROP TABLE next_user_account");
            v7.repair();
            v7.migrate();
            assertThat(tableExists(jdbc, "next_overlay_display_job")).isTrue();

            jdbc.execute("RENAME TABLE next_roulette_round TO unavailable_roulette_round");
            Flyway v71 = migrate(dataSource, "7.1");
            assertThatThrownBy(v71::migrate).isInstanceOf(Exception.class);
            jdbc.execute("RENAME TABLE unavailable_roulette_round TO next_roulette_round");
            v71.repair();
            v71.migrate();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.triggers
                     WHERE trigger_schema = DATABASE()
                       AND trigger_name LIKE 'trg_next!_%' ESCAPE '!'
                    """, Integer.class)).isEqualTo(24);

            System.setProperty(BACKFILL_PROPERTY, "true");
            System.setProperty(LEGACY_DATETIME_ZONE_PROPERTY, "Asia/Seoul");
            jdbc.update("""
                    UPDATE authorization_account
                       SET last_login_at = '2038-01-19 12:14:07.999999'
                     WHERE channel_id = 'streamer'
                    """);
            migrate(dataSource, "8").migrate();
            String originalItems = jdbc.queryForObject(
                    "SELECT items_snapshot_json FROM roulette_event WHERE id = 50", String.class);
            jdbc.update("UPDATE roulette_event SET items_snapshot_json = 'not-json' WHERE id = 50");
            Flyway v81 = migrate(dataSource, "8.1");
            assertThatThrownBy(v81::migrate).isInstanceOf(Exception.class);
            jdbc.update("UPDATE roulette_event SET items_snapshot_json = ? WHERE id = 50", originalItems);
            v81.repair();
            v81.migrate();
            int configCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM next_roulette_config", Integer.class);
            int optionCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM next_roulette_option", Integer.class);
            int roundCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM next_roulette_round", Integer.class);
            assertThat(configCount).isPositive();
            assertThat(optionCount).isPositive();
            assertThat(roundCount).isPositive();
            assertThat(jdbc.queryForObject(
                    "SELECT status FROM next_roulette_round WHERE id = 61", String.class
            )).isEqualTo("CONFIRMED");
            assertThat(jdbc.queryForObject(
                    "SELECT failure_reason FROM next_roulette_round WHERE id = 61", String.class
            )).isNull();

            jdbc.update("""
                    UPDATE favorite_account
                       SET nick_name = '시청자-변경',
                           modify_date = '2026-01-07 00:00:00.000000'
                     WHERE user_id = 'viewer'
                    """);
            Flyway v9 = migrate(dataSource, "9");
            assertThatThrownBy(v9::migrate)
                    .hasMessageContaining("changed since the V8 snapshot");
            v9.repair();
            jdbc.update("""
                    UPDATE favorite_account
                       SET nick_name = '시청자',
                           modify_date = '2026-01-02 01:00:00.000000'
                     WHERE user_id = 'viewer'
                    """);
            v9.migrate();
            assertThat(jdbc.queryForObject(
                    "SELECT status FROM next_roulette_round WHERE id = 61", String.class
            )).isEqualTo("FAILED");
            assertThat(jdbc.queryForObject(
                    "SELECT failure_reason FROM next_roulette_round WHERE id = 61", String.class
            )).isEqualTo("legacy failure");
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM migration_cutover_metadata
                     WHERE singleton_id = 1
                       AND source_checksum IS NOT NULL
                       AND target_checksum IS NOT NULL
                    """, Integer.class)).isOne();
        } finally {
            restoreProperty(CUTOVER_PROPERTY, originalCutoverApproval);
            restoreProperty(BACKFILL_PROPERTY, originalBackfillApproval);
            restoreProperty(LEGACY_DATETIME_ZONE_PROPERTY, originalLegacyDateTimeZone);
            container.stop();
        }
    }

    private void restoreProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    private Flyway migrate(DataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private void assertStableSnapshotIsolationRequired(DataSource dataSource, String target)
            throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            SingleConnectionDataSource readCommitted = new SingleConnectionDataSource(connection, true);
            Flyway migration = migrate(readCommitted, target);
            assertThatThrownBy(migration::migrate)
                    .hasMessageContaining("requires REPEATABLE READ or SERIALIZABLE");
            migration.repair();
        }
    }

    private void assertAsiaSeoulSessionRequired(DataSource dataSource, String target) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("SET time_zone = '+00:00'");
            }
            SingleConnectionDataSource utcSession = new SingleConnectionDataSource(connection, true);
            Flyway migration = migrate(utcSession, target);
            assertThatThrownBy(migration::migrate)
                    .hasMessageContaining("requires an Asia/Seoul (+09:00) DB session");
            migration.repair();
        }
    }

    private void assertExplicitTimestampDefaultsRequired(DataSource dataSource, String target)
            throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("SET time_zone = '+09:00', explicit_defaults_for_timestamp = OFF");
            }
            SingleConnectionDataSource legacyTimestampDefaults =
                    new SingleConnectionDataSource(connection, true);
            Flyway migration = migrate(legacyTimestampDefaults, target);
            assertThatThrownBy(migration::migrate)
                    .hasMessageContaining("explicit_defaults_for_timestamp=ON");
            migration.repair();
        }
    }

    private void assertMaxDbSqlModeRejected(DataSource dataSource, String target) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("SET sql_mode = 'MAXDB'");
            }
            SingleConnectionDataSource maxDbSession = new SingleConnectionDataSource(connection, true);
            Flyway migration = migrate(maxDbSession, target);
            assertThatThrownBy(migration::migrate)
                    .hasMessageContaining("cannot run with SQL_MODE=MAXDB");
            migration.repair();
        }
    }

    private void assertRuntimeSessionInitializationSql(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+09:00', explicit_defaults_for_timestamp = ON");
            try (var result = statement.executeQuery("""
                    SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                           @@session.explicit_defaults_for_timestamp
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(9 * 60 * 60);
                assertThat(result.getBoolean(2)).isTrue();
            }
        }
    }

    private void requireDocker() {
        boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        boolean required = Boolean.getBoolean("nyang.test.require-mariadb")
                || "true".equalsIgnoreCase(System.getenv("CI"));
        if (required) {
            assertThat(dockerAvailable)
                    .as("MariaDB migration verification is mandatory in CI and explicit migration-test runs")
                    .isTrue();
        }
        Assumptions.assumeTrue(
                dockerAvailable,
                "MariaDB 10.11 migration test requires Docker or OrbStack"
        );
    }

    private void insertRepresentativeV6Fixture(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO authorization_account
                    (channel_id, create_date, modify_date, channel_name,
                     access_token, refresh_token, token_type, expires_in, scope,
                     admin, last_login_at)
                VALUES
                    ('streamer', '2026-01-01 00:00:00.000000',
                     '2026-01-02 00:00:00.000000', '스트리머',
                     'access-token', 'refresh-token', 'Bearer', 3600, 'chat',
                     TRUE, '2026-01-02 00:00:00.000000'),
                    ('a', '2026-01-01 00:00:00.000000',
                     '2026-01-02 00:00:00.000000', 'lower-a',
                     'access-a', 'refresh-a', 'Bearer', 3600, 'chat',
                     FALSE, '2026-01-02 00:30:00.123456'),
                    ('B', '2026-01-01 00:00:00.000000',
                     '2026-01-02 00:00:00.000000', 'upper-b',
                     'access-b', 'refresh-b', 'Bearer', 3600, 'chat',
                     FALSE, '2026-01-02 00:00:00.000000')
                """);
        jdbc.update("""
                INSERT INTO favorite_account
                    (user_id, create_date, modify_date, nick_name, favorite)
                VALUES ('viewer', '2026-01-01 01:00:00.000000',
                        '2026-01-02 01:00:00.000000', '시청자', 175)
                """);
        jdbc.update("""
                INSERT INTO favorite_history
                    (id, create_date, modify_date, history, favorite,
                     favorite_account_user_id, actor_id, balance_after,
                     correction_of_ledger_id, delta, display_category,
                     idempotency_key, nick_name_snapshot, private_memo,
                     public_description, source_type, source_id)
                VALUES
                    (10, '2026-01-03 00:00:00.000000', '2026-01-03 00:00:00.000000',
                     '생존 확인', 100, 'viewer', 'streamer', 100, NULL, 100,
                     'ATTENDANCE', 'presence:10', '시청자', NULL, '생존 확인',
                     'ATTENDANCE', 'presence:10'),
                    (11, '2026-01-04 00:00:00.000000', '2026-01-04 00:00:00.000000',
                     '룰렛 포인트', 150, 'viewer', 'system', 150, NULL, 50,
                     'REWARD', 'roulette-ledger:11', '시청자', '자동 지급', '룰렛 포인트',
                     'UPBO_ROULETTE', 'roulette-round:60'),
                    (12, '2026-01-05 00:00:00.000000', '2026-01-05 00:00:00.000000',
                     '구글 시트 동기화', 175, 'viewer', 'system', 175, NULL, 25,
                     'MIGRATION', 'google-sheet-sync:12', '시청자', NULL, '구글 시트 동기화',
                     'SHEET_MIGRATION', 'google-sheet')
                """);
        jdbc.update("""
                INSERT INTO favorite_adjustment (id, create_date, modify_date, amount, label)
                VALUES (100, '2026-01-01 00:00:00.000000',
                        '2026-01-01 00:00:00.000000', 10, '테스트 조정')
                """);
        jdbc.update("""
                INSERT INTO weekly_chat_rank
                    (id, create_date, modify_date, week_start_date, user_id, nick_name, chat_count)
                VALUES (110, '2026-01-05 00:00:00.000000',
                        '2026-01-05 00:00:00.000000', '2026-01-05',
                        'viewer', '시청자', 7)
                """);
        jdbc.update("""
                INSERT INTO donation
                    (id, create_date, modify_date, donation_type, channel_id,
                     donator_channel_id, donator_nickname, pay_amount,
                     donation_text, emojis_json, donation_event_id)
                VALUES (20, '2026-01-06 00:00:00.000000',
                        '2026-01-06 00:00:00.000000', 'CHAT', 'streamer',
                        'viewer', '시청자', 2000, '룰렛!', '["🐱"]', 'donation-20')
                """);
        jdbc.update("""
                INSERT INTO timer_message
                    (id, create_date, modify_date, message_template,
                     interval_minutes, min_chat_count, active, next_run_at,
                     chat_count_since_last_send, claimed_chat_count,
                     claim_token, claim_expires_at, last_sent_at, created_by, updated_by)
                VALUES (120, '2026-01-01 00:00:00.000000',
                        '2026-01-01 00:00:00.000000',
                        '현재 포인트는 {favorite.balance}', 10, 1, FALSE, NULL,
                        3, 0, NULL, NULL, NULL, 'system', 'system')
                """);
        jdbc.update("""
                INSERT INTO roulette_table
                    (id, create_date, modify_date, title, command,
                     price_per_round, active, version, high_round_threshold)
                VALUES (30, '2026-01-01 00:00:00.000000',
                        '2026-01-01 00:00:00.000000', '현재 룰렛', '!룰렛',
                        1000, TRUE, 1, 100)
                """);
        jdbc.update("""
                INSERT INTO roulette_item
                    (id, create_date, modify_date, roulette_table_id, label,
                     probability_basis_points, losing_item, reward_type,
                     conversion_mode, exchange_favorite_value, active, display_order)
                VALUES
                    (40, '2026-01-01 00:00:00.000000',
                     '2026-01-01 00:00:00.000000', 30, '꽝', 5000, TRUE,
                     'CUSTOM', 'NONE', 0, TRUE, 0),
                    (41, '2026-01-01 00:00:00.000000',
                     '2026-01-01 00:00:00.000000', 30, '당첨', 5000, FALSE,
                     'FAVORITE', 'AUTO', 50, TRUE, 1)
                """);
        jdbc.update("""
                INSERT INTO roulette_event
                    (id, create_date, modify_date, donation_event_id, idempotency_key,
                     user_id, nick_name_snapshot, donation_amount, donation_text,
                     roulette_table_id, roulette_table_version, command,
                     price_per_round, round_count, items_snapshot_json, status)
                VALUES (50, '2026-01-06 00:00:00.000000',
                        '2026-01-06 00:01:00.000000', 'donation-20', 'roulette-event:50',
                        'viewer', '시청자', 2000, '룰렛!', 30, 1, '!룰렛', 1000, 2,
                        ?, 'CONFIRMED')
                """, """
                [{"id":40,"label":"꽝","probabilityBasisPoints":5000,
                  "losingItem":true,"rewardType":"CUSTOM","conversionMode":"NONE",
                  "exchangeFavoriteValue":0,"active":true,"displayOrder":0},
                 {"id":41,"label":"당첨","probabilityBasisPoints":5000,
                  "losingItem":false,"rewardType":"FAVORITE","conversionMode":"AUTO",
                  "exchangeFavoriteValue":50,"active":true,"displayOrder":1}]
                """);
        jdbc.update("""
                INSERT INTO roulette_round_result
                    (id, create_date, modify_date, roulette_event_id, round_no,
                     item_label, probability_basis_points, losing_item, reward_type,
                     conversion_mode, exchange_favorite_value, status, ledger_id,
                     user_upbo_id, failure_reason, ticket)
                VALUES
                    (60, '2026-01-06 00:00:10.000000',
                     '2026-01-06 00:00:20.000000', 50, 1, '당첨', 5000,
                     FALSE, 'FAVORITE', 'AUTO', 50, 'APPLIED', 11, 70, NULL, 6000),
                    (61, '2026-01-06 00:00:11.000000',
                     '2026-01-06 00:00:21.000000', 50, 2, '꽝', 5000,
                     TRUE, 'CUSTOM', 'NONE', 0, 'FAILED', NULL, NULL, 'legacy failure', 1000)
                """);
        jdbc.update("""
                INSERT INTO user_upbo
                    (id, create_date, modify_date, user_id, upbo_template_id,
                     nick_name_snapshot, label, status, exchange_favorite_value,
                     reward_type, conversion_mode, source_type, ledger_id,
                     public_description, private_memo, actor_id)
                VALUES (70, '2026-01-06 00:00:20.000000',
                        '2026-01-06 00:00:20.000000', 'viewer', NULL,
                        '시청자', '당첨', 'CONVERTED', 50, 'FAVORITE', 'AUTO',
                        'UPBO_ROULETTE', 11, '룰렛 당첨', '자동 지급', 'SYSTEM')
                """);
        jdbc.update("""
                INSERT INTO overlay_token
                    (id, create_date, modify_date, token_hash, active, revoked_at, issued_by)
                VALUES (80, '2026-01-01 00:00:00.000000',
                        '2026-01-01 00:00:00.000000', ?, TRUE, NULL, 'streamer')
                """, "A".repeat(43));
        jdbc.update("""
                INSERT INTO overlay_display_event
                    (id, create_date, modify_date, roulette_event_id,
                     replay_of_display_event_id, status, expires_at, fetched_at, displayed_at)
                VALUES (90, '2026-01-06 00:00:30.000000',
                        '2026-01-06 00:00:40.000000', 50, NULL, 'DISPLAYED',
                        '2026-01-06 00:10:30.000000',
                        '2026-01-06 00:00:35.000000', '2026-01-06 00:00:40.000000')
                """);
    }

    private void assertFinalCanonicalSchema(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
                """, Integer.class)).isEqualTo(16);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND (table_name LIKE 'next!_%' ESCAPE '!'
                        OR table_name LIKE 'migration!_%' ESCAPE '!'
                        OR table_name IN ('authorization_account', 'favorite_account',
                           'favorite_history', 'roulette_event', 'user_upbo',
                           'overlay_display_event', 'subscription'))
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                 WHERE constraint_schema = DATABASE() AND constraint_type = 'FOREIGN KEY'
                """, Integer.class)).isEqualTo(25);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                 WHERE success = TRUE AND version IS NOT NULL
                """, Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name <> 'flyway_schema_history'
                   AND data_type = 'timestamp'
                   AND datetime_precision = 6
                """, Integer.class)).isEqualTo(35);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name <> 'flyway_schema_history'
                   AND data_type = 'datetime'
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name <> 'flyway_schema_history'
                   AND data_type = 'date'
                """, Integer.class)).isZero();
    }

    private void assertRepresentativeData(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject(
                "SELECT display_name FROM user_account WHERE user_id = 'viewer'",
                String.class
        )).isEqualTo("시청자");
        assertThat(jdbc.queryForObject(
                "SELECT access_token FROM oauth_credential WHERE user_id = 'streamer'",
                String.class
        )).isEqualTo("access-token");
        assertThat(jdbc.queryForObject(
                "SELECT message_template FROM command WHERE trigger_token = '!호감도'",
                String.class
        )).contains("{point.balance}").doesNotContain("{favorite.balance}");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM command_execution", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT UNIX_TIMESTAMP(week_started_at) FROM weekly_chat_count WHERE id = 110",
                Long.class
        )).isEqualTo(Instant.parse("2026-01-04T15:00:00Z").getEpochSecond());
        assertThat(jdbc.queryForObject(
                "SELECT source_type FROM point_ledger_entry WHERE id = 10",
                String.class
        )).isEqualTo("PRESENCE_REWARD");
        assertThat(jdbc.queryForObject(
                "SELECT source_type FROM point_ledger_entry WHERE id = 12",
                String.class
        )).isEqualTo("GOOGLE_SHEET_SYNC");
        assertThat(jdbc.queryForObject(
                "SELECT SUM(delta) FROM point_ledger_entry WHERE user_id = 'viewer'",
                Long.class
        )).isEqualTo(175L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roulette_config", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM roulette_config WHERE id = 30",
                String.class
        )).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                "SELECT roulette_run_id FROM roulette_round WHERE id = 60",
                Long.class
        )).isEqualTo(20L);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM roulette_round WHERE id = 60",
                String.class
        )).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject(
                "SELECT reward_type FROM reward_grant WHERE id = 70",
                String.class
        )).isEqualTo("POINT");
        assertThat(jdbc.queryForObject(
                "SELECT roulette_round_id FROM reward_grant WHERE id = 70",
                Long.class
        )).isEqualTo(60L);
        assertThat(jdbc.queryForObject(
                "SELECT roulette_run_id FROM overlay_display_job WHERE id = 90",
                Long.class
        )).isEqualTo(20L);
        assertThat(jdbc.queryForObject(
                "SELECT idempotency_key FROM overlay_display_job WHERE id = 90",
                String.class
        )).isEqualTo("legacy-overlay-display:90");
        assertWallClock(jdbc, "user_account", "user_id", "streamer", "last_login_at",
                "2026-01-02 00:30:00.123456");
        assertWallClock(jdbc, "oauth_credential", "user_id", "streamer", "access_token_expires_at",
                "2026-01-02 01:00:00.000000");
        assertWallClock(jdbc, "point_ledger_entry", "id", 10L, "created_at",
                "2026-01-03 00:00:00.000000");
        assertWallClock(jdbc, "donation", "id", 20L, "received_at",
                "2026-01-06 00:00:00.000000");
        assertWallClock(jdbc, "timer_message", "id", 120L, "created_at",
                "2026-01-01 00:00:00.000000");
        assertWallClock(jdbc, "roulette_config", "id", 30L, "created_at",
                "2026-01-01 00:00:00.000000");
        assertWallClock(jdbc, "roulette_run", "donation_id", 20L, "created_at",
                "2026-01-06 00:00:00.000000");
        assertWallClock(jdbc, "roulette_round", "id", 60L, "created_at",
                "2026-01-06 00:00:10.000000");
        assertWallClock(jdbc, "reward_grant", "id", 70L, "created_at",
                "2026-01-06 00:00:20.000000");
        assertWallClock(jdbc, "overlay_display_job", "id", 90L, "created_at",
                "2026-01-06 00:00:30.000000");
    }

    private void assertCanonicalTriggers(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.triggers
                 WHERE trigger_schema = DATABASE()
                   AND trigger_name LIKE 'trg!_%' ESCAPE '!'
                """, Integer.class)).isEqualTo(24);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE point_ledger_entry SET delta = 999 WHERE id = 10"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("insert-only");
        Long commandId = jdbc.queryForObject(
                "SELECT id FROM command WHERE trigger_token = '!호감도'", Long.class);
        jdbc.update("""
                INSERT INTO command_execution
                    (command_id, user_id, executed_at, execution_policy_snapshot,
                     cooldown_seconds_snapshot, calendar_day_started_at)
                VALUES (?, 'viewer', '2026-01-02 00:30:00.000000',
                        'USER_CALENDAR_DAY', NULL, '2026-01-02 00:00:00.000000')
                """, commandId);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM command_execution WHERE command_id = ? AND user_id = 'viewer'",
                Integer.class,
                commandId
        )).isOne();
        jdbc.update("DELETE FROM command_execution WHERE command_id = ? AND user_id = 'viewer'", commandId);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO command_execution
                    (command_id, user_id, executed_at, execution_policy_snapshot,
                     cooldown_seconds_snapshot, calendar_day_started_at)
                VALUES (?, 'viewer', '2026-01-02 00:30:00.000000',
                        'USER_CALENDAR_DAY', NULL, '2026-01-01 00:00:00.000000')
                """, commandId))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Asia/Seoul day-start instant");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name <> 'flyway_schema_history'
                   AND column_comment = ''
                """, Integer.class)).isZero();
    }

    private void assertTimestampSessionConversion(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
                statement.execute("SET time_zone = '+00:00'");
            }
            JdbcTemplate utc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            assertThat(utc.queryForObject("""
                    SELECT DATE_FORMAT(last_login_at, '%Y-%m-%d %H:%i:%s.%f')
                      FROM user_account
                     WHERE user_id = 'streamer'
                    """, String.class)).isEqualTo("2026-01-01 15:30:00.123456");

            Long commandId = utc.queryForObject(
                    "SELECT id FROM command WHERE trigger_token = '!호감도'", Long.class);
            utc.update("""
                    INSERT INTO command_execution
                        (command_id, user_id, executed_at, execution_policy_snapshot,
                         cooldown_seconds_snapshot, calendar_day_started_at)
                    VALUES (?, 'viewer', '2026-01-01 15:30:00.000000',
                            'USER_CALENDAR_DAY', NULL, '2026-01-01 15:00:00.000000')
                    """, commandId);
            assertThat(utc.queryForObject(
                    "SELECT COUNT(*) FROM command_execution WHERE command_id = ? AND user_id = 'viewer'",
                    Integer.class,
                    commandId
            )).isOne();
            utc.update("DELETE FROM command_execution WHERE command_id = ? AND user_id = 'viewer'", commandId);

            utc.update("""
                    INSERT INTO weekly_chat_count (week_started_at, user_id, chat_count)
                    VALUES ('2026-01-04 15:00:00.000000', 'streamer', 1)
                    """);
            assertThat(utc.queryForObject("""
                    SELECT COUNT(*) FROM weekly_chat_count
                     WHERE user_id = 'streamer'
                       AND week_started_at = '2026-01-04 15:00:00.000000'
                    """, Integer.class)).isOne();
            utc.update("DELETE FROM weekly_chat_count WHERE user_id = 'streamer'");
            assertThatThrownBy(() -> utc.update("""
                    INSERT INTO weekly_chat_count (week_started_at, user_id, chat_count)
                    VALUES ('2026-01-04 15:00:01.000000', 'streamer', 1)
                    """))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("ck_weekly_chat_count__week_start");
        }
    }

    private void assertWallClock(
            JdbcTemplate jdbc,
            String table,
            String idColumn,
            Object id,
            String dateTimeColumn,
            String expected
    ) {
        String actual = jdbc.queryForObject(
                "SELECT DATE_FORMAT(" + dateTimeColumn + ", '%Y-%m-%d %H:%i:%s.%f') FROM "
                        + table + " WHERE " + idColumn + " = ?",
                String.class,
                id
        );
        assertThat(actual).isEqualTo(expected);
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT COUNT(*) > 0 FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = ?
                """, Boolean.class, tableName));
    }

    private int cutoverFenceCount(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.triggers
                 WHERE trigger_schema = DATABASE()
                   AND trigger_name LIKE 'trg_cutover!_%' ESCAPE '!'
                """, Integer.class);
    }
}
