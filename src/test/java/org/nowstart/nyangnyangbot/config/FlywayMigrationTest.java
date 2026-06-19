package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FlywayMigrationTest {

    @Test
    @DisplayName("Flyway SQL 마이그레이션을 신규 DB에 적용할 수 있다")
    void flywayMigration_ShouldApplyToEmptyDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-migration-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MONTH;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true and version is not null",
                Integer.class
        );
        Integer rouletteTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'roulette_table'",
                Integer.class
        );
        Integer ledgerColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history_entity' "
                        + "and lower(column_name) = 'idempotency_key'",
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(2);
        assertThat(rouletteTableCount).isEqualTo(1);
        assertThat(ledgerColumnCount).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 main 스키마가 있는 DB는 baseline 후 증분 마이그레이션만 적용할 수 있다")
    void flywayMigration_ShouldBaselineExistingMainSchemaAndApplyDeltaMigration() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-baseline-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MONTH;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("drop table flyway_schema_history");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        Integer versionTwoCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true and version = '2'",
                Integer.class
        );
        Integer rouletteTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'roulette_table'",
                Integer.class
        );
        Integer ledgerColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history_entity' "
                        + "and lower(column_name) = 'idempotency_key'",
                Integer.class
        );

        assertThat(versionTwoCount).isEqualTo(1);
        assertThat(rouletteTableCount).isEqualTo(1);
        assertThat(ledgerColumnCount).isEqualTo(1);
    }
}
