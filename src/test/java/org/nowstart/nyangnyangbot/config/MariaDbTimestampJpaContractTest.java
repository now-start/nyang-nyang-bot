package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mariadb.MariaDBContainer;

@EnabledIfSystemProperty(named = "nyang.test.require-mariadb", matches = "true")
@SpringBootTest(
        classes = MariaDbTimestampJpaContractTest.JpaContractApplication.class,
        properties = {
                "spring.flyway.enabled=false",
                "eureka.client.enabled=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul",
                "spring.datasource.hikari.connection-init-sql="
                        + "SET time_zone = '+09:00', explicit_defaults_for_timestamp = ON"
        }
)
class MariaDbTimestampJpaContractTest {

    private static final String PERSISTENCE_BASE_PACKAGE =
            "org.nowstart.nyangnyangbot.adapter.out.persistence";
    private static final String CUTOVER_PROPERTY = "nyang.migration.canonical-cutover-approved";
    private static final String BACKFILL_PROPERTY = "nyang.migration.canonical-backfill-approved";
    private static final String LEGACY_DATETIME_ZONE_PROPERTY =
            "nyang.migration.legacy-datetime-zone";
    private static final Instant EXPECTED_INSTANT =
            Instant.parse("2026-01-01T15:30:00.123456Z");
    private static final LocalDateTime EXPECTED_SEOUL_TIME =
            LocalDateTime.parse("2026-01-02T00:30:00.123456");
    private static final LocalDateTime EXPECTED_UTC_TIME =
            LocalDateTime.parse("2026-01-01T15:30:00.123456");
    private static final MariaDBContainer MARIADB = new MariaDBContainer("mariadb:10.11")
            .withDatabaseName("nyang_timestamp_jpa")
            .withUsername("nyang")
            .withPassword("nyang")
            .withCommand("--default-time-zone=+09:00");

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        MARIADB.start();
        migrateCanonicalSchema();
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
    }

    private static void migrateCanonicalSchema() {
        String originalCutoverApproval = System.getProperty(CUTOVER_PROPERTY);
        String originalBackfillApproval = System.getProperty(BACKFILL_PROPERTY);
        String originalLegacyDateTimeZone = System.getProperty(LEGACY_DATETIME_ZONE_PROPERTY);
        try {
            System.setProperty(CUTOVER_PROPERTY, "true");
            System.setProperty(BACKFILL_PROPERTY, "true");
            System.setProperty(LEGACY_DATETIME_ZONE_PROPERTY, "Asia/Seoul");
            Flyway.configure()
                    .dataSource(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
        } finally {
            restoreProperty(CUTOVER_PROPERTY, originalCutoverApproval);
            restoreProperty(BACKFILL_PROPERTY, originalBackfillApproval);
            restoreProperty(LEGACY_DATETIME_ZONE_PROPERTY, originalLegacyDateTimeZone);
        }
    }

    private static void restoreProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    @AfterAll
    static void stopContainer() {
        MARIADB.stop();
    }

    @Test
    void instantRoundTripsWithoutNineHourShiftAcrossJpaAndJdbcSessions() throws SQLException {
        assertThat(entityManagerFactory.getProperties())
                .containsEntry("hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP");

        UserAccount account = UserAccount.builder()
                .userId("timestamp-contract-user")
                .displayName("timestamp-contract")
                .admin(false)
                .lastLoginAt(EXPECTED_INSTANT)
                .build();

        userAccountRepository.saveAndFlush(account);
        entityManager.clear();

        UserAccount restored = userAccountRepository.findById(account.getUserId()).orElseThrow();
        assertThat(restored.getLastLoginAt()).isEqualTo(EXPECTED_INSTANT);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(queryString(connection, "SELECT @@global.time_zone")).isEqualTo("+09:00");
            assertThat(queryString(connection, "SELECT @@session.time_zone")).isEqualTo("+09:00");
            assertThat(queryLastLoginAt(connection)).isEqualTo(EXPECTED_SEOUL_TIME);
        }

        try (Connection connection = DriverManager.getConnection(
                MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("SET time_zone = '+00:00'");
            assertThat(queryLastLoginAt(connection)).isEqualTo(EXPECTED_UTC_TIME);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private static LocalDateTime queryLastLoginAt(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_login_at FROM user_account WHERE user_id = ?")) {
            statement.setString(1, "timestamp-contract-user");
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getObject(1, LocalDateTime.class);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(PERSISTENCE_BASE_PACKAGE)
    @EnableJpaRepositories(PERSISTENCE_BASE_PACKAGE)
    static class JpaContractApplication {
    }
}
