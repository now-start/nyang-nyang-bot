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
            ";MODE=MariaDB;TIME ZONE=Asia/Seoul;"
                    + "INIT=CREATE DOMAIN IF NOT EXISTS LONGTEXT AS LONGVARCHAR;DB_CLOSE_DELAY=-1";

    @Test
    @DisplayName("V1 canonical schema를 신규 DB에 적용할 수 있다")
    void flywayMigration_ShouldApplyCanonicalBaselineToEmptyDatabase() {
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

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'PUBLIC' "
                        + "and lower(table_name) <> 'flyway_schema_history'",
                Integer.class
        )).isEqualTo(16);
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.referential_constraints",
                Integer.class
        )).isEqualTo(25);
        assertThat(jdbc.queryForObject(
                "select count(*) from \"flyway_schema_history\" "
                        + "where \"success\" = true and \"version\" is not null",
                Integer.class
        )).isOne();
        assertThat(jdbc.queryForList(
                "select \"version\" from \"flyway_schema_history\" "
                        + "where \"success\" = true and \"version\" is not null "
                        + "order by \"installed_rank\"",
                String.class
        )).containsExactly("1");
        assertThat(jdbc.queryForObject(
                "select count(*) from command where trigger_token in ('!호감도', '!룰렛결과')",
                Integer.class
        )).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "select message_template from command where trigger_token = '!호감도'",
                String.class
        )).isEqualTo("{viewer.nickname}님의 호감도는 {point.balance} 입니다.💛");
    }

    @Test
    @DisplayName("baseline은 모든 JPA 엔티티 테이블을 생성한다")
    void flywayMigration_ShouldCreateTablesForJpaEntities() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-entity-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> missingTables = entityTableNames().stream()
                .filter(table -> jdbc.queryForObject(
                        "select count(*) from information_schema.tables "
                                + "where table_schema = 'PUBLIC' and lower(table_name) = ?",
                        Integer.class,
                        table
                ) == 0)
                .toList();

        assertThat(missingTables).isEmpty();
    }

    private List<String> entityTableNames() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        return scanner.findCandidateComponents("org.nowstart.nyangnyangbot.adapter.out.persistence")
                .stream()
                .map(definition -> definition.getBeanClassName())
                .map(this::simpleClassName)
                .map(this::snakeCase)
                .sorted()
                .toList();
    }

    private String simpleClassName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
