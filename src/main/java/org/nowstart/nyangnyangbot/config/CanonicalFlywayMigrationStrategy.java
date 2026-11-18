package org.nowstart.nyangnyangbot.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

@Component
public class CanonicalFlywayMigrationStrategy implements FlywayMigrationStrategy {

    @Override
    public void migrate(Flyway flyway) {
        validateSession(flyway.getConfiguration().getDataSource());
        flyway.migrate();
    }

    private void validateSession(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            PostgresqlSessionContractValidator.requireCanonicalSession(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("PostgreSQL session contract validation failed", exception);
        }
    }
}
