package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TimerMessageFlywayMigrationTest {

    private static final String H2_MARIADB_OPTIONS =
            ";MODE=MariaDB;TIME ZONE=Asia/Seoul;"
                    + "INIT=CREATE DOMAIN IF NOT EXISTS LONGTEXT AS LONGVARCHAR;DB_CLOSE_DELAY=-1";

    @Test
    void flywayMigration_ShouldCreateDuplicateSafeTimerMessageSchema() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:timer-message-flyway-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" "
                        + "where \"success\" = true and \"version\" = '5'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'timer_message'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'timer_message' and lower(column_name) in "
                        + "('next_run_at', 'chat_count_since_last_send', 'claimed_chat_count', "
                        + "'claim_token', 'claim_expires_at', 'last_sent_at')",
                Integer.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.indexes "
                        + "where lower(table_name) = 'timer_message' "
                        + "and lower(index_name) = 'idx_timer_message__due'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'command' and lower(column_name) like 'timer_%'",
                Integer.class
        )).isZero();
    }
}
