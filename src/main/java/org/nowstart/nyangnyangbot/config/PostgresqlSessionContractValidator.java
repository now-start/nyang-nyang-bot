package org.nowstart.nyangnyangbot.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class PostgresqlSessionContractValidator {
    private PostgresqlSessionContractValidator() {}

    static void requireCanonicalSession(Connection connection) throws SQLException {
        if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
            throw new SQLException("PostgreSQL is required");
        }
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT current_setting('TimeZone'), pg_is_in_recovery(), current_setting('transaction_read_only')")) {
            if (!result.next() || !"Asia/Seoul".equals(result.getString(1))
                    || result.getBoolean(2) || !"off".equals(result.getString(3))) {
                throw new SQLException("Writable PostgreSQL primary with Asia/Seoul session is required");
            }
        }
    }
}
