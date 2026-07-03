package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FlywayMigrationTest {

    private static final String H2_MARIADB_OPTIONS =
            ";MODE=MariaDB;INIT=CREATE DOMAIN IF NOT EXISTS LONGTEXT AS CLOB;DB_CLOSE_DELAY=-1";

    @Test
    @DisplayName("Flyway SQL 마이그레이션을 신규 DB에 적용할 수 있다")
    void flywayMigration_ShouldApplyToEmptyDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-migration-test" + H2_MARIADB_OPTIONS,
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
                "select count(*) from \"flyway_schema_history\" "
                        + "where \"success\" = true and \"version\" is not null",
                Integer.class
        );
        Integer rouletteTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'roulette_table'",
                Integer.class
        );
        Integer ledgerColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history' "
                        + "and lower(column_name) = 'idempotency_key'",
                Integer.class
        );
        Integer sourceTypeColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history' "
                        + "and lower(column_name) = 'source_type'",
                Integer.class
        );
        Integer subscriptionMonthColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'subscription' "
                        + "and lower(column_name) = 'subscription_month'",
                Integer.class
        );
        Integer legacyMonthColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'subscription' "
                        + "and lower(column_name) = 'month'",
                Integer.class
        );
        Integer oldFavoriteTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'favorite_entity'",
                Integer.class
        );
        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.referential_constraints",
                Integer.class
        );
        Integer weeklyUniqueIndexCount = indexExists(jdbcTemplate, "weekly_chat_rank", "uk_weekly_chat_rank_week_user");
        Integer weeklyWeekIndexCount = indexExists(jdbcTemplate, "weekly_chat_rank", "idx_weekly_chat_rank_week");

        assertThat(migrationCount).isEqualTo(3);
        assertThat(rouletteTableCount).isEqualTo(1);
        assertThat(ledgerColumnCount).isEqualTo(1);
        assertThat(sourceTypeColumnCount).isEqualTo(1);
        assertThat(subscriptionMonthColumnCount).isEqualTo(1);
        assertThat(legacyMonthColumnCount).isZero();
        assertThat(oldFavoriteTableCount).isZero();
        assertThat(foreignKeyCount).isZero();
        assertThat(weeklyUniqueIndexCount).isEqualTo(1);
        assertThat(weeklyWeekIndexCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Flyway SQL은 JPA 기본 물리 네이밍으로 매핑되는 모든 엔티티 테이블을 생성한다")
    void flywayMigration_ShouldCreateTablesForImplicitJpaEntityNames() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-entity-naming-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> missingTables = entityTableNames().stream()
                .filter(tableName -> tableExists(jdbcTemplate, tableName) == 0)
                .toList();

        assertThat(missingTables).isEmpty();
    }

    @Test
    @DisplayName("이미 main 스키마가 있는 DB는 baseline 후 증분 마이그레이션만 적용할 수 있다")
    void flywayMigration_ShouldBaselineExistingMainSchemaAndApplyDeltaMigration() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-baseline-test" + H2_MARIADB_OPTIONS,
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
        jdbcTemplate.update("insert into donation_entity (donation_event_id) values (''), ('')");
        jdbcTemplate.update("insert into favorite_history_entity (history, favorite, idempotency_key) values "
                + "('legacy-1', 10, ''), ('legacy-2', 20, '')");
        jdbcTemplate.execute("drop table \"flyway_schema_history\"");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        Integer versionTwoCount = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" "
                        + "where \"success\" = true and \"version\" = '2'",
                Integer.class
        );
        Integer rouletteTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'roulette_table'",
                Integer.class
        );
        Integer ledgerColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history' "
                        + "and lower(column_name) = 'idempotency_key'",
                Integer.class
        );
        Integer sourceTypeColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history' "
                        + "and lower(column_name) = 'source_type'",
                Integer.class
        );
        Integer favoriteAccountColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'favorite_history' "
                        + "and lower(column_name) = 'favorite_account_user_id'",
                Integer.class
        );
        Integer subscriptionMonthColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'subscription' "
                        + "and lower(column_name) = 'subscription_month'",
                Integer.class
        );
        Integer legacyMonthColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'subscription' "
                        + "and lower(column_name) = 'month'",
                Integer.class
        );
        Integer oldFavoriteTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = 'favorite_entity'",
                Integer.class
        );
        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.referential_constraints",
                Integer.class
        );
        Integer weeklyUniqueIndexCount = indexExists(jdbcTemplate, "weekly_chat_rank", "uk_weekly_chat_rank_week_user");
        Integer weeklyWeekIndexCount = indexExists(jdbcTemplate, "weekly_chat_rank", "idx_weekly_chat_rank_week");
        Integer normalizedDonationEventIdCount = jdbcTemplate.queryForObject(
                "select count(*) from donation where donation_event_id is null",
                Integer.class
        );
        Integer blankFavoriteIdempotencyKeyCount = jdbcTemplate.queryForObject(
                "select count(*) from favorite_history where idempotency_key = ''",
                Integer.class
        );
        Integer migratedFavoriteIdempotencyKeyCount = jdbcTemplate.queryForObject(
                "select count(*) from favorite_history where idempotency_key like 'legacy-favorite-history:%'",
                Integer.class
        );

        assertThat(versionTwoCount).isEqualTo(1);
        assertThat(rouletteTableCount).isEqualTo(1);
        assertThat(ledgerColumnCount).isEqualTo(1);
        assertThat(sourceTypeColumnCount).isEqualTo(1);
        assertThat(favoriteAccountColumnCount).isEqualTo(1);
        assertThat(subscriptionMonthColumnCount).isEqualTo(1);
        assertThat(legacyMonthColumnCount).isZero();
        assertThat(oldFavoriteTableCount).isZero();
        assertThat(foreignKeyCount).isZero();
        assertThat(weeklyUniqueIndexCount).isEqualTo(1);
        assertThat(weeklyWeekIndexCount).isEqualTo(1);
        assertThat(normalizedDonationEventIdCount).isEqualTo(2);
        assertThat(blankFavoriteIdempotencyKeyCount).isZero();
        assertThat(migratedFavoriteIdempotencyKeyCount).isEqualTo(2);
    }

    private List<String> entityTableNames() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        return scanner.findCandidateComponents("org.nowstart.nyangnyangbot.adapter.out.persistence").stream()
                .map(beanDefinition -> beanDefinition.getBeanClassName())
                .map(className -> className.substring(className.lastIndexOf('.') + 1))
                .map(this::toSnakeCase)
                .sorted()
                .toList();
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private Integer tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ?",
                Integer.class,
                tableName
        );
    }

    private Integer indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        return jdbcTemplate.queryForObject(
                "select count(*) from information_schema.indexes "
                        + "where lower(table_name) = ? "
                        + "and (lower(index_name) = ? or lower(index_name) like ?)",
                Integer.class,
                tableName,
                indexName,
                indexName + "_%"
        );
    }
}
