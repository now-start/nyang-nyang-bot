package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PostgresqlContractTest {
    @Test
    void validatesPrimaryAndRejectsWrongSessionTimeZone() throws Exception {
        var source = PostgresqlTestDatabase.create("postgres_contract");
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            PostgresqlSessionContractValidator.requireCanonicalSession(connection);
            statement.execute("SET TIME ZONE 'UTC'");
            assertThatThrownBy(() -> PostgresqlSessionContractValidator.requireCanonicalSession(connection))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void preservesMicrosecondsAcrossTimeZonesAndEnforcesTextContracts() {
        var source = PostgresqlTestDatabase.create("postgres_time_contract");
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO user_account(user_id,display_name,last_login_at) VALUES (?,?,?::timestamptz)",
                "CaseUser", "Éclair", "2026-01-01T15:00:00.123456Z");
        assertThat(jdbc.queryForObject("SELECT last_login_at FROM user_account WHERE user_id='CaseUser'",
                OffsetDateTime.class).toInstant()).isEqualTo(java.time.Instant.parse("2026-01-01T15:00:00.123456Z"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_account WHERE user_id='caseuser'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_account WHERE display_name='eclair'", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_account WHERE display_name LIKE '%clair%'", Integer.class)).isOne();
    }
}
