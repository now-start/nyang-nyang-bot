package db.migration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import org.junit.jupiter.api.Test;

class CanonicalMigrationSupportTest {

    @Test
    void stableSnapshotIsolation_ShouldAcceptRepeatableReadAndSerializable() throws Exception {
        Connection connection = mock(Connection.class);
        given(connection.getTransactionIsolation())
                .willReturn(Connection.TRANSACTION_REPEATABLE_READ)
                .willReturn(Connection.TRANSACTION_SERIALIZABLE);

        assertThatCode(() -> CanonicalMigrationSupport.requireStableSnapshotIsolation(connection))
                .doesNotThrowAnyException();
        assertThatCode(() -> CanonicalMigrationSupport.requireStableSnapshotIsolation(connection))
                .doesNotThrowAnyException();
    }

    @Test
    void stableSnapshotIsolation_ShouldRejectReadCommitted() throws Exception {
        Connection connection = mock(Connection.class);
        given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);

        assertThatThrownBy(() -> CanonicalMigrationSupport.requireStableSnapshotIsolation(connection))
                .hasMessageContaining("REPEATABLE READ or SERIALIZABLE");
    }
}
