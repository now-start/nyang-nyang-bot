package org.nowstart.nyangnyangbot.config;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class PostgresqlTestDatabase implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18")
            .withDatabaseName("nyang_test").withUsername("nyang_test").withPassword("nyang_test");

    private static synchronized DriverManagerDataSource database() {
        String url = System.getenv("NYANG_TEST_DATABASE_URL");
        if (url == null) {
            if (!POSTGRES.isRunning()) POSTGRES.start();
            url = POSTGRES.getJdbcUrl();
        } else if (!url.startsWith("jdbc:postgresql://127.0.0.1:") || !url.contains("/nyang_test?")) {
            throw new IllegalArgumentException("Only local disposable nyang_test is allowed");
        }
        return new DriverManagerDataSource(url, "nyang_test", "nyang_test");
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        DriverManagerDataSource source = database();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testPostgresql", Map.of(
                "spring.datasource.url", source.getUrl(),
                "spring.datasource.username", "nyang_test",
                "spring.datasource.password", "nyang_test",
                "spring.datasource.hikari.connection-init-sql", "SET TIME ZONE 'Asia/Seoul'")));
    }

    static DriverManagerDataSource create(String schema) {
        DriverManagerDataSource root = database();
        JdbcTemplate jdbc = new JdbcTemplate(root);
        if (!schema.matches("[a-z_]+")) throw new IllegalArgumentException("Invalid test schema");
        jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        jdbc.execute("CREATE SCHEMA " + schema);
        String url = root.getUrl();
        return new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?")
                + "currentSchema=" + schema + "&options=-c%20TimeZone=Asia/Seoul", "nyang_test", "nyang_test");
    }
}
