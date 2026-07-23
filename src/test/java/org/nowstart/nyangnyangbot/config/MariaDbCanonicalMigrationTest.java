package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
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
    private static final String LEGACY_DATETIME_UTC_PROPERTY =
            "nyang.migration.legacy-datetime-utc-approved";

    @Test
    void v6RepresentativeFixture_ShouldCutOverWithoutLegacyDataLoss() throws Exception {
        requireDocker();

        MariaDBContainer container = new MariaDBContainer("mariadb:10.11")
                .withDatabaseName("nyang_migration")
                .withUsername("nyang")
                .withPassword("nyang");
        String originalApproval = System.getProperty(CUTOVER_PROPERTY);
        String originalLegacyDateTimeApproval = System.getProperty(LEGACY_DATETIME_UTC_PROPERTY);
        try {
            container.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    container.getJdbcUrl(),
                    container.getUsername(),
                    container.getPassword()
            );
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            migrate(dataSource, "6").migrate();
            insertRepresentativeV6Fixture(jdbc);

            migrate(dataSource, "7.1").migrate();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.triggers
                     WHERE trigger_schema = DATABASE()
                       AND trigger_name LIKE 'trg_next!_%' ESCAPE '!'
                    """, Integer.class)).isEqualTo(24);
            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO next_roulette_config
                        (title, trigger_token, price_per_round, high_round_threshold, status)
                    VALUES ('invalid', '!invalid', 1, 1, 'ACTIVE')
                    """))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("roulette_config must be inserted as DRAFT");

            Flyway throughV9 = migrate(dataSource, "9");
            System.clearProperty(CUTOVER_PROPERTY);
            System.clearProperty(LEGACY_DATETIME_UTC_PROPERTY);
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("requires explicit approval");
            throughV9.repair();
            System.setProperty(CUTOVER_PROPERTY, "true");
            assertThatThrownBy(throughV9::migrate)
                    .hasMessageContaining("all legacy DATETIME values are UTC");
            throughV9.repair();
            System.setProperty(LEGACY_DATETIME_UTC_PROPERTY, "true");
            assertStableSnapshotIsolationRequired(dataSource, "8");
            migrate(dataSource, "8.2").migrate();
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
            assertCanonicalTriggers(jdbc);
        } finally {
            if (originalApproval == null) {
                System.clearProperty(CUTOVER_PROPERTY);
            } else {
                System.setProperty(CUTOVER_PROPERTY, originalApproval);
            }
            if (originalLegacyDateTimeApproval == null) {
                System.clearProperty(LEGACY_DATETIME_UTC_PROPERTY);
            } else {
                System.setProperty(LEGACY_DATETIME_UTC_PROPERTY, originalLegacyDateTimeApproval);
            }
            container.stop();
        }
    }

    @Test
    void partialShadowBuildPhases_ShouldRecoverAfterRepairAndRerun() {
        requireDocker();

        MariaDBContainer container = new MariaDBContainer("mariadb:10.11")
                .withDatabaseName("nyang_build_retry")
                .withUsername("nyang")
                .withPassword("nyang");
        String originalApproval = System.getProperty(CUTOVER_PROPERTY);
        String originalLegacyDateTimeApproval = System.getProperty(LEGACY_DATETIME_UTC_PROPERTY);
        try {
            container.start();
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    container.getJdbcUrl(),
                    container.getUsername(),
                    container.getPassword()
            );
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

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

            System.setProperty(CUTOVER_PROPERTY, "true");
            System.setProperty(LEGACY_DATETIME_UTC_PROPERTY, "true");
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
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM migration_cutover_metadata
                     WHERE singleton_id = 1
                       AND source_checksum IS NOT NULL
                       AND target_checksum IS NOT NULL
                    """, Integer.class)).isOne();
        } finally {
            if (originalApproval == null) {
                System.clearProperty(CUTOVER_PROPERTY);
            } else {
                System.setProperty(CUTOVER_PROPERTY, originalApproval);
            }
            if (originalLegacyDateTimeApproval == null) {
                System.clearProperty(LEGACY_DATETIME_UTC_PROPERTY);
            } else {
                System.setProperty(LEGACY_DATETIME_UTC_PROPERTY, originalLegacyDateTimeApproval);
            }
            container.stop();
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
                     FALSE, '2026-01-02 00:00:00.000000'),
                    ('B', '2026-01-01 00:00:00.000000',
                     '2026-01-02 00:00:00.000000', 'upper-b',
                     'access-b', 'refresh-b', 'Bearer', 3600, 'chat',
                     FALSE, '2026-01-02 00:00:00.000000')
                """);
        jdbc.update("""
                INSERT INTO favorite_account
                    (user_id, create_date, modify_date, nick_name, favorite)
                VALUES ('viewer', '2026-01-01 01:00:00.000000',
                        '2026-01-02 01:00:00.000000', '시청자', 150)
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
                     'UPBO_ROULETTE', 'roulette-round:60')
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
                        'viewer', '시청자', 1000, '룰렛!', '["🐱"]', 'donation-20')
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
                        'viewer', '시청자', 1000, '룰렛!', 30, 1, '!룰렛', 1000, 1,
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
                VALUES (60, '2026-01-06 00:00:10.000000',
                        '2026-01-06 00:00:20.000000', 50, 1, '당첨', 5000,
                        FALSE, 'FAVORITE', 'AUTO', 50, 'APPLIED', 11, 70, NULL, 6000)
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
                """, Integer.class)).isEqualTo(13);
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
                "SELECT source_type FROM point_ledger_entry WHERE id = 10",
                String.class
        )).isEqualTo("PRESENCE_REWARD");
        assertThat(jdbc.queryForObject(
                "SELECT SUM(delta) FROM point_ledger_entry WHERE user_id = 'viewer'",
                Long.class
        )).isEqualTo(150L);
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
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND table_name <> 'flyway_schema_history'
                   AND column_comment = ''
                """, Integer.class)).isZero();
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
