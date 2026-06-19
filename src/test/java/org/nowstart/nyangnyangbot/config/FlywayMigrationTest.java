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
                "select count(*) from information_schema.tables where lower(table_name) = 'roulette_table_entity'",
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
    @DisplayName("Flyway SQL은 JPA 기본 물리 네이밍으로 매핑되는 모든 엔티티 테이블을 생성한다")
    void flywayMigration_ShouldCreateTablesForImplicitJpaEntityNames() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-entity-naming-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=MONTH;DB_CLOSE_DELAY=-1",
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
                "select count(*) from information_schema.tables where lower(table_name) = 'roulette_table_entity'",
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
}
