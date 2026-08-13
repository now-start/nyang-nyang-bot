package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class MariaDbSessionContractValidatorTest {

    @Test
    void acceptsCanonicalMariaDbSession() throws SQLException {
        Connection connection = session("+09:00", 32_400, true, "STRICT_TRANS_TABLES");

        assertThatCode(() -> MariaDbSessionContractValidator.requireCanonicalSession(connection))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonSeoulSession() throws SQLException {
        Connection connection = session("+00:00", 0, true, "STRICT_TRANS_TABLES");

        assertThatThrownBy(() -> MariaDbSessionContractValidator.requireCanonicalSession(connection))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("Asia/Seoul (+09:00)");
    }

    @Test
    void rejectsMaxDbMode() throws SQLException {
        Connection connection = session("+09:00", 32_400, true, "STRICT_TRANS_TABLES,MAXDB");

        assertThatThrownBy(() -> MariaDbSessionContractValidator.requireCanonicalSession(connection))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("SQL_MODE=MAXDB");
    }

    private Connection session(String timeZone, int offsetSeconds,
                               boolean explicitTimestampDefaults, String sqlMode)
            throws SQLException {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn(timeZone);
        when(resultSet.getInt(2)).thenReturn(offsetSeconds);
        when(resultSet.getBoolean(3)).thenReturn(explicitTimestampDefaults);
        when(resultSet.getString(4)).thenReturn(sqlMode);
        return connection;
    }
}
