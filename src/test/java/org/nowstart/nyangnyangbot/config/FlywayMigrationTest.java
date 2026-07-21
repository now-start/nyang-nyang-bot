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
        Integer lastLoginColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'authorization_account' "
                        + "and lower(column_name) = 'last_login_at'",
                Integer.class
        );
        Integer favoriteHistoryLastSeenColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'authorization_account' "
                        + "and lower(column_name) = 'favorite_history_last_seen_at'",
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
        Integer legacyCommandColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'command' "
                        + "and lower(column_name) in "
                        + "('type', 'action_key', 'timer_interval_minutes', 'timer_min_chat_count', 'required_role')",
                Integer.class
        );
        Integer legacyCommandActionCount = jdbcTemplate.queryForObject(
                "select count(*) from command where trigger_token = '!룰렛'",
                Integer.class
        );
        String favoriteTemplate = jdbcTemplate.queryForObject(
                "select message_template from command where trigger_token = '!호감도'",
                String.class
        );
        String rouletteResultTemplate = jdbcTemplate.queryForObject(
                "select message_template from command where trigger_token = '!룰렛결과'",
                String.class
        );

        assertThat(migrationCount).isEqualTo(6);
        assertThat(rouletteTableCount).isEqualTo(1);
        assertThat(ledgerColumnCount).isEqualTo(1);
        assertThat(sourceTypeColumnCount).isEqualTo(1);
        assertThat(subscriptionMonthColumnCount).isEqualTo(1);
        assertThat(legacyMonthColumnCount).isZero();
        assertThat(lastLoginColumnCount).isEqualTo(1);
        assertThat(favoriteHistoryLastSeenColumnCount).isZero();
        assertThat(oldFavoriteTableCount).isZero();
        assertThat(foreignKeyCount).isZero();
        assertThat(weeklyUniqueIndexCount).isEqualTo(1);
        assertThat(weeklyWeekIndexCount).isEqualTo(1);
        assertThat(legacyCommandColumnCount).isZero();
        assertThat(legacyCommandActionCount).isZero();
        assertThat(favoriteTemplate).isEqualTo("{viewer.nickname}님의 호감도는 {favorite.balance} 입니다.💛");
        assertThat(rouletteResultTemplate).isEqualTo("{viewer.nickname}님의 {roulette.recentSummary}");
    }

    @Test
    @DisplayName("V6는 이전 컬럼 삭제 후 중단된 DB에도 다시 적용할 수 있다")
    void flywayMigration_ShouldRecoverFromPartiallyAppliedV6() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-v6-recovery-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("5")
                .load();
        flyway.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("alter table authorization_account drop column favorite_history_last_seen_at");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Integer lastLoginColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where lower(table_name) = 'authorization_account' "
                        + "and lower(column_name) = 'last_login_at'",
                Integer.class
        );
        assertThat(lastLoginColumnCount).isEqualTo(1);
    }

    @Test
    @DisplayName("V4는 실제 사용 중인 룰렛 후원 키워드를 룰렛 설정으로 이전한다")
    void flywayMigration_ShouldTransferLegacyRouletteDonationCommand() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-command-transfer-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("3")
                .load();
        flyway.migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "insert into roulette_table "
                        + "(title, command, price_per_round, active, version, high_round_threshold) "
                        + "values ('기본 룰렛', '!원본', 1000, true, 0, 100)"
        );
        jdbcTemplate.update(
                "update command set trigger_token = '!후원룰렛', active = true "
                        + "where action_key = 'ROULETTE_DONATION'"
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForObject(
                "select command from roulette_table where active = true",
                String.class
        )).isEqualTo("!후원룰렛");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from command where trigger_token = '!후원룰렛'",
                Integer.class
        )).isZero();
    }

    @Test
    @DisplayName("V4는 비활성 레거시 명령에 의존하던 룰렛을 활성 상태로 남기지 않는다")
    void flywayMigration_ShouldDeactivateRouletteWhenLegacyCommandIsInactive() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-command-inactive-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("3")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update(
                "insert into roulette_table "
                        + "(title, command, price_per_round, active, version, high_round_threshold) "
                        + "values ('기본 룰렛', '!원본', 1000, true, 0, 100)"
        );
        jdbcTemplate.update(
                "update command set active = false where action_key = 'ROULETTE_DONATION'"
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForObject(
                "select active from roulette_table where title = '기본 룰렛'",
                Boolean.class
        )).isFalse();
    }

    @Test
    @DisplayName("V4는 저장된 이전 템플릿 변수를 정식 변수 키로 이전한다")
    void flywayMigration_ShouldMigrateLegacyTemplateVariables() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:flyway-command-variable-migration-test" + H2_MARIADB_OPTIONS,
                "sa",
                ""
        );
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("3")
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String boundaryTemplate = "{arg1}".repeat(50);
        jdbcTemplate.update(
                "insert into command "
                        + "(type, trigger_token, message_template, active, required_role, "
                        + "user_cooldown_seconds, created_by, updated_by) "
                        + "values ('TEXT', '!이전변수', "
                        + "'{nickname}|{command}|{args}|{arg1}|{arg2}|{favorite}|{date}|{time}|{datetime}', "
                        + "true, 'USER', 30, 'system', 'system')"
        );
        jdbcTemplate.update(
                "insert into command "
                        + "(type, trigger_token, message_template, active, required_role, "
                        + "user_cooldown_seconds, created_by, updated_by) "
                        + "values ('TEXT', '!경계변수', ?, true, 'USER', 30, 'system', 'system')",
                boundaryTemplate
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(jdbcTemplate.queryForObject(
                "select message_template from command where trigger_token = '!이전변수'",
                String.class
        )).isEqualTo(
                "{viewer.nickname}|{invocation.command}|{invocation.args}|{invocation.arg1}|"
                        + "{invocation.arg2}|{favorite.balance}|{time.date}|{time.time}|{time.datetime}"
        );
        assertThat(boundaryTemplate).hasSize(300);
        assertThat(jdbcTemplate.queryForObject(
                "select message_template from command where trigger_token = '!경계변수'",
                String.class
        )).isEqualTo("{invocation.arg1}".repeat(50)).hasSize(850);
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
