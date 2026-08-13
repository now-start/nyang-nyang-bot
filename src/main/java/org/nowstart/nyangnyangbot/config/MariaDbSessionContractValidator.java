package org.nowstart.nyangnyangbot.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

final class MariaDbSessionContractValidator {

    private static final int SEOUL_OFFSET_SECONDS = 9 * 60 * 60;
    private static final Pattern MAXDB_SQL_MODE =
            Pattern.compile("(?i)(?:^|,)\\s*MAXDB\\s*(?:,|$)");

    private MariaDbSessionContractValidator() {
    }

    static void requireCanonicalSession(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT @@session.time_zone,
                            TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                            @@session.explicit_defaults_for_timestamp,
                            @@session.sql_mode
                     """)) {
            if (!resultSet.next()) {
                throw new SQLException("Database did not return the session time zone");
            }
            String sessionTimeZone = resultSet.getString(1);
            int offsetSeconds = resultSet.getInt(2);
            if (offsetSeconds != SEOUL_OFFSET_SECONDS) {
                throw new SQLException("Application requires an Asia/Seoul (+09:00) DB session; found "
                        + sessionTimeZone + " with offset seconds " + offsetSeconds);
            }
            if (!resultSet.getBoolean(3)) {
                throw new SQLException("Application requires explicit_defaults_for_timestamp=ON");
            }
            String sqlMode = resultSet.getString(4);
            if (sqlMode != null && MAXDB_SQL_MODE.matcher(sqlMode).find()) {
                throw new SQLException("Application cannot run with SQL_MODE=MAXDB because MariaDB "
                        + "silently converts TIMESTAMP columns to DATETIME");
            }
        }
    }
}
